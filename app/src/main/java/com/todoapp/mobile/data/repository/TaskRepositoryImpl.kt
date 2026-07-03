// Detekt's IgnoredReturnValue rule mis-flags `Flow<T> = source.map { it.toDomain() }` patterns
// as ignored when in fact the result is the function's return value. Suppress at the file level
// rather than per-call-site since the same pattern recurs throughout this repository.
// Detekt without type resolution also mis-flags trailing code after `?: return@mapNotNull null`
// guards (e.g. inside syncDailyCompletions) as unreachable; suppress the rule file-wide.
@file:Suppress("IgnoredReturnValue", "UnreachableCode")

package com.todoapp.mobile.data.repository

import android.util.Log
import com.todoapp.mobile.common.DomainException
import com.todoapp.mobile.data.mapper.toDomain
import com.todoapp.mobile.data.mapper.toEntity
import com.todoapp.mobile.data.model.entity.SubtaskEntity
import com.todoapp.mobile.data.model.entity.SyncStatus
import com.todoapp.mobile.data.model.entity.TaskDailyCompletionEntity
import com.todoapp.mobile.data.model.entity.TaskEntity
import com.todoapp.mobile.data.model.network.data.SubtaskData
import com.todoapp.mobile.data.source.local.SubtaskCount
import com.todoapp.mobile.data.source.local.TaskDailyCompletionDao
import com.todoapp.mobile.data.source.local.datasource.GroupTaskLocalDataSource
import com.todoapp.mobile.data.source.local.datasource.TaskLocalDataSource
import com.todoapp.mobile.data.source.remote.datasource.TaskRemoteDataSource
import com.todoapp.mobile.di.IoDispatcher
import com.todoapp.mobile.domain.alarm.AlarmScheduler
import com.todoapp.mobile.domain.alarm.AlarmType
import com.todoapp.mobile.domain.constants.DailyPlanDefaults
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.Subtask
import com.todoapp.mobile.domain.model.Task
import com.todoapp.mobile.domain.model.firesOn
import com.todoapp.mobile.domain.model.toAlarmItem
import com.todoapp.mobile.domain.model.toDomain
import com.todoapp.mobile.domain.repository.CompletedCountByDay
import com.todoapp.mobile.domain.repository.DailyBucket
import com.todoapp.mobile.domain.repository.DailyPlanPreferences
import com.todoapp.mobile.domain.repository.MonthlyWeekBucket
import com.todoapp.mobile.domain.repository.TaskRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

