package com.todoapp.mobile.data.repository

import com.todoapp.mobile.common.DomainException
import com.todoapp.mobile.data.model.entity.SyncStatus
import com.todoapp.mobile.data.model.entity.TaskEntity
import com.todoapp.mobile.data.source.local.datasource.TaskLocalDataSource
import com.todoapp.mobile.data.source.remote.datasource.TaskRemoteDataSource
import com.todoapp.mobile.domain.model.Task
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Characterization tests pinning the §7.2 sync invariants that were bug-fixed on 2026-07-03/04.
 * All collaborators are mocked, so TaskRepositoryImpl instantiates on the plain JVM (the two methods
 * under test touch no android.util.Log, hence no Robolectric needed).
 */
class TaskRepositoryImplTest {
    private val localDataSource = mockk<TaskLocalDataSource>(relaxed = true)
    private val remoteDataSource = mockk<TaskRemoteDataSource>(relaxed = true)

    private fun repository() = TaskRepositoryImpl(
        remoteDataSource = remoteDataSource,
        localDataSource = localDataSource,
        groupTaskLocalDataSource = mockk(relaxed = true),
        todoApi = mockk(relaxed = true),
        pendingPhotoRepository = mockk(relaxed = true),
        dailyCompletionDao = mockk(relaxed = true),
        alarmScheduler = mockk(relaxed = true),
        dailyPlanPreferences = mockk(relaxed = true),
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

    private companion object {
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
