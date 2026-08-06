package com.todoapp.mobile.data.repository

import android.util.Log
import com.todoapp.mobile.common.DomainException
import com.todoapp.mobile.data.model.entity.SubtaskEntity
import com.todoapp.mobile.data.model.entity.SyncStatus
import com.todoapp.mobile.data.model.entity.TaskEntity
import com.todoapp.mobile.data.model.entity.TaskReminderEntity
import com.todoapp.mobile.data.model.network.data.TaskData
import com.todoapp.mobile.data.model.network.data.TaskListData
import com.todoapp.mobile.data.source.local.SubtaskDailyCompletionDao
import com.todoapp.mobile.data.source.local.TaskDailyCompletionDao
import com.todoapp.mobile.data.source.local.datasource.TaskLocalDataSource
import com.todoapp.mobile.data.source.remote.datasource.TaskRemoteDataSource
import com.todoapp.mobile.domain.alarm.AlarmScheduler
import com.todoapp.mobile.domain.model.AlarmItem
import com.todoapp.mobile.domain.model.Task
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Characterization tests pinning the §7.2 sync invariants that were bug-fixed on 2026-07-03/04.
 * All collaborators are mocked, so TaskRepositoryImpl instantiates on the plain JVM; android.util.Log
 * is only reached on the push path and is statically mocked below.
 */
class TaskRepositoryImplTest {
    private val localDataSource = mockk<TaskLocalDataSource>(relaxed = true)
    private val remoteDataSource = mockk<TaskRemoteDataSource>(relaxed = true)
    private val dailyCompletionDao = mockk<TaskDailyCompletionDao>(relaxed = true)
    private val subtaskDailyCompletionDao = mockk<SubtaskDailyCompletionDao>(relaxed = true)
    private val alarmScheduler = mockk<AlarmScheduler>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any<Throwable>()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun repository() = TaskRepositoryImpl(
        remoteDataSource = remoteDataSource,
        localDataSource = localDataSource,
        groupTaskLocalDataSource = mockk(relaxed = true),
        todoApi = mockk(relaxed = true),
        pendingPhotoRepository = mockk(relaxed = true),
        dailyCompletionDao = dailyCompletionDao,
        subtaskDailyCompletionDao = subtaskDailyCompletionDao,
        alarmScheduler = alarmScheduler,
        dailyPlanPreferences = mockk(relaxed = true),
        analyticsHelper = mockk(relaxed = true),
        ioDispatcher = Dispatchers.Unconfined,
    )

    // --- delete: branches on remoteId presence, NOT syncStatus (2026-07 fix) ---

    @Test
    fun `delete with no remoteId removes locally and never calls the server`() = runTest {
        val entity = taskEntity(id = 1L, remoteId = null, syncStatus = SyncStatus.PENDING_CREATE)
        coEvery { localDataSource.getTaskById(1L) } returns entity

        repository().delete(domainTask(id = 1L))

        coVerify(exactly = 1) { localDataSource.delete(entity) }
        coVerify(exactly = 0) { remoteDataSource.deleteTask(any()) }
    }

    @Test
    fun `delete with a remoteId deletes on the server then locally`() = runTest {
        val entity = taskEntity(id = 1L, remoteId = 99L, syncStatus = SyncStatus.SYNCED)
        coEvery { localDataSource.getTaskById(1L) } returns entity
        coEvery { remoteDataSource.deleteTask(99L) } returns Result.success(Unit)

        repository().delete(domainTask(id = 1L))

        coVerify(exactly = 1) { remoteDataSource.deleteTask(99L) }
        coVerify(exactly = 1) { localDataSource.delete(entity) }
    }

    @Test
    fun `delete tombstones a 404 as a successful local delete`() = runTest {
        val entity = taskEntity(id = 1L, remoteId = 99L, syncStatus = SyncStatus.SYNCED)
        coEvery { localDataSource.getTaskById(1L) } returns entity
        coEvery { remoteDataSource.deleteTask(99L) } returns Result.failure(DomainException.NotFound())

        repository().delete(domainTask(id = 1L))

        coVerify(exactly = 1) { localDataSource.delete(entity) }
        coVerify(exactly = 0) { localDataSource.update(any()) }
    }