// Aggregate-root repository (tasks + recurrence + per-day completion + sync + staged subtasks).
// The function count is inherent to the aggregate, not accidental complexity — suppress narrowly.
@Suppress("LargeClass", "TooManyFunctions")
class TaskRepositoryImpl
@Inject
constructor(
    private val remoteDataSource: TaskRemoteDataSource,
    private val localDataSource: TaskLocalDataSource,
    private val groupTaskLocalDataSource: GroupTaskLocalDataSource,
    private val todoApi: com.todoapp.mobile.data.source.remote.api.ToDoApi,
    private val pendingPhotoRepository: com.todoapp.mobile.domain.repository.PendingPhotoRepository,
    private val dailyCompletionDao: TaskDailyCompletionDao,
    private val alarmScheduler: AlarmScheduler,
    private val dailyPlanPreferences: DailyPlanPreferences,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TaskRepository {
    private val taskPhotoUrls = kotlinx.coroutines.flow.MutableStateFlow<Map<Long, List<String>>>(emptyMap())

    // Staged-task progress preservation: when a parent is completed via the cascade shortcut we snapshot
    // which steps were already done, so un-completing the parent restores that progress instead of zeroing
    // it. Session-memory only.
    private val stagedSnapshots = mutableMapOf<Long, Set<Long>>()

    // Serializes the two sync bodies (push + pull) across the whole app. This repository is a @Singleton,
    // so every WorkManager worker and ViewModel shares this one Mutex. SYNC_WORK and FETCH_WORK are distinct
    // WorkManager unique chains that both run SyncWorker, so without this two pushes — or a push and the
    // pull's non-atomic reconcile — could run concurrently and double-write / corrupt local state.
    private val syncMutex = Mutex()

    override fun observeTaskPhotoUrls(): Flow<Map<Long, List<String>>> = taskPhotoUrls

    override fun observeAllTaskEntities(): Flow<List<TaskEntity>> = localDataSource.observeAll()

    override fun observeAllTasks(): Flow<List<Task>> = observeAllTaskEntities().map { list ->
        list.map { it.toDomain() }
    }

    override fun observeRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<Task>> = localDataSource
        .observeRange(
            startDate = startDate.toEpochDay(),
            endDate = endDate.toEpochDay(),
        ).map { list ->
            list.map { it.toDomain() }
        }.withSubtaskCounts()

    override fun observeTasksByDate(date: LocalDate, includeRecurringInstances: Boolean): Flow<List<Task>> {
        val epochDay = date.toEpochDay()
        return combine(
            localDataSource.observeByDate(date = epochDay),
            localDataSource.observeAllRecurringTasks(),
            dailyCompletionDao.observeForDate(epochDay),
        ) { dateAnchored, recurring, completions ->
            val completedTaskIds = completions.map { it.taskId }.toSet()
            val nonRecurring = dateAnchored
                .filter { it.recurrence == Recurrence.NONE.name }
                .map { it.toDomain() }
            if (!includeRecurringInstances) return@combine nonRecurring
            // Recurring rows are intentionally excluded from the anchor-day list above so this
            // firesOn() expansion is the single source for recurring instances on the day.
            val recurringInstances = recurring.mapNotNull { entity ->
                val rule = Recurrence.fromStorage(entity.recurrence)
                val anchor = LocalDate.ofEpochDay(entity.date)
                val finishedOn = entity.finishedOn?.let { LocalDate.ofEpochDay(it) }
                if (!rule.firesOn(anchor, date, finishedOn)) return@mapNotNull null
                entity.toDomain().copy(
                    date = date,
                    // The finish day itself renders completed (derived from finishedOn, which syncs);
                    // earlier days keep their own per-day completion.
                    isCompleted = entity.id in completedTaskIds || entity.finishedOn == epochDay,
                )
            }
            nonRecurring + recurringInstances
        }.withSubtaskCounts()
    }

    override fun observeRecurringByType(recurrence: Recurrence): Flow<List<Task>> {
        if (recurrence == Recurrence.NONE) return kotlinx.coroutines.flow.flowOf(emptyList())
        // The recurrence-filter tabs are a "manage the routine" view: the checkbox reflects whether the
        // WHOLE routine is finished (finishedOn != null) — set via setRoutineFinished — not today's
        // per-day completion (that lives on the Today tab). Stamp date=today so the row still renders
        // against the current day.
        val today = LocalDate.now()
        return localDataSource.observeByRecurrence(recurrence.name).map { list ->
            list.map { entity ->
                entity.toDomain().copy(
                    date = today,
                    isCompleted = entity.finishedOn != null,
                )
            }
        }
    }

    override fun observeOverdueTasks(today: LocalDate): Flow<List<Task>> = localDataSource.observeOverdueTasks(today.toEpochDay()).map { list ->
        list.map { it.toDomain() }
    }

    override suspend fun deferTasksToTomorrow(taskIds: List<Long>) = withContext(ioDispatcher) {
        if (taskIds.isNotEmpty()) {
            localDataSource.shiftDatesByOneDay(taskIds)
        }
    }

    override suspend fun setInstanceCompletion(
        taskId: Long,
        date: LocalDate,
        completed: Boolean,
    ) = withContext(ioDispatcher) {
        val epochDay = date.toEpochDay()
        if (completed) {
            // Mark completed and queue a push. Re-marks a not-yet-pushed uncomplete (PENDING_DELETE) as a
            // live completion again.
            dailyCompletionDao.upsert(
                TaskDailyCompletionEntity(
                    taskId = taskId,
                    date = epochDay,
                    completedAt = System.currentTimeMillis(),
                    syncStatus = SyncStatus.PENDING_CREATE,
                ),
            )
        } else {
            when (dailyCompletionDao.get(taskId, epochDay)?.syncStatus) {
                // Never reached the server (or no row): drop it locally, nothing to tell the server.
                null, SyncStatus.PENDING_CREATE -> dailyCompletionDao.delete(taskId, epochDay)
                // Was synced: soft-delete so the uncomplete (completed=false) still reaches the server.
                // Hidden from the observe queries so the UI shows it unchecked immediately.
                else -> dailyCompletionDao.upsert(
                    TaskDailyCompletionEntity(
                        taskId = taskId,
                        date = epochDay,
                        completedAt = System.currentTimeMillis(),
                        syncStatus = SyncStatus.PENDING_DELETE,
                    ),
                )
            }
        }
        // Best-effort immediate push; pushPendingDailyCompletions() replays any failure durably.
        runCatching { pushDailyCompletion(taskId, epochDay, completed) }
        Unit
    }

    // Pushes a single day's completion state and, on success, settles the local row (SYNCED for a
    // completion, hard-delete for an uncomplete). A 404 (task gone) leaves the row for FK-cascade cleanup
    // when the parent task is tombstoned. Idempotent, so the immediate call and the replay can't conflict.
    private suspend fun pushDailyCompletion(taskId: Long, epochDay: Long, completed: Boolean) {
        val remoteId = localDataSource.getTaskById(taskId)?.remoteId ?: return
        com.todoapp.mobile.common
            .handleEmptyRequest {
                todoApi.setTaskDailyCompletion(
                    remoteId,
                    com.todoapp.mobile.data.model.network.request.TaskDailyCompletionRequest(
                        date = epochDay,
                        completed = completed,
                    ),
                )
            }.onSuccess {
                if (completed) {
                    dailyCompletionDao.markSynced(taskId, epochDay)
                } else {
                    dailyCompletionDao.delete(taskId, epochDay)
                }
            }
    }

    // Replays every pending completion/uncompletion whose immediate push failed. Runs inside the sync mutex
    // (called from pushPendingTasks), after task pushes so any FK-cascaded rows are already gone.
    private suspend fun pushPendingDailyCompletions() {
        dailyCompletionDao.getPending().forEach { row ->
            val completed = row.syncStatus != SyncStatus.PENDING_DELETE
            runCatching { pushDailyCompletion(row.taskId, row.date, completed) }
        }
    }

    override suspend fun setRoutineFinished(
        taskId: Long,
        finishedOn: LocalDate?,
    ) = withContext(ioDispatcher) {
        val current = localDataSource.getTaskById(taskId) ?: return@withContext
        // Only recurring tasks can be "finished"; plain tasks use the base isCompleted path.
        if (current.recurrence == Recurrence.NONE.name) return@withContext
        val epochDay = finishedOn?.toEpochDay()
        if (current.finishedOn == epochDay) return@withContext
        // Persist + flip SYNCED→PENDING_UPDATE so SyncWorker pushes finishedOn to the backend.
        // syncUpdatedTask preserves the local row, so finishedOn survives the SYNCED transition.
        val updated = current.copy(finishedOn = epochDay, syncStatus = current.syncStatus.afterEdit())
        localDataSource.update(updated)
        if (finishedOn != null) {
            // Finishing: the routine arms no more alarms. The finish-day occurrence renders completed in
            // the Today tab + week/month stats by deriving from finishedOn (which syncs), so no per-day
            // write is needed — keeping it consistent cross-device and off the daily-only completion endpoint.
            runCatching { alarmScheduler.cancelRecurring(taskId) }
        } else {
            // Un-finishing: the routine resumes, so re-arm its recurring alarm.
            scheduleRecurringAlarmIfNeeded(updated.id, updated.toDomain())
        }
        Unit
    }

    override fun countCompletedTasksInAWeek(date: LocalDate, includeRecurring: Boolean): Flow<Int> = observeWeeklyCounts(date, includeRecurring).map { (completed, _) -> completed.values.sum() }

    override fun countCompletedCountsByDayInAWeek(date: LocalDate, includeRecurring: Boolean): Flow<List<CompletedCountByDay>> = observeWeeklyCounts(date, includeRecurring).map { (completed, _) ->
        completed.toSortedMap().map { (day, count) -> CompletedCountByDay(day, count) }
    }

    override fun countCompletedTasksYearToDate(date: LocalDate): Flow<Int> {
        val yearStart = date.withDayOfYear(1)
        return localDataSource.countInRange(
            startDate = yearStart.toEpochDay(),
            endDate = date.toEpochDay(),
            isCompleted = true,
        )
    }

    override fun observePendingTasksYearToDate(date: LocalDate): Flow<Int> {
        val yearStart = date.withDayOfYear(1)
        return localDataSource.countInRange(
            startDate = yearStart.toEpochDay(),
            endDate = date.toEpochDay(),
            isCompleted = false,
        )
    }

    override fun observePendingTasksInAWeek(date: LocalDate, includeRecurring: Boolean): Flow<Int> = observeWeeklyCounts(date, includeRecurring).map { (_, pending) -> pending.values.sum() }

    override fun observeCompletedCountsByDayInAWeek(date: LocalDate, includeRecurring: Boolean): Flow<List<Int>> {
        val weekStart = date.with(DayOfWeek.MONDAY)
        return countCompletedCountsByDayInAWeek(date, includeRecurring).map { dayCounts ->
            val map = dayCounts.associate { it.date to it.count }
            (0 until DAYS_IN_WEEK).map { dayOffset ->
                map[weekStart.plusDays(dayOffset.toLong())] ?: 0
            }
        }
    }

    override fun observePendingCountsByDayInAWeek(date: LocalDate, includeRecurring: Boolean): Flow<List<Int>> {
        val weekStart = date.with(DayOfWeek.MONDAY)
        return observeWeeklyCounts(date, includeRecurring).map { (_, pending) ->
            (0 until DAYS_IN_WEEK).map { offset ->
                pending[weekStart.plusDays(offset.toLong())] ?: 0
            }
        }
    }

    /**
     * Single source of truth for weekly count aggregations: returns (completedByDay, pendingByDay)
     * with recurring tasks expanded to per-day instances. Non-recurring tasks contribute on their
     * own date based on `is_completed`. Recurring tasks contribute on every day they fire per
     * `Recurrence.firesOn`, with completion looked up in `task_daily_completions`.
     */
    private fun observeWeeklyCounts(
        date: LocalDate,
        includeRecurring: Boolean = true,
    ): Flow<Pair<Map<LocalDate, Int>, Map<LocalDate, Int>>> {
        val weekStart = date.with(DayOfWeek.MONDAY)
        val weekEnd = weekStart.plusDays(DAYS_TO_ADD.toLong())
        return observeRangeCounts(weekStart, weekEnd, includeRecurring)
    }

    override fun observeCompletedCountsByDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
        includeRecurring: Boolean,
    ): Flow<Map<LocalDate, Int>> = observeRangeCounts(startDate, endDate, includeRecurring).map { (completed, _) -> completed }

    override fun observeMonthlyWeekBuckets(
        monthStart: LocalDate,
        includeRecurring: Boolean,
    ): Flow<List<MonthlyWeekBucket>> {
        val monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth())
        return observeRangeCounts(monthStart, monthEnd, includeRecurring).map { (completed, pending) ->
            val totalDays = monthStart.lengthOfMonth()
            val bucketCount = (totalDays + DAYS_IN_WEEK - 1) / DAYS_IN_WEEK
            (0 until bucketCount).map { index ->
                val rangeStart = monthStart.plusDays((index * DAYS_IN_WEEK).toLong())
                val rangeEndDayOfMonth = ((index + 1) * DAYS_IN_WEEK).coerceAtMost(totalDays)
                val rangeEnd = monthStart.withDayOfMonth(rangeEndDayOfMonth)
                var completedSum = 0
                var pendingSum = 0
                var cursor = rangeStart
                while (!cursor.isAfter(rangeEnd)) {
                    completedSum += completed[cursor] ?: 0
                    pendingSum += pending[cursor] ?: 0
                    cursor = cursor.plusDays(1)
                }
                MonthlyWeekBucket(
                    weekIndex = index + 1,
                    rangeStart = rangeStart,
                    rangeEnd = rangeEnd,
                    completed = completedSum,
                    pending = pendingSum,
                )
            }
        }
    }

    override fun countCompletedTasksInAMonth(
        monthStart: LocalDate,
        includeRecurring: Boolean,
    ): Flow<Int> {
        val monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth())
        return observeRangeCounts(monthStart, monthEnd, includeRecurring).map { (completed, _) -> completed.values.sum() }
    }

    override fun observeDailyBucketsByDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
        includeRecurring: Boolean,
    ): Flow<List<DailyBucket>> = observeRangeCounts(startDate, endDate, includeRecurring).map { (completed, pending) ->
        val totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
        (0 until totalDays).map { offset ->
            val date = startDate.plusDays(offset.toLong())
            DailyBucket(
                date = date,
                completed = completed[date] ?: 0,
                pending = pending[date] ?: 0,
            )
        }
    }

    private fun observeRangeCounts(
        startDate: LocalDate,
        endDate: LocalDate,
        includeRecurring: Boolean,
    ): Flow<Pair<Map<LocalDate, Int>, Map<LocalDate, Int>>> = kotlinx.coroutines.flow.combine(
        localDataSource.observeRange(startDate.toEpochDay(), endDate.toEpochDay()),
        localDataSource.observeAllRecurringTasks(),
        dailyCompletionDao.observeRange(startDate.toEpochDay(), endDate.toEpochDay()),
    ) { dateBased, recurring, completions ->
        val completed = mutableMapOf<LocalDate, Int>()
        val pending = mutableMapOf<LocalDate, Int>()
        dateBased.filter { it.recurrence == Recurrence.NONE.name }.forEach { entity ->
            val day = LocalDate.ofEpochDay(entity.date)
            if (entity.isCompleted) completed.merge(day, 1, Int::plus)
            else pending.merge(day, 1, Int::plus)
        }
        if (includeRecurring) {
            val completionKeys = completions.map { it.taskId to it.date }.toSet()
            val totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
            recurring.forEach { entity ->
                val recurrence = Recurrence.fromStorage(entity.recurrence)
                val anchor = LocalDate.ofEpochDay(entity.date)
                val finishedOn = entity.finishedOn?.let { LocalDate.ofEpochDay(it) }
                for (offset in 0 until totalDays) {
                    val day = startDate.plusDays(offset.toLong())
                    if (recurrence.firesOn(anchor, day, finishedOn)) {
                        // Finish day counts as completed via finishedOn so stats match the Today tab.
                        val done = entity.id to day.toEpochDay() in completionKeys ||
                            entity.finishedOn == day.toEpochDay()
                        if (done) completed.merge(day, 1, Int::plus) else pending.merge(day, 1, Int::plus)
                    }
                }
            }
        }
        completed to pending
    }

    override suspend fun insert(task: Task) {
        // Local-first to close the race with concurrent syncRemoteTasksWithLocal: if we awaited
        // addTask before inserting locally, a sync running between the two could see the
        // server-side row with no local match and insert a duplicate, which then collides with
        // the local insert that runs after addTask returns.
        // §4.12: mint a stable idempotency key once so the first POST and any SyncWorker retry carry the
        // SAME clientTaskId → the backend dedups a lost-response retry instead of inserting a duplicate.
        val taskWithKey = task.copy(clientTaskId = task.clientTaskId ?: UUID.randomUUID().toString())
        val localEntity = taskWithKey.toEntity(SyncStatus.PENDING_CREATE).copy(id = 0L)
        val localId = localDataSource.insert(withInitializedOrder(localEntity))
        if (task.subtasks.isNotEmpty()) {
            localDataSource.insertSubtasks(
                task.subtasks.mapIndexed { index, subtask ->
                    subtask.toEntity(parentTaskId = localId, orderIndex = index)
                },
            )
        }
        scheduleRecurringAlarmIfNeeded(localId, task)

        remoteDataSource
            .addTask(taskWithKey)
            .onSuccess { remoteTask ->
                val current = localDataSource.getTaskById(localId) ?: return@onSuccess
                localDataSource.update(
                    current.copy(
                        remoteId = remoteTask.id,
                        syncStatus = SyncStatus.SYNCED,
                        photoUrls = remoteTask.photoUrls.joinToString(","),
                    ),
                )
                writeBackSubtasks(localId, remoteTask.subtasks)
            }
        // onFailure: row already exists as PENDING_CREATE, SyncWorker will push it later
        // (syncCreatedTask re-sends the local steps).
    }

    override suspend fun insertWithPhotos(
        task: Task,
        photos: List<Pair<ByteArray, String>>,
    ): Result<Unit> = runCatching {
        // Same local-first pattern as insert(); see comment above for the race rationale.
        // §4.12: same stable-key minting as insert() so a retried create dedups server-side.
        val taskWithKey = task.copy(clientTaskId = task.clientTaskId ?: UUID.randomUUID().toString())
        val localEntity = taskWithKey.toEntity(SyncStatus.PENDING_CREATE).copy(id = 0L)
        val localId = localDataSource.insert(withInitializedOrder(localEntity))
        if (task.subtasks.isNotEmpty()) {
            localDataSource.insertSubtasks(
                task.subtasks.mapIndexed { index, subtask ->
                    subtask.toEntity(parentTaskId = localId, orderIndex = index)
                },
            )
        }

        remoteDataSource
            .addTask(taskWithKey)
            .onSuccess { remoteTask ->
                val current = localDataSource.getTaskById(localId)
                if (current != null) {
                    localDataSource.update(
                        current.copy(
                            remoteId = remoteTask.id,
                            syncStatus = SyncStatus.SYNCED,
                        ),
                    )
                }
                writeBackSubtasks(localId, remoteTask.subtasks)
                for ((bytes, mime) in photos) {
                    uploadTaskPhoto(remoteTask.id, bytes, mime).getOrNull()
                }
                refreshPhotoUrlsForTask(remoteTask.id)
            }
            .onFailure {
                // Photos are buffered keyed by the local row id; drained once syncCreatedTask
                // succeeds and we have a remoteId.
                for ((bytes, mime) in photos) {
                    pendingPhotoRepository.queue(localId, bytes, mime)
                }
            }
    }.fold(
        onSuccess = { Result.success(Unit) },
        onFailure = { Result.failure(it) },
    )

    override suspend fun delete(task: Task) {
        val entity = localDataSource.getTaskById(task.id) ?: return
        cancelRecurringAlarmIfNeeded(entity.id, entity.toDomain())
        // Branch on remoteId, NOT syncStatus. A PENDING_UPDATE/PENDING_DELETE row still carries a remoteId,
        // so the task exists on the server and must be deleted there. The old `syncStatus != SYNCED` check
        // deleted such a row locally-only, leaving the server copy intact; the next pull then re-inserted
        // it as SYNCED — a deleted task resurrecting. remoteId == null means it never reached the server.
        val remoteId = entity.remoteId
        if (remoteId == null) {
            localDataSource.delete(entity)
            return
        }
        remoteDataSource
            .deleteTask(remoteId)
            .onSuccess {
                localDataSource.delete(entity)
            }.onFailure { error ->
                // Already gone on the server -> treat as a successful delete (shares K1's 404 tombstone).
                if (error is DomainException.NotFound) {
                    localDataSource.delete(entity)
                } else {
                    localDataSource.update(entity.copy(syncStatus = SyncStatus.PENDING_DELETE))
                }
            }
    }

    override suspend fun updateTaskCompletion(
        id: Long,
        isCompleted: Boolean,
    ) = withContext(ioDispatcher) {
        val current = localDataSource.getTaskById(id) ?: return@withContext
        val steps = localDataSource.getSubtasks(id)
        if (steps.isNotEmpty()) {
            // Staged: the parent checkbox is a snapshot-preserving shortcut (see applyStagedParentCompletion).
            applyStagedParentCompletion(id, steps, isCompleted)
            recomputeParentCompletion(id)
            return@withContext
        }
        if (current.isCompleted == isCompleted) return@withContext
        // SYNCED rows must flip to PENDING_UPDATE so SyncWorker pushes the change; otherwise
        // the next syncRemoteTasksWithLocal reconcile sees the local row as in-sync, compares
        // it to the (still-uncompleted) server row, and overwrites the user's checkbox.
        // PENDING_CREATE/UPDATE/DELETE must NOT be downgraded — SyncWorker pipeline expects
        // those states to be processed in order.
        localDataSource.update(current.copy(isCompleted = isCompleted, syncStatus = current.syncStatus.afterEdit()))
    }

    /**
     * Completing a staged parent snapshots which steps were already done and marks all done; un-completing
     * restores that snapshot so prior progress is preserved (1/3 → ✔ 3/3 → ✗ → 1/3). With no snapshot
     * (the task was completed by ticking every step individually), un-completing clears all steps.
     */
    private suspend fun applyStagedParentCompletion(
        taskId: Long,
        steps: List<SubtaskEntity>,
        complete: Boolean,
    ) {
        if (complete) {
            if (steps.any { !it.isCompleted }) {
                stagedSnapshots[taskId] = steps.filter { it.isCompleted }.map { it.id }.toSet()
            }
            steps.filter { !it.isCompleted }.forEach {
                localDataSource.updateSubtask(it.copy(isCompleted = true, syncStatus = it.syncStatus.afterEdit()))
            }
        } else {
            val snapshot = stagedSnapshots.remove(taskId)
            steps.forEach { step ->
                val nextDone = snapshot?.contains(step.id) ?: false
                if (step.isCompleted != nextDone) {
                    localDataSource.updateSubtask(
                        step.copy(isCompleted = nextDone, syncStatus = step.syncStatus.afterEdit()),
                    )
                }
            }
        }
    }

    override suspend fun getTaskById(id: Long): Task? = withContext(ioDispatcher) {
        val entity = localDataSource.getTaskById(id) ?: return@withContext null
        entity.toDomain().copy(subtasks = localDataSource.getSubtasks(id).map { it.toDomain() })
    }

    override suspend fun fetchRemoteTask(id: Long): Result<Task> = com.todoapp.mobile.common
        .handleRequest { todoApi.getTaskById(id) }
        .map { it.toDomain() }

    override suspend fun uploadTaskPhoto(
        taskId: Long,
        bytes: ByteArray,
        mimeType: String,
    ): Result<String> {
        val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val part = okhttp3.MultipartBody.Part.createFormData("file", "photo.jpg", body)
        return com.todoapp.mobile.common
            .handleRequest { todoApi.uploadTaskPhoto(taskId, part) }
            .map { it.url }
            .onSuccess { refreshPhotoUrlsForTask(taskId) }
    }

    override suspend fun deleteTaskPhoto(
        taskId: Long,
        photoId: Long,
    ): Result<Unit> = com.todoapp.mobile.common
        .handleEmptyRequest { todoApi.deleteTaskPhoto(taskId, photoId) }
        .onSuccess { refreshPhotoUrlsForTask(taskId) }

    override suspend fun refreshPhotoUrls(taskRemoteIds: List<Long>) {
        taskRemoteIds.forEach { refreshPhotoUrlsForTask(it) }
    }

    /** Pull the current photo URL list for a single task and patch the in-memory map. */
    private suspend fun refreshPhotoUrlsForTask(taskId: Long) {
        com.todoapp.mobile.common
            .handleRequest { todoApi.getTaskById(taskId) }
            .onSuccess { data ->
                val current = taskPhotoUrls.value.toMutableMap()
                if (data.photoUrls.isEmpty()) current.remove(taskId) else current[taskId] = data.photoUrls
                taskPhotoUrls.value = current
            }
    }

    override suspend fun update(task: Task) = withContext(ioDispatcher) {
        val taskEntity = localDataSource.getTaskById(task.id)

        // Finish-state (finishedOn) is owned exclusively by setRoutineFinished, never by the edit form
        // (which always carries finishedOn = null). Preserve the stored value across an edit — locally
        // AND on the wire — so editing a finished routine doesn't silently resurrect it.
        val preserved = task.copy(finishedOn = taskEntity?.finishedOn?.let { LocalDate.ofEpochDay(it) })

        // Re-arm or cancel the recurring alarm based on the new recurrence. Always cancel first
        // (no-op if there was no alarm) so a change to NONE clears it. A finished routine stays
        // alarm-less (scheduleRecurringAlarmIfNeeded skips finishedOn != null).
        runCatching { alarmScheduler.cancelRecurring(preserved.id) }
        scheduleRecurringAlarmIfNeeded(preserved.id, preserved)
        // Edits to a non-recurring task can change date/timeStart/reminderOffsetMinutes, all of which
        // affect when the one-shot alarm fires. Cancel + reschedule so the user-visible reminder stays
        // in sync with what they just saved. cancelTask is taskId-based and idempotent (no-op if none scheduled).
        rescheduleOneShotAlarm(preserved)

        if (taskEntity?.syncStatus != SyncStatus.SYNCED) {
            // Preserve the pending KIND by remoteId; don't collapse to PENDING_CREATE. A row that already
            // has a remoteId lives on the server, so an edit is an UPDATE (PUT). Writing PENDING_CREATE made
            // SyncWorker POST instead, and the backend's clientTaskId dedup returned the existing row
            // unchanged -> the edit was silently lost. Only a never-pushed row (remoteId == null) is a CREATE.
            val nextStatus = if (taskEntity?.remoteId == null) {
                SyncStatus.PENDING_CREATE
            } else {
                SyncStatus.PENDING_UPDATE
            }
            localDataSource.update(
                preserved.toEntity(nextStatus).copy(
                    id = preserved.id,
                    remoteId = taskEntity?.remoteId,
                ),
            )
            return@withContext
        }

        val remoteIdForUpdate = checkNotNull(taskEntity.remoteId) {
            "SYNCED task ${taskEntity.id} is missing remoteId"
        }
        remoteDataSource
            .updateTask(remoteIdForUpdate, preserved)
            .onSuccess { remoteTask ->
                localDataSource.update(
                    preserved
                        .toEntity(SyncStatus.SYNCED)
                        .copy(
                            id = taskEntity.id,
                            remoteId = remoteTask.id,
                            photoUrls = remoteTask.photoUrls.joinToString(","),
                        ),
                )
            }.onFailure {
                localDataSource.update(
                    preserved.toEntity(SyncStatus.PENDING_UPDATE).copy(
                        id = taskEntity.id,
                        remoteId = taskEntity.remoteId,
                    ),
                )
            }
    }

    private suspend fun syncDailyCompletionsWindow() {
        val today = LocalDate.now()
        val from = today.minusDays(DAILY_COMPLETION_PAST_DAYS).toEpochDay()
        val to = today.plusDays(DAILY_COMPLETION_FUTURE_DAYS).toEpochDay()
        runCatching {
            val response = todoApi.getTaskDailyCompletions(from, to)
            val items = response.body()?.data?.items ?: return
            val all = localDataSource.observeAll().first()
            val remoteToLocal = all.mapNotNull { e -> e.remoteId?.let { it to e.id } }.toMap()

            // Rows with a pending local change are the source of truth until their push lands — never let
            // the server snapshot overwrite or delete them.
            val pendingKeys = dailyCompletionDao.getPending().map { it.taskId to it.date }.toSet()

            val serverEntities = items.mapNotNull { item ->
                val localId = remoteToLocal[item.taskId] ?: return@mapNotNull null
                TaskDailyCompletionEntity(taskId = localId, date = item.date, completedAt = item.completedAt)
            }
            val serverKeys = serverEntities.map { it.taskId to it.date }.toSet()

            val toUpsert = serverEntities.filter { (it.taskId to it.date) !in pendingKeys }
            if (toUpsert.isNotEmpty()) dailyCompletionDao.upsertAll(toUpsert)

            // Full window reconciliation: a SYNCED local row the server no longer has was uncompleted on
            // another device — delete it so the checkbox clears here too. Pending rows are left alone.
            dailyCompletionDao.getSyncedInWindow(from, to).forEach { local ->
                val key = local.taskId to local.date
                if (key !in serverKeys && key !in pendingKeys) {
                    dailyCompletionDao.delete(local.taskId, local.date)
                }
            }
        }.onFailure { Log.w("syncDailyCompletions", "failed: ${it.message}") }
    }

    private suspend fun scheduleRecurringAlarmIfNeeded(taskId: Long, task: Task) {
        if (task.recurrence == Recurrence.NONE) return
        // A finished routine arms no future alarms — it no longer fires on upcoming days.
        if (task.finishedOn != null) return
        // All-day tasks have a 00:00 placeholder timeStart; honor the user's daily-plan hour
        // (or the 09:00 default) so a daily birthday-style reminder doesn't fire at midnight.
        val effectiveTime = effectiveAlarmTime(task)
        runCatching {
            alarmScheduler.scheduleRecurring(
                taskId = taskId,
                recurrence = task.recurrence,
                anchorDate = task.date,
                hour = effectiveTime.hour,
                minute = effectiveTime.minute,
                message = task.title,
            )
        }.onFailure { Log.w("scheduleRecurring", "failed: ${it.message}") }
    }

    private fun cancelRecurringAlarmIfNeeded(taskId: Long, task: Task) {
        if (task.recurrence == Recurrence.NONE) return
        runCatching { alarmScheduler.cancelRecurring(taskId) }
    }

    private suspend fun rescheduleOneShotAlarm(task: Task) {
        runCatching { alarmScheduler.cancelTask(task.toAlarmItem()) }
        if (task.recurrence != Recurrence.NONE) return
        val offset = task.reminderOffsetMinutes ?: return
        val effectiveTime = effectiveAlarmTime(task)
        val item = task.toAlarmItem(
            remindBeforeMinutes = offset,
            overrideStartTime = effectiveTime.takeIf { task.isAllDay },
        )
        // AlarmManager fires past triggerAtMillis values immediately. Without this guard,
        // any update() on a task whose date+time has passed (e.g. isSecret toggle on an old
        // task) would pop the alarm overlay the moment the row is saved.
        val triggerMillis = item.time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (triggerMillis <= System.currentTimeMillis()) {
            timber.log.Timber.tag("scheduleOneShot").d("skip past trigger taskId=%d", task.id)
            return
        }
        runCatching {
            alarmScheduler.schedule(item, AlarmType.TASK)
        }.onFailure { Log.w("scheduleOneShot", "failed: ${it.message}") }
    }

    private suspend fun effectiveAlarmTime(task: Task): LocalTime = if (task.isAllDay) {
        dailyPlanPreferences.observePlanTime().first() ?: DailyPlanDefaults.DEFAULT_PLAN_TIME
    } else {
        task.timeStart
    }

    override suspend fun syncRemoteTasksWithLocal(): Result<Unit> {
        // Serialize against the push. SYNC_WORK and FETCH_WORK are separate WorkManager unique chains, so
        // without this a standalone push can interleave with this pull's non-atomic reconcile block.
        return syncMutex.withLock { reconcileRemoteIntoLocal() }
    }

    private suspend fun reconcileRemoteIntoLocal(): Result<Unit> {
        val remoteTasks =
            remoteDataSource.getTasks().fold(
                onSuccess = { it },
                onFailure = { return Result.failure(it) },
            )

        // Refresh the in-memory photo-url map so Home/Calendar can show thumbnails.
        timber.log.Timber.tag("TaskFetch").d(
            "syncRemoteTasksWithLocal: ${remoteTasks.tasks.size} tasks, " +
                "${remoteTasks.tasks.count { it.photoUrls.isNotEmpty() }} with photos, " +
                "total ${remoteTasks.tasks.sumOf { it.photoUrls.size }} URLs",
        )
        taskPhotoUrls.value =
            remoteTasks.tasks
                .filter { it.photoUrls.isNotEmpty() }
                .associate { it.id to it.photoUrls }

        val localTasks = localDataSource.observeAll().first()
        val remotePersonal = remoteTasks.tasks.filter { it.familyGroupId == null }
        val remoteIds = remotePersonal.map { it.id }.toSet()
        val localByRemoteId = localTasks
            .mapNotNull { entity -> entity.remoteId?.let { id -> id to entity } }
            .toMap()

        // PENDING_CREATE rows have remoteId=null so they don't show up in localByRemoteId.
        // If a sync runs between insert()'s local row commit and addTask returning, we'd
        // otherwise insert a duplicate of the just-uploaded server task. Match by content
        // (title + date + timeStart + timeEnd) so we can promote the local PENDING_CREATE
        // row to SYNCED instead of inserting a second one.
        val pendingCreateLocals =
            localTasks.filter { it.remoteId == null && it.syncStatus == SyncStatus.PENDING_CREATE }
        val pendingCreateBySignature =
            pendingCreateLocals.associateBy { it.contentSignature() }
        // §4.12: prefer an EXACT match on the client-generated key (robust for similar title/date/time)
        // and fall back to the lossy content signature only for legacy rows that predate the key.
        val pendingCreateByClientId = clientIdIndex(pendingCreateLocals)

        val toInsert = mutableListOf<TaskEntity>()
        val toUpdate = mutableListOf<TaskEntity>()
        val promoted = mutableSetOf<Long>()

        for (remote in remotePersonal) {
            val incoming = remote.toDomain().toEntity()
            reconcileRemote(
                incoming = incoming,
                local = localByRemoteId[remote.id],
                pendingCreateBySignature = pendingCreateBySignature,
                pendingCreateByClientId = pendingCreateByClientId,
                promoted = promoted,
                toInsert = toInsert,
                toUpdate = toUpdate,
            )
        }

        // Tasks that vanished from the server (deleted via chatbot/web/another device).
        // Only purge SYNCED personal rows; never trample pending uploads/updates/deletes.
        val orphaned =
            localTasks.filter { local ->
                local.remoteId != null &&
                    local.syncStatus == SyncStatus.SYNCED &&
                    local.remoteId !in remoteIds
            }

        suspend fun nextOrder(dateEpochDay: Long): Int {
            val current =
                localDataSource
                    .observeByDate(date = dateEpochDay)
                    .first()
                    .maxOfOrNull { it.orderIndex }
                    ?: -1
            return current + 1
        }

        val insertsWithOrder =
            toInsert.map { entity ->
                if (entity.orderIndex != 0) entity else entity.copy(orderIndex = nextOrder(entity.date))
            }

        return runCatching {
            // Pre-delete any local row already holding a remoteId we're about to insert. Closes
            // the race window where a just-created task hadn't committed in Room yet when this
            // sync read its snapshot — without this, the same remoteId ends up in two rows.
            val incomingRemoteIds = insertsWithOrder.mapNotNull { it.remoteId }
            if (incomingRemoteIds.isNotEmpty()) {
                localDataSource.deleteByRemoteIds(incomingRemoteIds)
            }
            if (insertsWithOrder.isNotEmpty()) {
                localDataSource.insertAll(insertsWithOrder)
            }

            // Apply remote-side updates (titles, dates, completion flags changed off-device).
            // Re-arm the recurring alarm in case the schedule moved.
            toUpdate.forEach { updated ->
                localDataSource.update(updated)
                runCatching { alarmScheduler.cancelRecurring(updated.id) }
                scheduleRecurringAlarmIfNeeded(updated.id, updated.toDomain())
            }

            // Apply remote-side deletes. Cancel alarms first so they don't fire for a row
            // that's about to disappear.
            if (orphaned.isNotEmpty()) {
                orphaned.forEach { runCatching { alarmScheduler.cancelRecurring(it.id) } }
                localDataSource.deleteByRemoteIds(orphaned.mapNotNull { it.remoteId })
                timber.log.Timber
                    .tag("TaskFetch")
                    .d("syncRemoteTasksWithLocal: removed %d locally-orphaned tasks", orphaned.size)
            }

            // Safety net: purge any personal-task rows whose remoteId actually belongs to a
            // group task (from stale data on earlier builds). Keeps Home free of dups.
            val groupRemoteIds = groupTaskLocalDataSource.getAllRemoteIds()
            if (groupRemoteIds.isNotEmpty()) localDataSource.deleteByRemoteIds(groupRemoteIds)

            // Reconcile staged-task steps for every personal task still present locally. Re-read the
            // local set so freshly-inserted parents (with their new local ids) are included; match
            // remote tasks by remoteId. reconcileSubtasksFromRemote skips any task with a pending
            // local push, so it never clobbers unsynced step edits.
            val localByRemoteIdAfterSync = localDataSource.observeAll().first()
                .mapNotNull { entity -> entity.remoteId?.let { id -> id to entity } }
                .toMap()
            for (remote in remotePersonal) {
                val localParent = localByRemoteIdAfterSync[remote.id] ?: continue
                reconcileSubtasksFromRemote(localParent, remote.subtasks)
            }

            // Pull per-day completions for daily tasks so the home toggle stays consistent
            // across devices. Window is small enough to fetch eagerly.
            syncDailyCompletionsWindow()
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { t -> Result.failure(DomainException.fromThrowable(t)) },
        )
    }

    /** §4.12: index PENDING_CREATE rows by their client key, skipping legacy rows that have none. */
    private fun clientIdIndex(rows: List<TaskEntity>): Map<String, TaskEntity> {
        return rows.mapNotNull { e -> e.clientTaskId?.let { it to e } }.toMap()
    }

    /**
     * Stable signature used to match a PENDING_CREATE local row against a server-side task
     * when their remoteId hasn't been linked yet. Title + date + start/end times are enough
     * to make duplicate rows extremely unlikely under realistic user behaviour, while keeping
     * the match independent of fields the user can still toggle in the form (description,
     * category, location, etc).
     */
    private fun TaskEntity.contentSignature(): String = "$title|$date|$timeStart|$timeEnd"

    /**
     * Conflict strategy (remote pull vs. local), §5.5: a remote row overwrites the local one ONLY
     * when the local row is clean (SYNCED) and actually differs; any pending local CRUD
     * (PENDING_CREATE/UPDATE/DELETE) always wins and is preserved until it is pushed. There is NO
     * `updatedAt`/last-write-wins timestamp — "differs" is a field-by-field [contentEquals] and
     * "remote wins" is gated purely on the local sync status. A null [local] means no remoteId
     * match: adopt a matching PENDING_CREATE row (idempotency dedup, §4.12) or insert fresh.
     */
    private fun reconcileRemote(
        incoming: TaskEntity,
        local: TaskEntity?,
        pendingCreateBySignature: Map<String, TaskEntity>,
        pendingCreateByClientId: Map<String, TaskEntity>,
        promoted: MutableSet<Long>,
        toInsert: MutableList<TaskEntity>,
        toUpdate: MutableList<TaskEntity>,
    ) {
        if (local != null) {
            if (local.syncStatus == SyncStatus.SYNCED && !local.contentEquals(incoming)) {
                // Off-device edit (chatbot, web, another phone). Reconcile, preserving
                // local id and orderIndex so UI ordering isn't disturbed.
                toUpdate += incoming.copy(id = local.id, orderIndex = local.orderIndex)
            }
            // else: identical, or local has pending CRUD — leave alone.
            return
        }
        // No remoteId match. Match a PENDING_CREATE row before a fresh insert (closes the insert()
        // race). §4.12: exact client-key match first, content signature only as the legacy fallback.
        val pending = incoming.clientTaskId?.let { pendingCreateByClientId[it] }
            ?: pendingCreateBySignature[incoming.contentSignature()]
        if (pending != null && pending.id !in promoted) {
            promoted += pending.id
            toUpdate += incoming.copy(id = pending.id, orderIndex = pending.orderIndex)
        } else {
            toInsert += incoming.copy(id = 0L)
        }
    }

    private fun TaskEntity.contentEquals(other: TaskEntity): Boolean = title == other.title &&
        description == other.description &&
        date == other.date &&
        timeStart == other.timeStart &&
        timeEnd == other.timeEnd &&
        isCompleted == other.isCompleted &&
        isSecret == other.isSecret &&
        category == other.category &&
        customCategoryName == other.customCategoryName &&
        recurrence == other.recurrence &&
        reminderOffsetMinutes == other.reminderOffsetMinutes &&
        isAllDay == other.isAllDay &&
        photoUrls == other.photoUrls &&
        finishedOn == other.finishedOn

    override suspend fun syncLocalTasksToServer(): Result<Unit> = withContext(ioDispatcher) {
        // Serialize the push. SYNC_WORK and FETCH_WORK are separate WorkManager unique chains that BOTH run
        // SyncWorker, so two pushes could otherwise execute concurrently (double POST/DELETE). The shared
        // @Singleton mutex makes the second wait, and also serializes push vs the pull in
        // syncRemoteTasksWithLocal so a standalone push can't interleave with the pull's non-atomic reconcile.
        syncMutex.withLock { pushPendingTasks() }
    }

    private suspend fun pushPendingTasks(): Result<Unit> {
        // Push every pending row before giving up. A single poisoned row (e.g. a permanent 4xx) must not
        // abort the whole batch: SyncWorker's failure short-circuits the FETCH_WORK chain that runs the
        // pull, so one stuck row used to freeze push AND pull on this device until a data wipe. Collect
        // retryable failures and surface only one at the end (so WorkManager retries the transient case);
        // terminal failures (e.g. NotFound, already tombstoned in syncTask) are logged and dropped per-row.
        val nonSyncedTasks = findNonSyncedTasks()
        val retryable = mutableListOf<Throwable>()
        nonSyncedTasks.forEach { taskEntity ->
            syncTask(taskEntity).onFailure { error ->
                if (isRetryable(error)) {
                    Log.e("syncLocalTasksToServer", "retryable failure on task ${taskEntity.id}", error)
                    retryable += error
                } else {
                    Log.e("syncLocalTasksToServer", "dropping poisoned task ${taskEntity.id}", error)
                }
            }
        }
        // Replay any daily-completion toggles whose immediate push failed, inside the same mutex.
        pushPendingDailyCompletions()
        return if (retryable.isEmpty()) Result.success(Unit) else Result.failure(retryable.first())
    }

    // Mirrors SyncWorker's retry set exactly. Keep in lockstep: repo reports "retryable" => the worker
    // retries (capped); repo reports success/terminal => the worker stops. NotFound is deliberately NOT
    // retryable — a permanently-gone row is tombstoned in place (syncTask), never re-pushed.
    private fun isRetryable(error: Throwable): Boolean {
        return error is DomainException.NoInternet ||
            error is DomainException.Server ||
            error is DomainException.Unauthorized
    }

    override suspend fun syncTask(taskEntity: TaskEntity): Result<Unit> = when (taskEntity.syncStatus) {
        SyncStatus.SYNCED -> Result.success(Unit)
        // Defense-in-depth for Y2: a PENDING_CREATE row that already carries a remoteId exists on the
        // server, so PUT it instead of POSTing a duplicate. A genuine create has remoteId == null.
        SyncStatus.PENDING_CREATE ->
            if (taskEntity.remoteId != null) syncUpdatedTask(taskEntity) else syncCreatedTask(taskEntity)
        SyncStatus.PENDING_UPDATE -> syncUpdatedTask(taskEntity)
        SyncStatus.PENDING_DELETE -> syncDeletedTask(taskEntity)
    }

    override suspend fun deleteAllTasks(): Result<Unit> = withContext(ioDispatcher) {
        try {
            localDataSource.deleteAll()
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(DomainException.fromThrowable(t))
        }
    }

    override suspend fun getAllTasks(): Result<Unit> = withContext(ioDispatcher) {
        remoteDataSource.getTasks().fold(
            onSuccess = { tasks ->
                runCatching {
                    val nextByDate = mutableMapOf<Long, Int>()

                    suspend fun next(dateEpochDay: Long): Int {
                        val current =
                            nextByDate[dateEpochDay]
                                ?: (
                                    localDataSource
                                        .observeByDate(date = dateEpochDay)
                                        .first()
                                        .maxOfOrNull { it.orderIndex }
                                        ?: -1
                                    )
                        val n = current + 1
                        nextByDate[dateEpochDay] = n
                        return n
                    }

                    val entities =
                        tasks.tasks
                            .map { it.toDomain().toEntity().copy(id = 0L) }
                            .map { entity ->
                                if (entity.orderIndex != 0) {
                                    entity
                                } else {
                                    entity.copy(orderIndex = next(entity.date))
                                }
                            }

                    localDataSource.insertAll(entities)
                }.fold(
                    onSuccess = { Result.success(Unit) },
                    onFailure = { t -> Result.failure(DomainException.fromThrowable(t)) },
                )
            },
            onFailure = { t ->
                Result.failure(DomainException.fromThrowable(t))
            },
        )
    }

    private suspend fun syncDeletedTask(taskEntity: TaskEntity): Result<Unit> {
        val remoteId = checkNotNull(taskEntity.remoteId) {
            "syncDeletedTask called with locally-only task ${taskEntity.id}"
        }
        val remoteResult = remoteDataSource.deleteTask(remoteId)

        return remoteResult.fold(
            onSuccess = {
                runCatching { localDataSource.delete(taskEntity) }
            },
            onFailure = { error ->
                // Already gone on the server (another device/chat deleted it, or our own DELETE committed
                // before the response timed out). A 404 here IS a successful delete — tombstone locally so
                // it stops blocking the push queue instead of failing forever.
                if (error is DomainException.NotFound) {
                    runCatching { localDataSource.delete(taskEntity) }
                } else {
                    Result.failure(error)
                }
            },
        )
    }

    private suspend fun syncUpdatedTask(taskEntity: TaskEntity): Result<Unit> {
        val remoteId = checkNotNull(taskEntity.remoteId) {
            "syncUpdatedTask called with locally-only task ${taskEntity.id}"
        }
        // Steps travel with the parent task (one aggregate). For a plain task this is empty, which
        // toCreateTaskRequestDto turns into subtasks=null so the server leaves any steps untouched.
        val localSubtasks = localDataSource.getSubtasks(taskEntity.id).map { it.toDomain() }
        val remoteResult = remoteDataSource.updateTask(remoteId, taskEntity.toDomain().copy(subtasks = localSubtasks))

        return remoteResult.fold(
            onSuccess = { remoteTask ->
                val updated =
                    taskEntity.copy(
                        remoteId = remoteTask.id,
                        syncStatus = SyncStatus.SYNCED,
                    )

                runCatching {
                    localDataSource.update(updated)
                    writeBackSubtasks(taskEntity.id, remoteTask.subtasks)
                }
            },
            onFailure = { error ->
                // We hold a pending edit for a task that was deleted elsewhere. Re-POSTing would resurrect
                // a task another device/user deleted, so tombstone the local row instead of retrying.
                if (error is DomainException.NotFound) {
                    runCatching { localDataSource.delete(taskEntity) }
                } else {
                    Result.failure(error)
                }
            },
        )
    }

    private suspend fun syncCreatedTask(taskEntity: TaskEntity): Result<Unit> {
        val localSubtasks = localDataSource.getSubtasks(taskEntity.id).map { it.toDomain() }
        val remoteResult = remoteDataSource.addTask(taskEntity.toDomain().copy(subtasks = localSubtasks))

        return remoteResult.fold(
            onSuccess = { remoteTask ->
                val updated =
                    taskEntity.copy(
                        remoteId = remoteTask.id,
                        syncStatus = SyncStatus.SYNCED,
                    )

                runCatching {
                    localDataSource.update(updated)
                    writeBackSubtasks(taskEntity.id, remoteTask.subtasks)
                    pendingPhotoRepository.drain(taskEntity.id, remoteTask.id) { bytes, mime ->
                        uploadTaskPhoto(remoteTask.id, bytes, mime).map {}
                    }
                    refreshPhotoUrlsForTask(remoteTask.id)
                }
            },
            onFailure = {
                Result.failure(it)
            },
        )
    }

    override suspend fun findNonSyncedTasks(): List<TaskEntity> {
        val tasks = observeAllTaskEntities().first()
        val nonSyncedTasks = tasks.filter { it.syncStatus != SyncStatus.SYNCED }
        return nonSyncedTasks
    }

    private suspend fun nextOrderForDate(dateEpochDay: Long): Int {
        val current =
            localDataSource
                .observeByDate(date = dateEpochDay)
                .first()
                .maxOfOrNull { it.orderIndex }
                ?: -1
        return current + 1
    }

    private suspend fun withInitializedOrder(entity: TaskEntity): TaskEntity = if (entity.orderIndex != 0) {
        entity
    } else {
        entity.copy(
            orderIndex =
            nextOrderForDate(
                entity.date,
            ),
        )
    }

    override fun searchTasks(query: String): Flow<List<Task>> {
        val likeQuery = "%$query%"
        return localDataSource.search(likeQuery).map { list -> list.map { it.toDomain() } }.withSubtaskCounts()
    }

    override fun observeTasksByWeekAndStatus(
        date: LocalDate,
        isCompleted: Boolean,
    ): Flow<List<Task>> {
        val weekStart = date.with(DayOfWeek.MONDAY)
        val weekEnd = weekStart.plusDays(DAYS_TO_ADD.toLong())
        return localDataSource
            .observeByWeekAndStatus(
                startDate = weekStart.toEpochDay(),
                endDate = weekEnd.toEpochDay(),
                isCompleted = isCompleted,
            ).map { list -> list.map { it.toDomain() } }
            .withSubtaskCounts()
    }

    override suspend fun reorderTasksForDate(
        date: LocalDate,
        fromIndex: Int,
        toIndex: Int,
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            if (fromIndex == toIndex) return@runCatching

            val current =
                localDataSource
                    .observeByDate(date.toEpochDay())
                    .first()

            if (fromIndex !in current.indices || toIndex !in current.indices) {
                return@runCatching
            }

            val reordered =
                current.toMutableList().apply {
                    add(toIndex, removeAt(fromIndex))
                }

            val start = minOf(fromIndex, toIndex)
            val end = maxOf(fromIndex, toIndex)

            val updates =
                (start..end).map { index ->
                    reordered[index].id to index
                }

            localDataSource.updateOrderIndices(updates)
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { t -> Result.failure(DomainException.fromThrowable(t)) },
        )
    }

    override fun observeSubtasks(taskId: Long): Flow<List<Subtask>> = localDataSource.observeSubtasks(taskId).map { list -> list.map { it.toDomain() } }

    override suspend fun addSubtask(taskId: Long, title: String) = withContext(ioDispatcher) {
        val order = localDataSource.countSubtasks(taskId)
        localDataSource.insertSubtask(
            SubtaskEntity(title = title, parentTaskId = taskId, orderIndex = order),
        )
        // Adding an incomplete step reopens a parent that had been auto-completed.
        recomputeParentCompletion(taskId)
        markParentPendingUpdate(taskId)
    }

    override suspend fun toggleSubtask(subtaskId: Long, isCompleted: Boolean) = withContext(ioDispatcher) {
        val subtask = localDataSource.getSubtaskById(subtaskId) ?: return@withContext
        if (subtask.isCompleted != isCompleted) {
            localDataSource.updateSubtask(
                subtask.copy(isCompleted = isCompleted, syncStatus = subtask.syncStatus.afterEdit()),
            )
        }
        // An individual step edit invalidates any cascade snapshot for this parent.
        stagedSnapshots.remove(subtask.parentTaskId)
        recomputeParentCompletion(subtask.parentTaskId)
        markParentPendingUpdate(subtask.parentTaskId)
    }

    override suspend fun deleteSubtask(subtaskId: Long) = withContext(ioDispatcher) {
        val subtask = localDataSource.getSubtaskById(subtaskId) ?: return@withContext
        // A staged task keeps at least one step so it never silently degrades into a plain task.
        if (localDataSource.countSubtasks(subtask.parentTaskId) <= 1) return@withContext
        localDataSource.deleteSubtask(subtask)
        recomputeParentCompletion(subtask.parentTaskId)
        markParentPendingUpdate(subtask.parentTaskId)
    }

    override suspend fun updateSubtaskTitle(subtaskId: Long, title: String) = withContext(ioDispatcher) {
        val subtask = localDataSource.getSubtaskById(subtaskId) ?: return@withContext
        if (subtask.title != title) {
            localDataSource.updateSubtask(
                subtask.copy(title = title, syncStatus = subtask.syncStatus.afterEdit()),
            )
            markParentPendingUpdate(subtask.parentTaskId)
        }
    }

    /** Reapplies the staged invariant: the parent is done iff it has steps and all of them are done. */
    private suspend fun recomputeParentCompletion(taskId: Long) {
        val subtasks = localDataSource.getSubtasks(taskId)
        if (subtasks.isEmpty()) return
        val allDone = subtasks.all { it.isCompleted }
        val parent = localDataSource.getTaskById(taskId) ?: return
        if (parent.isCompleted != allDone) {
            localDataSource.update(
                parent.copy(isCompleted = allDone, syncStatus = parent.syncStatus.afterEdit()),
            )
        }
    }

    private fun SyncStatus.afterEdit(): SyncStatus = if (this == SyncStatus.SYNCED) SyncStatus.PENDING_UPDATE else this

    /**
     * Steps ride along with their parent task on sync (the whole staged task is one aggregate).
     * A step edit doesn't touch the `tasks` row, and SyncWorker only walks task rows, so flip the
     * parent to PENDING_UPDATE here — otherwise the step change is invisible to the pusher.
     */
    private suspend fun markParentPendingUpdate(taskId: Long) {
        val parent = localDataSource.getTaskById(taskId) ?: return
        if (parent.syncStatus == SyncStatus.SYNCED) {
            localDataSource.update(parent.copy(syncStatus = SyncStatus.PENDING_UPDATE))
        }
    }

    /**
     * Mirrors the server's step set onto the local rows after a push (create/update) returns. We
     * full-replace rather than match, so local rows pick up their server `remoteId` and land as
     * SYNCED. The parent's local id is unchanged, so this never trips the subtasks' CASCADE FK.
     */
    private suspend fun writeBackSubtasks(localParentId: Long, remote: List<SubtaskData>) {
        localDataSource.deleteSubtasksByTask(localParentId)
        if (remote.isEmpty()) return
        localDataSource.insertSubtasks(
            remote.sortedBy { it.orderIndex }.mapIndexed { index, s ->
                SubtaskEntity(
                    title = s.title,
                    parentTaskId = localParentId,
                    isCompleted = s.isCompleted,
                    orderIndex = index,
                    syncStatus = SyncStatus.SYNCED,
                    remoteId = s.id,
                )
            },
        )
    }

    /**
     * Pull-side step reconcile: bring a synced parent's local steps in line with the server's set
     * (covers steps added/edited/deleted on another device — even when the parent's own fields are
     * unchanged). Skips any task with a pending local push so we never overwrite unsynced edits;
     * the local push wins and the next pull reconciles afterwards.
     */
    private suspend fun reconcileSubtasksFromRemote(localParent: TaskEntity, remote: List<SubtaskData>) {
        if (localParent.syncStatus != SyncStatus.SYNCED) return
        val locals = localDataSource.getSubtasks(localParent.id)
        if (locals.any { it.syncStatus != SyncStatus.SYNCED }) return
        if (subtasksMatch(locals, remote)) return
        writeBackSubtasks(localParent.id, remote)
    }

    /** True when local steps already equal the server set (by remoteId + title + completion). */
    private fun subtasksMatch(locals: List<SubtaskEntity>, remote: List<SubtaskData>): Boolean {
        if (locals.size != remote.size) return false
        val localSorted = locals.sortedBy { it.orderIndex }
        val remoteSorted = remote.sortedBy { it.orderIndex }
        return localSorted.zip(remoteSorted).all { (l, r) ->
            l.remoteId == r.id && l.title == r.title && l.isCompleted == r.isCompleted
        }
    }

    /** Decorates a task-list flow with cheap staged-progress counts (subtaskTotal / subtaskDone). */
    private fun Flow<List<Task>>.withSubtaskCounts(): Flow<List<Task>> = combine(localDataSource.observeSubtaskCounts()) { tasks, counts ->
        if (counts.isEmpty()) {
            tasks
        } else {
            val byId: Map<Long, SubtaskCount> = counts.associateBy { it.taskId }
            tasks.map { task ->
                val count = byId[task.id]
                if (count != null) task.copy(subtaskTotal = count.total, subtaskDone = count.done) else task
            }
        }
    }

    companion object {
        private const val DAYS_TO_ADD = 6
        private const val DAYS_IN_WEEK = 7
        private const val DAILY_COMPLETION_PAST_DAYS = 30L
        private const val DAILY_COMPLETION_FUTURE_DAYS = 7L
    }
}
