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
import com.todoapp.mobile.data.model.entity.SyncStatus
import com.todoapp.mobile.data.model.entity.TaskDailyCompletionEntity
import com.todoapp.mobile.data.model.entity.TaskEntity
import com.todoapp.mobile.data.source.local.TaskDailyCompletionDao
import com.todoapp.mobile.data.source.local.datasource.GroupTaskLocalDataSource
import com.todoapp.mobile.data.source.local.datasource.TaskLocalDataSource
import com.todoapp.mobile.data.source.remote.datasource.TaskRemoteDataSource
import com.todoapp.mobile.di.IoDispatcher
import com.todoapp.mobile.domain.alarm.AlarmScheduler
import com.todoapp.mobile.domain.alarm.AlarmType
import com.todoapp.mobile.domain.constants.DailyPlanDefaults
import com.todoapp.mobile.domain.model.Recurrence
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
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

@Suppress("LargeClass")
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
        }

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
                if (!rule.firesOn(anchor, date)) return@mapNotNull null
                entity.toDomain().copy(
                    date = date,
                    isCompleted = entity.id in completedTaskIds,
                )
            }
            nonRecurring + recurringInstances
        }
    }

    override fun observeRecurringByType(recurrence: Recurrence): Flow<List<Task>> {
        if (recurrence == Recurrence.NONE) return kotlinx.coroutines.flow.flowOf(emptyList())
        return localDataSource.observeByRecurrence(recurrence.name).map { list ->
            list.map { it.toDomain() }
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
            dailyCompletionDao.upsert(
                TaskDailyCompletionEntity(
                    taskId = taskId,
                    date = epochDay,
                    completedAt = System.currentTimeMillis(),
                ),
            )
        } else {
            dailyCompletionDao.delete(taskId, epochDay)
        }
        runCatching {
            val task = localDataSource.getTaskById(taskId) ?: return@runCatching
            val remoteId = task.remoteId ?: return@runCatching
            todoApi.setTaskDailyCompletion(
                remoteId,
                com.todoapp.mobile.data.model.network.request.TaskDailyCompletionRequest(
                    date = epochDay,
                    completed = completed,
                ),
            )
        }.onFailure { Log.w("setDailyCompletion", "remote sync failed: ${it.message}") }
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
                for (offset in 0 until totalDays) {
                    val day = startDate.plusDays(offset.toLong())
                    if (recurrence.firesOn(anchor, day)) {
                        if (entity.id to day.toEpochDay() in completionKeys) {
                            completed.merge(day, 1, Int::plus)
                        } else {
                            pending.merge(day, 1, Int::plus)
                        }
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
        val localEntity = task.toEntity(SyncStatus.PENDING_CREATE).copy(id = 0L)
        val localId = localDataSource.insert(withInitializedOrder(localEntity))
        scheduleRecurringAlarmIfNeeded(localId, task)

        remoteDataSource
            .addTask(task)
            .onSuccess { remoteTask ->
                val current = localDataSource.getTaskById(localId) ?: return@onSuccess
                localDataSource.update(
                    current.copy(
                        remoteId = remoteTask.id,
                        syncStatus = SyncStatus.SYNCED,
                        photoUrls = remoteTask.photoUrls.joinToString(","),
                    ),
                )
            }
        // onFailure: row already exists as PENDING_CREATE, SyncWorker will push it later.
    }

    override suspend fun insertWithPhotos(
        task: Task,
        photos: List<Pair<ByteArray, String>>,
    ): Result<Unit> = runCatching {
        // Same local-first pattern as insert(); see comment above for the race rationale.
        val localEntity = task.toEntity(SyncStatus.PENDING_CREATE).copy(id = 0L)
        val localId = localDataSource.insert(withInitializedOrder(localEntity))

        remoteDataSource
            .addTask(task)
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
        if (entity.syncStatus != SyncStatus.SYNCED) {
            localDataSource.delete(entity)
            return
        }
        val remoteId = checkNotNull(entity.remoteId) {
            "SYNCED task ${entity.id} is missing remoteId"
        }
        remoteDataSource
            .deleteTask(remoteId)
            .onSuccess {
                localDataSource.delete(entity)
            }.onFailure {
                localDataSource.update(entity.copy(syncStatus = SyncStatus.PENDING_DELETE))
            }
    }

    override suspend fun updateTaskCompletion(
        id: Long,
        isCompleted: Boolean,
    ) = withContext(ioDispatcher) {
        val current = localDataSource.getTaskById(id) ?: return@withContext
        if (current.isCompleted == isCompleted) return@withContext
        // SYNCED rows must flip to PENDING_UPDATE so SyncWorker pushes the change; otherwise
        // the next syncRemoteTasksWithLocal reconcile sees the local row as in-sync, compares
        // it to the (still-uncompleted) server row, and overwrites the user's checkbox.
        // PENDING_CREATE/UPDATE/DELETE must NOT be downgraded — SyncWorker pipeline expects
        // those states to be processed in order.
        val nextSyncStatus = when (current.syncStatus) {
            SyncStatus.SYNCED -> SyncStatus.PENDING_UPDATE
            else -> current.syncStatus
        }
        localDataSource.update(current.copy(isCompleted = isCompleted, syncStatus = nextSyncStatus))
    }

    override suspend fun getTaskById(id: Long): Task? = withContext(ioDispatcher) {
        localDataSource.getTaskById(id)?.toDomain()
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

        // Re-arm or cancel the recurring alarm based on the new recurrence. Always cancel first
        // (no-op if there was no alarm) so a change to NONE clears it.
        runCatching { alarmScheduler.cancelRecurring(task.id) }
        scheduleRecurringAlarmIfNeeded(task.id, task)
        // Edits to a non-recurring task can change date/timeStart/reminderOffsetMinutes, all of which
        // affect when the one-shot alarm fires. Cancel + reschedule so the user-visible reminder stays
        // in sync with what they just saved. cancelTask is taskId-based and idempotent (no-op if none scheduled).
        rescheduleOneShotAlarm(task)

        if (taskEntity?.syncStatus != SyncStatus.SYNCED) {
            // no need to update remote because its not synced
            localDataSource.update(
                task.toEntity(SyncStatus.PENDING_CREATE).copy(
                    id = task.id,
                    remoteId = taskEntity?.remoteId,
                ),
            )
            return@withContext
        }

        val remoteIdForUpdate = checkNotNull(taskEntity.remoteId) {
            "SYNCED task ${taskEntity.id} is missing remoteId"
        }
        remoteDataSource
            .updateTask(remoteIdForUpdate, task)
            .onSuccess { remoteTask ->
                localDataSource.update(
                    task
                        .toEntity(SyncStatus.SYNCED)
                        .copy(
                            id = taskEntity.id,
                            remoteId = remoteTask.id,
                            photoUrls = remoteTask.photoUrls.joinToString(","),
                        ),
                )
            }.onFailure {
                localDataSource.update(
                    task.toEntity(SyncStatus.PENDING_UPDATE).copy(
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
            // Map remoteId → localId once
            val all = localDataSource.observeAll().first()
            val remoteToLocal = all.mapNotNull { e -> e.remoteId?.let { it to e.id } }.toMap()
            val entities = items.mapNotNull { item ->
                val localId = remoteToLocal[item.taskId] ?: return@mapNotNull null
                TaskDailyCompletionEntity(
                    taskId = localId,
                    date = item.date,
                    completedAt = item.completedAt,
                )
            }
            if (entities.isNotEmpty()) dailyCompletionDao.upsertAll(entities)
        }.onFailure { Log.w("syncDailyCompletions", "failed: ${it.message}") }
    }

    private suspend fun scheduleRecurringAlarmIfNeeded(taskId: Long, task: Task) {
        if (task.recurrence == Recurrence.NONE) return
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

        val toInsert = mutableListOf<TaskEntity>()
        val toUpdate = mutableListOf<TaskEntity>()
        val promoted = mutableSetOf<Long>()

        for (remote in remotePersonal) {
            val incoming = remote.toDomain().toEntity()
            reconcileRemote(
                incoming = incoming,
                local = localByRemoteId[remote.id],
                pendingCreateBySignature = pendingCreateBySignature,
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
            // Pull per-day completions for daily tasks so the home toggle stays consistent
            // across devices. Window is small enough to fetch eagerly.
            syncDailyCompletionsWindow()
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { t -> Result.failure(DomainException.fromThrowable(t)) },
        )
    }

    /**
     * Stable signature used to match a PENDING_CREATE local row against a server-side task
     * when their remoteId hasn't been linked yet. Title + date + start/end times are enough
     * to make duplicate rows extremely unlikely under realistic user behaviour, while keeping
     * the match independent of fields the user can still toggle in the form (description,
     * category, location, etc).
     */
    private fun TaskEntity.contentSignature(): String = "$title|$date|$timeStart|$timeEnd"

    private fun reconcileRemote(
        incoming: TaskEntity,
        local: TaskEntity?,
        pendingCreateBySignature: Map<String, TaskEntity>,
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
        // No remoteId match. Try a content-signature match against PENDING_CREATE rows
        // before falling through to a fresh insert (closes the insert() race).
        val pending = pendingCreateBySignature[incoming.contentSignature()]
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
        photoUrls == other.photoUrls

    override suspend fun syncLocalTasksToServer(): Result<Unit> = withContext(ioDispatcher) {
        val nonSyncedTasks = findNonSyncedTasks()
        nonSyncedTasks.forEach { taskEntity ->
            syncTask(taskEntity).onFailure {
                Log.e("syncLocalTasksToServer", it.message ?: "Unknown error")
                return@withContext Result.failure(it)
            }
        }
        Result.success(Unit)
    }

    override suspend fun syncTask(taskEntity: TaskEntity): Result<Unit> = when (taskEntity.syncStatus) {
        SyncStatus.SYNCED -> Result.success(Unit)
        SyncStatus.PENDING_CREATE -> syncCreatedTask(taskEntity)
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
            onFailure = {
                Result.failure(it)
            },
        )
    }

    private suspend fun syncUpdatedTask(taskEntity: TaskEntity): Result<Unit> {
        val remoteId = checkNotNull(taskEntity.remoteId) {
            "syncUpdatedTask called with locally-only task ${taskEntity.id}"
        }
        val remoteResult = remoteDataSource.updateTask(remoteId, taskEntity.toDomain())

        return remoteResult.fold(
            onSuccess = { remoteTask ->
                val updated =
                    taskEntity.copy(
                        remoteId = remoteTask.id,
                        syncStatus = SyncStatus.SYNCED,
                    )

                runCatching { localDataSource.update(updated) }
            },
            onFailure = {
                Result.failure(it)
            },
        )
    }

    private suspend fun syncCreatedTask(taskEntity: TaskEntity): Result<Unit> {
        val remoteResult = remoteDataSource.addTask(taskEntity.toDomain())

        return remoteResult.fold(
            onSuccess = { remoteTask ->
                val updated =
                    taskEntity.copy(
                        remoteId = remoteTask.id,
                        syncStatus = SyncStatus.SYNCED,
                    )

                runCatching {
                    localDataSource.update(updated)
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
        return localDataSource.search(likeQuery).map { list -> list.map { it.toDomain() } }
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

    companion object {
        private const val DAYS_TO_ADD = 6
        private const val DAYS_IN_WEEK = 7
        private const val DAILY_COMPLETION_PAST_DAYS = 30L
        private const val DAILY_COMPLETION_FUTURE_DAYS = 7L
    }
}
