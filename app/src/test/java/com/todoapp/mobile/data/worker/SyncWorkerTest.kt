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
 * Locks SyncWorker's retry policy: transient failures (NoInternet/Server/Unauthorized) retry while
 * runAttemptCount <= MAX_ATTEMPT and give up afterwards, so a bad sync neither drops silently nor
 * retries forever. Runs on Robolectric (CoroutineWorker + TestListenableWorkerBuilder need a Context)
 * with JVM MockK — avoids on-device mockk-android.
 */
// Use a vanilla Application, not the app's real one — the latter's onCreate boots Firebase/Hilt,
// which isn't configured under Robolectric. The worker only needs a Context; its repo is injected below.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class SyncWorkerTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    private fun workerFor(repository: TaskRepository, attempt: Int): SyncWorker = TestListenableWorkerBuilder<SyncWorker>(context)
        .setRunAttemptCount(attempt)
        .setWorkerFactory(
            object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ): ListenableWorker = SyncWorker(appContext, workerParameters, repository)
            },
        ).build()

    @Test
    fun `succeeds when the sync succeeds`() = runBlocking {
        val repository = mockk<TaskRepository>()
        coEvery { repository.syncLocalTasksToServer() } returns Result.success(Unit)
        assertEquals(ListenableWorker.Result.success(), workerFor(repository, attempt = 1).doWork())
    }

    @Test
    fun `retries a server error within max attempts`() = runBlocking {
        val repository = mockk<TaskRepository>()
        coEvery { repository.syncLocalTasksToServer() } returns Result.failure(DomainException.Server("boom"))
        assertEquals(ListenableWorker.Result.retry(), workerFor(repository, attempt = 1).doWork())
    }

    @Test
    fun `fails after exhausting max attempts`() = runBlocking {
        val repository = mockk<TaskRepository>()
        coEvery { repository.syncLocalTasksToServer() } returns Result.failure(DomainException.Server("boom"))
        // MAX_ATTEMPT = 2, so attempt 3 exhausts the retries.
        assertEquals(ListenableWorker.Result.failure(), workerFor(repository, attempt = 3).doWork())
    }
}
