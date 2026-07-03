package com.todoapp.mobile.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.todoapp.mobile.data.worker.FetchTasksWorker
import com.todoapp.mobile.data.worker.SyncWorker
import com.todoapp.mobile.domain.repository.TaskSyncRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

class TaskSyncRepositoryImpl
@Inject
constructor(
    @ApplicationContext context: Context,
) : TaskSyncRepository {
    private val workManager = WorkManager.getInstance(context)

    @Volatile private var lastFetchAt: Long = 0L

    override fun syncPendingTasks() {
        val constraints =
            Constraints
                .Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        val sync =
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

        // KEEP (not REPLACE): REPLACE cancels an already-running SyncWorker mid-flight, which can leave a
        // POST committed on the server but the local row still PENDING_CREATE (re-POST), or re-issue a
        // DELETE that then 404s. KEEP lets the in-flight push finish and folds this trigger onto it.
        workManager
            .beginUniqueWork(
                SYNC_WORK,
                ExistingWorkPolicy.KEEP,
                sync,
            ).enqueue()
    }

    override fun resetCooldown() {
        lastFetchAt = 0L
    }

    override fun fetchTasks(force: Boolean) {
        val withinCooldown = System.currentTimeMillis() - lastFetchAt < FETCH_COOLDOWN_MS
        Timber.tag("TaskFetch").d("fetchTasks(force=$force) withinCooldown=$withinCooldown")
        if (!force && withinCooldown) return
        lastFetchAt = System.currentTimeMillis()

        val constraints =
            Constraints
                .Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        val sync =
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

        val fetch =
            OneTimeWorkRequestBuilder<FetchTasksWorker>()
                .setConstraints(constraints)
                .build()

        // KEEP (not REPLACE): never cancel an in-flight sync chain. The repository-level mutex serializes
        // the actual push/pull work; KEEP just avoids tearing down a running chain when a new fetch arrives.
        // A forced refresh that lands while a chain is running is intentionally dropped onto the running one.
        workManager
            .beginUniqueWork(
                FETCH_WORK,
                ExistingWorkPolicy.KEEP,
                sync,
            ).then(fetch)
            .enqueue()
    }

    companion object {
        const val SYNC_WORK = "sync_work"
        const val FETCH_WORK = "fetch_work"
        private const val FETCH_COOLDOWN_MS = 60_000L
    }
}