    @Test
    fun `delete keeps the row as PENDING_DELETE on a transient server failure`() = runTest {
        val entity = taskEntity(id = 1L, remoteId = 99L, syncStatus = SyncStatus.SYNCED)
        coEvery { localDataSource.getTaskById(1L) } returns entity
        coEvery { remoteDataSource.deleteTask(99L) } returns Result.failure(DomainException.Server("boom"))

        repository().delete(domainTask(id = 1L))

        coVerify(exactly = 1) {
            localDataSource.update(entity.copy(syncStatus = SyncStatus.PENDING_DELETE))
        }
        coVerify(exactly = 0) { localDataSource.delete(any()) }
    }

    // --- updateTaskCompletion: a SYNCED row must flip to PENDING_UPDATE so the worker pushes it ---

    @Test
    fun `completing a SYNCED task flips it to PENDING_UPDATE`() = runTest {
        val entity = taskEntity(id = 5L, isCompleted = false, syncStatus = SyncStatus.SYNCED)
        coEvery { localDataSource.getTaskById(5L) } returns entity
        coEvery { localDataSource.getSubtasks(5L) } returns emptyList()

        repository().updateTaskCompletion(id = 5L, isCompleted = true)

        coVerify(exactly = 1) {
            localDataSource.update(entity.copy(isCompleted = true, syncStatus = SyncStatus.PENDING_UPDATE))
        }
    }

    @Test
    fun `completing a still-unsynced PENDING_CREATE task keeps it PENDING_CREATE`() = runTest {
        val entity = taskEntity(id = 5L, isCompleted = false, syncStatus = SyncStatus.PENDING_CREATE)
        coEvery { localDataSource.getTaskById(5L) } returns entity
        coEvery { localDataSource.getSubtasks(5L) } returns emptyList()

        repository().updateTaskCompletion(id = 5L, isCompleted = true)

        coVerify(exactly = 1) {
            localDataSource.update(entity.copy(isCompleted = true, syncStatus = SyncStatus.PENDING_CREATE))
        }
    }

    @Test
    fun `toggling a task to the state it is already in is a no-op`() = runTest {
        val entity = taskEntity(id = 5L, isCompleted = true, syncStatus = SyncStatus.SYNCED)
        coEvery { localDataSource.getTaskById(5L) } returns entity
        coEvery { localDataSource.getSubtasks(5L) } returns emptyList()

        repository().updateTaskCompletion(id = 5L, isCompleted = true)

        coVerify(exactly = 0) { localDataSource.update(any()) }
    }

    // --- staged parent completion: snapshot the done steps on complete, restore them on un-complete ---

    @Test
    fun `completing a staged parent marks only the undone steps done`() = runTest {
        val steps = listOf(
            subtask(id = 101L, done = true),
            subtask(id = 102L, done = false),
            subtask(id = 103L, done = false),
        )
        coEvery { localDataSource.getTaskById(10L) } returns taskEntity(id = 10L, syncStatus = SyncStatus.SYNCED)
        coEvery { localDataSource.getSubtasks(10L) } returns steps

        repository().updateTaskCompletion(id = 10L, isCompleted = true)

        coVerify(exactly = 1) { localDataSource.updateSubtask(match { it.id == 102L && it.isCompleted }) }
        coVerify(exactly = 1) { localDataSource.updateSubtask(match { it.id == 103L && it.isCompleted }) }
        coVerify(exactly = 0) { localDataSource.updateSubtask(match { it.id == 101L }) }
    }

    @Test
    fun `un-completing a staged parent restores the pre-completion snapshot`() = runTest {
        val repo = repository()
        val partial = listOf(
            subtask(id = 101L, done = true),
            subtask(id = 102L, done = false),
            subtask(id = 103L, done = false),
        )
        coEvery { localDataSource.getTaskById(10L) } returns taskEntity(id = 10L, syncStatus = SyncStatus.SYNCED)
        coEvery { localDataSource.getSubtasks(10L) } returns partial
        repo.updateTaskCompletion(id = 10L, isCompleted = true) // snapshots {101}, marks 102/103 done

        coEvery { localDataSource.getSubtasks(10L) } returns partial.map { it.copy(isCompleted = true) }
        repo.updateTaskCompletion(id = 10L, isCompleted = false) // restore: only 101 stays done

        coVerify { localDataSource.updateSubtask(match { it.id == 102L && !it.isCompleted }) }
        coVerify { localDataSource.updateSubtask(match { it.id == 103L && !it.isCompleted }) }
    }

