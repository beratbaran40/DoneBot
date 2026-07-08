package com.todoapp.mobile.data.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.todoapp.mobile.common.DomainException
import com.todoapp.mobile.domain.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Locks FetchTasksWorker's retry classification through the ServerUnreachable taxonomy change:
 * every transient shape a cold-starting/deploying backend produces (NoInternet, Server,
 * ServerUnreachable) must retry, while terminal shapes fail so WorkManager doesn't loop forever.
 * Before the taxonomy change, ConnectException/callTimeout mapped to Unknown and made the worker
 * give up permanently during exactly the windows it should have ridden out.
 */
// Vanilla Application (not the app's real one) — see SyncWorkerTest: avoids Firebase/Hilt boot.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class FetchTasksWorkerTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    private fun workerFor(repository: TaskRepository): FetchTasksWorker = TestListenableWorkerBuilder<FetchTasksWorker>(context)
        .setWorkerFactory(
            object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ): ListenableWorker = FetchTasksWorker(appContext, workerParameters, repository)
            },
        ).build()

    private fun repositoryFailingWith(error: Throwable): TaskRepository = mockk<TaskRepository> {
        coEvery { syncRemoteTasksWithLocal() } returns Result.failure(error)
    }

    @Test
    fun `succeeds when the fetch succeeds`() = runBlocking {
        val repository = mockk<TaskRepository>()
        coEvery { repository.syncRemoteTasksWithLocal() } returns Result.success(Unit)
        assertEquals(ListenableWorker.Result.success(), workerFor(repository).doWork())
    }

    @Test
    fun `retries transient failures including a cold-start ServerUnreachable`() = runBlocking {
        listOf(
            DomainException.NoInternet(),
            DomainException.Server("boom"),
            DomainException.ServerUnreachable("timeout", requestNeverReachedServer = false),
            DomainException.ServerUnreachable("edge 502", requestNeverReachedServer = true),
        ).forEach { error ->
            assertEquals(
                "expected retry for ${error.javaClass.simpleName}",
                ListenableWorker.Result.retry(),
                workerFor(repositoryFailingWith(error)).doWork(),
            )
        }
    }

    @Test
    fun `fails on terminal errors instead of retrying forever`() = runBlocking {
        listOf(
            DomainException.Unknown(RuntimeException("bug")),
            DomainException.NotFound(),
        ).forEach { error ->
            assertEquals(
                "expected failure for ${error.javaClass.simpleName}",
                ListenableWorker.Result.failure(),
                workerFor(repositoryFailingWith(error)).doWork(),
            )
        }
    }
}
