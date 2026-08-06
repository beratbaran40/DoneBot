package com.todoapp.mobile.data.repository

import android.util.Log
import com.todoapp.mobile.data.model.entity.SyncStatus
import com.todoapp.mobile.data.model.entity.TaskEntity
import com.todoapp.mobile.domain.alarm.AlarmScheduler
import com.todoapp.mobile.domain.model.AlarmItem
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.Task
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * The alarm half of the task lifecycle. Every case here is a bug that shipped, and every one of them
 * is invisible without a test: nothing throws, nothing logs, a reminder simply does not arrive — or
 * arrives for a task that no longer exists.
 */
class TaskAlarmLifecycleTest {
    private val localDataSource =
        mockk<com.todoapp.mobile.data.source.local.datasource.TaskLocalDataSource>(relaxed = true)
    private val remoteDataSource =
        mockk<com.todoapp.mobile.data.source.remote.datasource.TaskRemoteDataSource>(relaxed = true)
    private val alarmScheduler = mockk<AlarmScheduler>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any<Throwable>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        // insert() awaits the create; a failure just leaves the row PENDING_CREATE, which is fine here.
        coEvery { remoteDataSource.addTask(any()) } returns Result.failure(IllegalStateException("offline"))
        // Both insert paths derive the new row's order index from the day's existing rows, and a
        // relaxed mock hands back an empty Flow whose first() throws.
        every { localDataSource.observeByDate(any()) } returns flowOf(emptyList())
        every { localDataSource.observeAll() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() = unmockkStatic(Log::class)

    private fun repository() = TaskRepositoryImpl(
        remoteDataSource = remoteDataSource,
        localDataSource = localDataSource,
        groupTaskLocalDataSource = mockk(relaxed = true),
        todoApi = mockk(relaxed = true),
        pendingPhotoRepository = mockk(relaxed = true),
        dailyCompletionDao = mockk(relaxed = true),
        subtaskDailyCompletionDao = mockk(relaxed = true),
        alarmScheduler = alarmScheduler,
        dailyPlanPreferences = mockk(relaxed = true),
        analyticsHelper = mockk(relaxed = true),
        ioDispatcher = Dispatchers.Unconfined,
    )

    // --- create: the alarm must be keyed on the id Room minted, never on the caller's Task ---

    @Test
    fun `creating a task arms its reminder under the id room assigned, not the incoming zero`() = runTest {
        coEvery { localDataSource.insert(any()) } returns NEW_ID
        val item = slot<AlarmItem>()

        repository().insert(futureTask(id = 0L))

        verify { alarmScheduler.schedule(capture(item), any()) }
        // The whole bug in one assertion: an id of 0 is what every ViewModel used to pass, and it put
        // every task on the same request code.
        assertEquals(NEW_ID, item.captured.taskId)
    }

    @Test
    fun `two tasks created in a row get different alarm identities`() = runTest {
        val items = mutableListOf<AlarmItem>()
        val repo = repository()

        coEvery { localDataSource.insert(any()) } returns 11L
        repo.insert(futureTask(id = 0L))
        coEvery { localDataSource.insert(any()) } returns 12L
        repo.insert(futureTask(id = 0L))

        verify(exactly = 2) { alarmScheduler.schedule(capture(items), any()) }
        assertNotEquals(
            "both tasks landed on the same alarm, so the second silently cancelled the first",
            items[0].taskId,
            items[1].taskId,
        )
    }

    @Test
    fun `creating a task with photos arms its reminder too`() = runTest {
        // insertWithPhotos was missing both the reminder rows and the alarm that insert() had, so a
        // task created with a photo attached was simply never going to remind anyone.
        coEvery { localDataSource.insert(any()) } returns NEW_ID

        repository().insertWithPhotos(futureTask(id = 0L), photos = emptyList())

        verify(exactly = 1) { alarmScheduler.schedule(any(), any()) }
        coVerifyReplaceRemindersRan()
    }

    // --- delete / logout: an armed alarm needs no database row to fire ---

    @Test
    fun `deleting a one-off cancels its alarm`() = runTest {
        val entity = entity(id = 3L, remoteId = null)
        coEvery { localDataSource.getTaskById(3L) } returns entity

        repository().delete(domain(id = 3L))

        // Both spaces: the old code called only cancelRecurring, which returns early for a one-off.
        verify(exactly = 1) { alarmScheduler.cancelTask(3L) }
        verify(exactly = 1) { alarmScheduler.cancelRecurring(3L, any()) }
    }

    @Test
    fun `wiping all tasks cancels every alarm before the rows disappear`() = runTest {
        // Logout and account deletion. A recurring alarm re-arms itself from its own intent extras,
        // so anything left behind here outlives the account that created it.
        coEvery { localDataSource.observeAll() } returns flowOf(
            listOf(entity(id = 1L), entity(id = 2L), entity(id = 3L)),
        )

        repository().deleteAllTasks()

        listOf(1L, 2L, 3L).forEach { id ->
            verify(exactly = 1) { alarmScheduler.cancelTask(id) }
            verify(exactly = 1) { alarmScheduler.cancelRecurring(id, any()) }
        }
    }

    // --- completion: ticking a task off should stop it reminding, and un-ticking should restore it ---

    @Test
    fun `completing a one-off cancels its reminder and arms nothing`() = runTest {
        val entity = entity(id = 5L, isCompleted = false)
        coEvery { localDataSource.getTaskById(5L) } returns entity
        coEvery { localDataSource.getSubtasks(5L) } returns emptyList()

        repository().updateTaskCompletion(id = 5L, isCompleted = true)

        verify(exactly = 1) { alarmScheduler.cancelTask(5L) }
        verify(exactly = 0) { alarmScheduler.schedule(any(), any()) }
    }

    @Test
    fun `un-completing a future task arms its reminder again`() = runTest {
        val entity = entity(id = 5L, isCompleted = true, date = FUTURE_EPOCH_DAY)
        coEvery { localDataSource.getTaskById(5L) } returns entity
        coEvery { localDataSource.getSubtasks(5L) } returns emptyList()

        repository().updateTaskCompletion(id = 5L, isCompleted = false)

        verify(exactly = 1) { alarmScheduler.schedule(any(), any()) }
    }

    private fun coVerifyReplaceRemindersRan() {
        coVerify(exactly = 1) { localDataSource.replaceReminders(NEW_ID, any()) }
    }

    private fun futureTask(id: Long) = Task(
        id = id,
        title = "task",
        description = null,
        date = LocalDate.now().plusDays(2),
        timeStart = LocalTime.of(9, 0),
        timeEnd = LocalTime.of(10, 0),
        isCompleted = false,
        isSecret = false,
        recurrence = Recurrence.NONE,
        reminderOffsetMinutes = 30L,
    )

    private fun domain(id: Long) = futureTask(id)

    private fun entity(
        id: Long,
        remoteId: Long? = null,
        isCompleted: Boolean = false,
        date: Long = FUTURE_EPOCH_DAY,
    ) = TaskEntity(
        title = "task",
        description = null,
        date = date,
        timeStart = 540L,
        timeEnd = 600L,
        isCompleted = isCompleted,
        remoteId = remoteId,
        syncStatus = SyncStatus.SYNCED,
        id = id,
    )

    private companion object {
        const val NEW_ID = 42L

        /** Far enough ahead that the scheduler's past-trigger guard never suppresses the alarm. */
        val FUTURE_EPOCH_DAY: Long = LocalDate.now().plusDays(2).toEpochDay()
    }
}