    // --- reconcileRemote: server overwrites local only when local is SYNCED and differs (§5.5) ---

    @Test
    fun `reconcile overwrites a SYNCED local row when the server copy differs`() {
        val local = taskEntity(id = 1L, remoteId = 50L, syncStatus = SyncStatus.SYNCED)
        val incoming = local.copy(title = "edited on another device")
        val toInsert = mutableListOf<TaskEntity>()
        val toUpdate = mutableListOf<TaskEntity>()

        repository().reconcileRemote(incoming, local, emptyMap(), emptyMap(), mutableSetOf(), toInsert, toUpdate)

        assertEquals(1, toUpdate.size)
        assertEquals(1L, toUpdate.first().id) // keeps the local id/order
        assertEquals("edited on another device", toUpdate.first().title)
        assertEquals(0, toInsert.size)
    }

    @Test
    fun `reconcile leaves a locally-edited pending row untouched`() {
        val local = taskEntity(id = 1L, remoteId = 50L, syncStatus = SyncStatus.PENDING_UPDATE)
        val incoming = local.copy(title = "server version", syncStatus = SyncStatus.SYNCED)
        val toInsert = mutableListOf<TaskEntity>()
        val toUpdate = mutableListOf<TaskEntity>()

        repository().reconcileRemote(incoming, local, emptyMap(), emptyMap(), mutableSetOf(), toInsert, toUpdate)

        assertEquals(0, toUpdate.size)
        assertEquals(0, toInsert.size)
    }

    @Test
    fun `reconcile is a no-op when a SYNCED local row already matches the server`() {
        val local = taskEntity(id = 1L, remoteId = 50L, syncStatus = SyncStatus.SYNCED)
        val toInsert = mutableListOf<TaskEntity>()
        val toUpdate = mutableListOf<TaskEntity>()

        repository().reconcileRemote(local.copy(), local, emptyMap(), emptyMap(), mutableSetOf(), toInsert, toUpdate)

        assertEquals(0, toUpdate.size)
        assertEquals(0, toInsert.size)
    }

    @Test
    fun `reconcile inserts a brand-new server row with no local match`() {
        val incoming = taskEntity(id = 77L, remoteId = 88L, syncStatus = SyncStatus.SYNCED)
        val toInsert = mutableListOf<TaskEntity>()
        val toUpdate = mutableListOf<TaskEntity>()

        repository().reconcileRemote(incoming, null, emptyMap(), emptyMap(), mutableSetOf(), toInsert, toUpdate)

        assertEquals(1, toInsert.size)
        assertEquals(0L, toInsert.first().id) // id reset so Room autogenerates
        assertEquals(0, toUpdate.size)
    }

    @Test
    fun `reconcile promotes a matching pending-create row instead of inserting a duplicate`() {
        val incoming = taskEntity(id = 77L, remoteId = 88L, syncStatus = SyncStatus.SYNCED).copy(clientTaskId = "uuid-1")
        val pending = taskEntity(id = 5L, syncStatus = SyncStatus.PENDING_CREATE).copy(clientTaskId = "uuid-1")
        val promoted = mutableSetOf<Long>()
        val toInsert = mutableListOf<TaskEntity>()
        val toUpdate = mutableListOf<TaskEntity>()

        repository().reconcileRemote(incoming, null, emptyMap(), mapOf("uuid-1" to pending), promoted, toInsert, toUpdate)

        assertEquals(1, toUpdate.size)
        assertEquals(5L, toUpdate.first().id) // promoted onto the local pending row's id
        assertEquals(0, toInsert.size)
        assertTrue(promoted.contains(5L))
    }

    // --- pushPendingTasks: one poisoned row must not freeze the batch (surface only retryable) ---

