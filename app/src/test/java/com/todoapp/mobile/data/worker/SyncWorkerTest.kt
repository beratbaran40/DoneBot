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

    /**
     * Pomodoro defaults to a no-op success so the existing cases keep testing exactly what they did.
     * The cases that care pass their own stub.
     */
    private fun workerFor(
        repository: TaskRepository,
        attempt: Int,
        pomodoro: com.todoapp.mobile.domain.repository.PomodoroSessionRepository = mockk(relaxed = true) {
            coEvery { pushPending() } returns Result.success(Unit)
        },
    ): SyncWorker = TestListenableWorkerBuilder<SyncWorker>(context)
        .setRunAttemptCount(attempt)
        .setWorkerFactory(
            object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ): ListenableWorker = SyncWorker(appContext, workerParameters, repository, pomodoro)
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
    fun `retries a cold-start ServerUnreachable within max attempts`() = runBlocking {
        val repository = mockk<TaskRepository>()
        coEvery { repository.syncLocalTasksToServer() } returns
            Result.failure(DomainException.ServerUnreachable("timeout", requestNeverReachedServer = false))
        assertEquals(ListenableWorker.Result.retry(), workerFor(repository, attempt = 1).doWork())
    }

    @Test
    fun `fails after exhausting max attempts`() = runBlocking {
        val repository = mockk<TaskRepository>()
        coEvery { repository.syncLocalTasksToServer() } returns Result.failure(DomainException.Server("boom"))
        // MAX_ATTEMPT = 2, so attempt 3 exhausts the retries.
        assertEquals(ListenableWorker.Result.failure(), workerFor(repository, attempt = 3).doWork())
    }

    @Test
    fun `a retryable pomodoro failure retries even though the task sync succeeded`() = runBlocking {
        val repository = mockk<TaskRepository>()
        coEvery { repository.syncLocalTasksToServer() } returns Result.success(Unit)
        val pomodoro = mockk<com.todoapp.mobile.domain.repository.PomodoroSessionRepository>(relaxed = true)
        coEvery { pomodoro.pushPending() } returns Result.failure(DomainException.NoInternet())

        // Otherwise the rows sit pending until something else happens to enqueue a sync.
        assertEquals(
            ListenableWorker.Result.retry(),
            workerFor(repository, attempt = 1, pomodoro = pomodoro).doWork(),
        )
    }

    @Test
    fun `a thrown pomodoro push cannot fail a task sync that already succeeded`() = runBlocking {
        val repository = mockk<TaskRepository>()
        coEvery { repository.syncLocalTasksToServer() } returns Result.success(Unit)
        val pomodoro = mockk<com.todoapp.mobile.domain.repository.PomodoroSessionRepository>(relaxed = true)
        coEvery { pomodoro.pushPending() } throws IllegalStateException("boom")

        // Pomodoro is the newcomer here; an unexpected throw in it must not regress task syncing.
        assertEquals(
            ListenableWorker.Result.success(),
            workerFor(repository, attempt = 1, pomodoro = pomodoro).doWork(),
        )
    }
}
