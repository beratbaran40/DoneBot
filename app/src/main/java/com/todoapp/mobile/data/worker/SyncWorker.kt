package com.todoapp.mobile.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.todoapp.mobile.common.DomainException
import com.todoapp.mobile.data.perf.firebaseTrace
import com.todoapp.mobile.domain.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker
@AssistedInject
constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val pomodoroSessionRepository: com.todoapp.mobile.domain.repository.PomodoroSessionRepository,
) : CoroutineWorker(ctx, params) {
    // §3.10 sync_pending_tasks trace spans the whole push (enqueue → many POST/PUT/DELETE → reconcile),
    // which a single automatic HTTP trace can't capture. No-op when perf collection is off.
    override suspend fun doWork(): Result = firebaseTrace("sync_pending_tasks") {
        // Pomodoro rides this worker rather than getting its own. A second worker would mean either a
        // third independent chain — two wake-ups and two network windows against a database that scales
        // to zero — or a new node on fetch_work, where a pomodoro failure would block FetchTasksWorker.
        // It also keeps one definition of "retryable" instead of two that can drift apart.
        //
        // Tasks run FIRST and their result still decides the outcome, so if pomodoro misbehaves the
        // existing behaviour is unchanged. runCatching isolates it: an unexpected throw here must never
        // roll back a task sync that already succeeded.
        val taskResult = taskRepository.syncLocalTasksToServer()

        // kotlin.Result spelled out: inside a CoroutineWorker, bare `Result` is
        // androidx.work.ListenableWorker.Result, and the two are silently confusable here.
        val pomodoroResult: kotlin.Result<Unit> =
            runCatching { pomodoroSessionRepository.pushPending() }
                .getOrElse { throwable ->
                    Log.e("SyncWorker", "pomodoro push threw; task sync unaffected", throwable)
                    kotlin.Result.success(Unit)
                }

        // A task failure still wins, so the retry semantics this worker already had are untouched; a
        // pomodoro failure only decides the outcome when tasks succeeded.
        (if (taskResult.isFailure) taskResult else pomodoroResult)
            .fold(
                onSuccess = { Result.success() },
                onFailure = { throwable ->
                    Log.e("SyncWorker", "Failed to sync tasks (attempt=$runAttemptCount) $throwable", throwable)
                    when (throwable) {
                        is DomainException.NoInternet,
                        is DomainException.Server,
                        is DomainException.ServerUnreachable,
                        is DomainException.Unauthorized,
                        -> {
                            if (runAttemptCount <= MAX_ATTEMPT) {
                                Result.retry()
                            } else {
                                Result.failure()
                            }
                        }
                        else -> Result.failure()
                    }
                },
            )
    }
}

const val MAX_ATTEMPT = 2