    @Test
    fun `push keeps going past a poisoned row and surfaces the retryable failure`() = runTest {
        val poisoned = taskEntity(id = 1L, syncStatus = SyncStatus.PENDING_CREATE).copy(title = "poison")
        val retryable = taskEntity(id = 2L, syncStatus = SyncStatus.PENDING_CREATE).copy(title = "retry")
        every { localDataSource.observeAll() } returns flowOf(listOf(poisoned, retryable))
        coEvery { localDataSource.getSubtasks(any()) } returns emptyList()
        coEvery { remoteDataSource.addTask(match { it.title == "poison" }) } returns
            Result.failure(DomainException.NotFound())
        coEvery { remoteDataSource.addTask(match { it.title == "retry" }) } returns
            Result.failure(DomainException.Server("boom"))

        val result = repository().syncLocalTasksToServer()

        // both rows attempted (the poisoned NotFound did not abort the batch)
        coVerify(exactly = 1) { remoteDataSource.addTask(match { it.title == "poison" }) }
        coVerify(exactly = 1) { remoteDataSource.addTask(match { it.title == "retry" }) }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainException.Server) // retryable surfaced, poisoned dropped
    }

    // --- recurring + staged: steps reset each occurrence (Phase 4) ---

    @Test
    fun `ticking a step of a recurring task writes the day row and never the step flag`() = runTest {
        val steps = listOf(subtask(1L, done = false), subtask(2L, done = false))
        coEvery { localDataSource.getSubtaskById(1L) } returns steps[0]
        coEvery { localDataSource.getTaskById(10L) } returns taskEntity(id = 10L).copy(recurrence = "DAILY")
        coEvery { localDataSource.getSubtasks(10L) } returns steps
        coEvery { subtaskDailyCompletionDao.getDoneStepIds(10L, DAY) } returns listOf(1L)

        repository().toggleSubtask(1L, isCompleted = true, onDate = LocalDate.ofEpochDay(DAY))

        coVerify(exactly = 1) { subtaskDailyCompletionDao.upsert(any()) }
        // The single flag on the row would stick "done" forever across occurrences.
        coVerify(exactly = 0) { localDataSource.updateSubtask(any()) }
    }

    @Test
    fun `the last step of a day completes that day, earlier days untouched`() = runTest {
        val steps = listOf(subtask(1L, done = false), subtask(2L, done = false))
        coEvery { localDataSource.getSubtaskById(2L) } returns steps[1]
        coEvery { localDataSource.getTaskById(10L) } returns taskEntity(id = 10L).copy(recurrence = "DAILY")
        coEvery { localDataSource.getSubtasks(10L) } returns steps
        // After this tick both steps are done for DAY.
        coEvery { subtaskDailyCompletionDao.getDoneStepIds(10L, DAY) } returns listOf(1L, 2L)

        repository().toggleSubtask(2L, isCompleted = true, onDate = LocalDate.ofEpochDay(DAY))

        coVerify(exactly = 1) { dailyCompletionDao.upsert(match { it.taskId == 10L && it.date == DAY }) }
    }

    @Test
    fun `un-ticking a step re-opens only that day`() = runTest {
        val steps = listOf(subtask(1L, done = false), subtask(2L, done = false))
        coEvery { localDataSource.getSubtaskById(1L) } returns steps[0]
        coEvery { localDataSource.getTaskById(10L) } returns taskEntity(id = 10L).copy(recurrence = "DAILY")
        coEvery { localDataSource.getSubtasks(10L) } returns steps
        coEvery { subtaskDailyCompletionDao.getDoneStepIds(10L, DAY) } returns listOf(2L)

        repository().toggleSubtask(1L, isCompleted = false, onDate = LocalDate.ofEpochDay(DAY))

        coVerify(exactly = 1) { subtaskDailyCompletionDao.delete(1L, DAY) }
        // Not all steps done ⇒ the day is not complete. Only DAY is touched, never another date.
        coVerify(exactly = 0) { dailyCompletionDao.upsert(any()) }
    }

    @Test
    fun `a recurring parent never has its base completion flag written`() = runTest {
        val steps = listOf(subtask(1L, done = true))
        coEvery { localDataSource.getSubtaskById(1L) } returns steps[0]
        coEvery { localDataSource.getTaskById(10L) } returns taskEntity(id = 10L).copy(recurrence = "WEEKLY")
        coEvery { localDataSource.getSubtasks(10L) } returns steps

        // No date: falls through to the classic path, which must still refuse to write the flag —
        // once true it sticks forever and the task can never be un-done.
        repository().toggleSubtask(1L, isCompleted = true, onDate = null)

        // markParentPendingUpdate legitimately rewrites the row to flip syncStatus, so the invariant
        // is specifically that isCompleted is never set — not that the row is never touched.
        coVerify(exactly = 0) { localDataSource.update(match { it.id == 10L && it.isCompleted }) }
    }

    @Test
    fun `the last step is protected on a pure staged task but not on a recurring one`() = runTest {
        val step = subtask(1L, done = false)
        coEvery { localDataSource.getSubtaskById(1L) } returns step
        coEvery { localDataSource.countSubtasks(10L) } returns 1

        coEvery { localDataSource.getTaskById(10L) } returns taskEntity(id = 10L)
        repository().deleteSubtask(1L)
        // A staged task with no steps would silently become a plain task.
        coVerify(exactly = 0) { localDataSource.deleteSubtask(any()) }

        coEvery { localDataSource.getTaskById(10L) } returns taskEntity(id = 10L).copy(recurrence = "DAILY")
        repository().deleteSubtask(1L)
        // A routine still has an identity without steps, so it may drop to zero.
        coVerify(exactly = 1) { localDataSource.deleteSubtask(step) }
    }

    // --- pull-side reminders + alarms: the chatbot's reminders never fired before this ---

    /**
     * Drives one full `syncRemoteTasksWithLocal()`.
     *
     * [localBefore] and [localAfter] are the two `observeAll()` reads the sync makes — one to build the
     * reconcile plan, one after the writes land so freshly-inserted rows have their local ids. Passing
     * two DIFFERENT lists is the only way to reach the insert path: seed the same row in both and
     * `reconcileRemote` finds it content-equal, puts it in neither list, and every assertion about
     * arming passes for reasons that have nothing to do with the code under test.
     */
    private suspend fun runSync(
        remote: TaskData,
        localBefore: List<TaskEntity>,
        localAfter: List<TaskEntity> = localBefore,
        storedReminders: List<Int> = emptyList(),
    ) {
        coEvery { remoteDataSource.getTasks() } returns
            Result.success(TaskListData(tasks = listOf(remote), count = 1))
        every { localDataSource.observeAll() } returnsMany listOf(flowOf(localBefore), flowOf(localAfter))
        // Only the insert path reads this (to pick the new row's orderIndex), and a relaxed mock hands
        // back a Flow that never emits — so without this stub the insert test dies in nextOrder().
        every { localDataSource.observeByDate(any()) } returns flowOf(emptyList())
        coEvery { localDataSource.getSubtasks(any()) } returns emptyList()
        coEvery { localDataSource.getAllReminders() } returns storedReminders.mapIndexed { index, minute ->
            TaskReminderEntity(taskId = LOCAL_ID, minuteOfDay = minute, slot = index)
        }
        repository().syncRemoteTasksWithLocal()
    }

    @Test
    fun `a task arriving from the server for the first time gets its reminders and its alarm`() = runTest {
        // Nothing local beforehand, so the row genuinely goes through toInsert — the branch that armed
        // nothing at all, which is why a task the chatbot created stayed silent until the next reboot.
        runSync(
            remote = remoteTask(id = 70L, recurrence = "DAILY", reminderTimes = listOf(28_800, 50_400)),
            localBefore = emptyList(),
            localAfter = listOf(syncedTask(recurrence = "DAILY")),
        )

        // Wire is second-of-day, Room is minute-of-day: 08:00 and 14:00.
        coVerify(exactly = 1) { localDataSource.replaceReminders(LOCAL_ID, listOf(480, 840)) }
        verify(exactly = 1) { alarmScheduler.scheduleRecurring(LOCAL_ID, any(), any(), any(), any(), any(), any(), any()) }
        // A brand-new local id can have nothing armed under it, so the 8-slot sweep is pure waste —
        // and on a first sign-in every single task takes this path.
        verify(exactly = 0) { alarmScheduler.cancelRecurring(any(), any()) }
    }

    @Test
    fun `a routine that stopped repeating off-device is swept and its stale reminders dropped`() = runTest {
        // The chat's setTaskSchedule(recurrence=NONE) makes this happen on the server. Locally the row
        // is still DAILY, so it lands in toUpdate — the real shape of this scenario.
        runSync(
            remote = remoteTask(id = 70L, recurrence = "NONE"),
            localBefore = listOf(syncedTask(recurrence = "DAILY")),
            localAfter = listOf(syncedTask(recurrence = "NONE")),
            storedReminders = listOf(480),
        )

        // An armed slot re-arms itself from its own intent extras forever, so failing to sweep leaves
        // the user with a daily reminder for a task that no longer repeats and no way to stop it.
        verify(exactly = 1) { alarmScheduler.cancelRecurring(LOCAL_ID, any()) }
        // Reminder times are meaningless without a recurrence; left behind they keep rendering in the
        // edit screen and re-badge the task as CUSTOM.
        coVerify(exactly = 1) { localDataSource.replaceReminders(LOCAL_ID, emptyList()) }
    }

    @Test
    fun `a one-off that became a routine has its old one-shot cancelled`() = runTest {
        // Request-code spaces are disjoint, so cancelRecurring does not cover the one-shot: without an
        // explicit cancel the original single alarm still fires alongside the new daily one.
        runSync(
            remote = remoteTask(id = 70L, recurrence = "DAILY"),
            localBefore = listOf(syncedTask(recurrence = "NONE")),
            localAfter = listOf(syncedTask(recurrence = "DAILY")),
        )

        // Overload-qualified: cancelTask now also has an id-only form used by the delete/logout paths.
        verify(exactly = 1) { alarmScheduler.cancelTask(any<AlarmItem>()) }
    }

    @Test
    fun `an empty server reminder set never deletes a recurring task's rows`() = runTest {
        // TaskData.reminderTimes defaults to an empty list, so "the user has none" and "these times
        // never reached the server" are the same payload — and every row created before the push
        // started sending them is permanently in the second state.
        runSync(
            remote = remoteTask(id = 70L, recurrence = "DAILY", reminderTimes = emptyList()),
            localBefore = listOf(syncedTask(recurrence = "DAILY")),
            storedReminders = listOf(480, 840),
        )

        coVerify(exactly = 0) { localDataSource.replaceReminders(any(), any()) }
    }

    @Test
    fun `an unchanged task is left completely alone`() = runTest {
        runSync(
            remote = remoteTask(id = 70L, recurrence = "DAILY", reminderTimes = listOf(28_800)),
            localBefore = listOf(syncedTask(recurrence = "DAILY")),
            storedReminders = listOf(480),
        )

        // replaceReminders re-derives every slot from the list index and `slot` seeds the alarm request
        // code, so re-writing an identical set would rebuild eight PendingIntents on every sync.
        coVerify(exactly = 0) { localDataSource.replaceReminders(any(), any()) }
        verify(exactly = 0) { alarmScheduler.cancelRecurring(any(), any()) }
        verify(exactly = 0) { alarmScheduler.scheduleRecurring(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `a row with an unpushed local edit is never reconciled from the server`() = runTest {
        runSync(
            remote = remoteTask(id = 70L, reminderTimes = listOf(28_800)),
            localBefore = listOf(syncedTask(recurrence = "NONE").copy(syncStatus = SyncStatus.PENDING_UPDATE)),
        )

        // The local push wins; the next pull reconciles once it has landed.
        coVerify(exactly = 0) { localDataSource.replaceReminders(any(), any()) }
    }

    @Test
    fun `pushing a task carries its reminder rows to the server`() = runTest {
        // TaskEntity has no reminder columns, so toDomain() cannot carry them and the push has to
        // re-attach them the same way it re-attaches steps. Without this they never left the device —
        // which is what made the pull path's empty-server-set branch destructive.
        val pending = taskEntity(id = LOCAL_ID, syncStatus = SyncStatus.PENDING_CREATE)
        every { localDataSource.observeAll() } returns flowOf(listOf(pending))
        coEvery { localDataSource.getSubtasks(any()) } returns emptyList()
        coEvery { localDataSource.getReminders(LOCAL_ID) } returns listOf(
            TaskReminderEntity(taskId = LOCAL_ID, minuteOfDay = 480, slot = 0),
            TaskReminderEntity(taskId = LOCAL_ID, minuteOfDay = 840, slot = 1),
        )
        coEvery { remoteDataSource.addTask(any()) } returns Result.failure(DomainException.Server("stop here"))

        repository().syncLocalTasksToServer()

        coVerify {
            remoteDataSource.addTask(
                match { it.reminderTimes == listOf(LocalTime.of(8, 0), LocalTime.of(14, 0)) },
            )
        }
    }

    // --- contentEquals: a field it doesn't compare is a field the server can never deliver ---

    @Test
    fun `an extended repeat-rule change alone still reaches the device`() {
        val local = taskEntity(id = 1L, remoteId = 50L, syncStatus = SyncStatus.SYNCED)
            .copy(recurrence = "WEEKLY", recurrenceInterval = 1)
        val incoming = local.copy(recurrenceInterval = 2)
        val toUpdate = mutableListOf<TaskEntity>()

        repository().reconcileRemote(incoming, local, emptyMap(), emptyMap(), mutableSetOf(), mutableListOf(), toUpdate)

        // "Make it every other week" changes nothing else on the row — without interval in the
        // comparison the pull decided nothing had changed and the edit died on the server.
        assertEquals(1, toUpdate.size)
        assertEquals(2, toUpdate.first().recurrenceInterval)
    }

    @Test
    fun `a location-only change still reaches the device`() {
        val local = taskEntity(id = 1L, remoteId = 50L, syncStatus = SyncStatus.SYNCED)
        val incoming = local.copy(locationName = "Kadıköy", locationLat = 40.99, locationLng = 29.02)
        val toUpdate = mutableListOf<TaskEntity>()

        repository().reconcileRemote(incoming, local, emptyMap(), emptyMap(), mutableSetOf(), mutableListOf(), toUpdate)

        // This is what setTaskLocation has been doing all along: server row correct, device unchanged.
        assertEquals(1, toUpdate.size)
        assertEquals("Kadıköy", toUpdate.first().locationName)
    }

    private companion object {
        const val DAY = 20_000L

        /** Local row id / server id of the single task the sync tests reconcile. */
        const val LOCAL_ID = 7L
        const val REMOTE_ID = 70L

        /** The synced local counterpart of [remoteTask], differing only where a test says so. */
        fun syncedTask(recurrence: String) = taskEntity(id = LOCAL_ID, remoteId = REMOTE_ID, syncStatus = SyncStatus.SYNCED)
            .copy(recurrence = recurrence)

        fun remoteTask(
            id: Long,
            recurrence: String = "NONE",
            reminderTimes: List<Int> = emptyList(),
        ) = TaskData(
            id = id,
            title = "task",
            description = null,
            date = 20_000L,
            timeStart = 32_400L,
            timeEnd = 36_000L,
            isCompleted = false,
            isSecret = false,
            recurrence = recurrence,
            reminderTimes = reminderTimes,
        )

        fun taskEntity(
            id: Long,
            remoteId: Long? = null,
            isCompleted: Boolean = false,
            syncStatus: SyncStatus = SyncStatus.SYNCED,
        ) = TaskEntity(
            title = "task",
            description = null,
            date = 20_000L,
            // Personal-task TaskEntity stores time as minute-of-day (not seconds). See TaskMapper.
            timeStart = 540L,
            timeEnd = 600L,
            isCompleted = isCompleted,
            remoteId = remoteId,
            syncStatus = syncStatus,
            id = id,
        )

        fun subtask(
            id: Long,
            done: Boolean,
        ) = SubtaskEntity(
            title = "step",
            parentTaskId = 10L,
            isCompleted = done,
            id = id,
        )

        // delete(task) only reads task.id; the rest are filler valid values.
        fun domainTask(id: Long) = Task(
            id = id,
            title = "task",
            description = null,
            date = LocalDate.of(2026, 7, 4),
            timeStart = LocalTime.of(9, 0),
            timeEnd = LocalTime.of(10, 0),
            isCompleted = false,
            isSecret = false,
        )
    }
}
