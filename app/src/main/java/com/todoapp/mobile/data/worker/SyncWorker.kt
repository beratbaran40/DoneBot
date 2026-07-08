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
) : CoroutineWorker(ctx, params) {
    // §3.10 sync_pending_tasks trace spans the whole push (enqueue → many POST/PUT/DELETE → reconcile),
    // which a single automatic HTTP trace can't capture. No-op when perf collection is off.
    override suspend fun doWork(): Result = firebaseTrace("sync_pending_tasks") {
        taskRepository
            .syncLocalTasksToServer()
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
