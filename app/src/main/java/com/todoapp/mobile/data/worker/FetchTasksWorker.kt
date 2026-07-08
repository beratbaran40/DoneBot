package com.todoapp.mobile.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.todoapp.mobile.common.DomainException
import com.todoapp.mobile.domain.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class FetchTasksWorker
@AssistedInject
constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository,
) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result = taskRepository
        .syncRemoteTasksWithLocal()
        .fold(
            onSuccess = {
                Log.d("FetchTasksWorker", "Success")
                Result.success()
            },
            onFailure = { throwable ->
                Log.e("FetchTasksWorker", "Failed: ${throwable::class.simpleName}: ${throwable.message}")
                when (throwable) {
                    is DomainException.NoInternet,
                    is DomainException.Server,
                    is DomainException.ServerUnreachable,
                    -> Result.retry()
                    else -> Result.failure()
                }
            },
        )
}
