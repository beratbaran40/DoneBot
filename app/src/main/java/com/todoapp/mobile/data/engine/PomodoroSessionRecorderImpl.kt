package com.todoapp.mobile.data.engine

import com.todoapp.mobile.data.model.entity.PomodoroSessionEntity
import com.todoapp.mobile.data.repository.DataStoreHelper
import com.todoapp.mobile.data.source.local.PomodoroSessionDao
import com.todoapp.mobile.di.IoDispatcher
import com.todoapp.mobile.domain.engine.PomodoroMode
import com.todoapp.mobile.domain.engine.PomodoroSessionRecord
import com.todoapp.mobile.domain.engine.PomodoroSessionRecorder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Writes finished and abandoned intervals to Room, off the engine's thread.
 *
 * **Owns its own scope** rather than sharing the engine's. `PomodoroEngine.shutdown()` cancels that one,
 * and a record handed over during shutdown would be cancelled mid-write; [shutdown] here is called
 * immediately after the engine's from `Application`, which satisfies the house rule that no singleton
 * scope exists without a shutdown path.
 *
 * Never reads engine state. The engine builds an immutable [PomodoroSessionRecord] from its own fields
 * before mutating them; by the time this runs, the engine has already moved to the next session.
 */
@Singleton
class PomodoroSessionRecorderImpl
@Inject
constructor(
    private val dao: PomodoroSessionDao,
    // The shared "flush pending writes" chain. Its name is now a misnomer — it enqueues SyncWorker,
    // which pushes tasks AND pomodoro — but renaming it is a separate change.
    private val taskSyncRepository: com.todoapp.mobile.domain.repository.TaskSyncRepository,
    private val dataStoreHelper: DataStoreHelper,
    private val clock: Clock,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PomodoroSessionRecorder {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    override fun record(record: PomodoroSessionRecord) {
        scope.launch {
            // Swallowing is deliberate: this is called from the engine's control path, where a throw
            // would abort a session transition. A lost row is a missing statistic; a thrown one is a
            // stuck timer.
            runCatching { dao.insert(record.toEntity()) }
                .onFailure { Timber.tag(TAG).w(it, "failed to record pomodoro session") }
        }
    }

    /**
     * One enqueue per run, never per session.
     *
     * Without it a run finishing at 22:00 would sit unsent until the next Home or Calendar visit —
     * SyncWorker only runs today after a token refresh or a fetchTasks() from one of those screens.
     * Unique work with KEEP, so this folds onto an in-flight sync rather than restarting it, and
     * WorkManager simply defers it while offline.
     */
    override fun onRunEnded() {
        runCatching { taskSyncRepository.syncPendingTasks() }
            .onFailure { Timber.tag(TAG).w(it, "failed to enqueue sync after run") }
    }

    override fun shutdown() {
        scope.cancel()
    }

    private suspend fun PomodoroSessionRecord.toEntity(): PomodoroSessionEntity {
        // Both derived from endedAt rather than "now": a row written moments after midnight must still
        // belong to the day the session actually ended on.
        val endedAt = Instant.ofEpochMilli(endedAtMillis)
        val zone = clock.zone
        val offsetMinutes = zone.rules.getOffset(endedAt).totalSeconds / SECONDS_PER_MINUTE
        return PomodoroSessionEntity(
            clientSessionId = clientSessionId,
            clientRunId = clientRunId,
            sessionIndex = sessionIndex,
            mode = mode.toStorageValue(),
            plannedSeconds = plannedSeconds.toInt(),
            elapsedSeconds = elapsedSeconds.toInt(),
            completed = completed,
            startedAt = startedAtMillis,
            endedAt = endedAtMillis,
            localDate = endedAt.atZone(zone).toLocalDate().toEpochDay(),
            tzOffsetMinutes = offsetMinutes,
            ownerUserId = dataStoreHelper.observeUser().first()?.id ?: GUEST_OWNER_ID,
        )
    }

    /**
     * Exhaustive on purpose. `OverTime` cannot reach here — overtime is a state the engine enters, never
     * a queued [com.todoapp.mobile.domain.engine.Session] — but the server already accepts the value, so
     * mapping it now means a future release that does record overtime needs no schema or backend change.
     */
    private fun PomodoroMode.toStorageValue(): String = when (this) {
        PomodoroMode.Focus -> "FOCUS"
        PomodoroMode.ShortBreak -> "SHORT_BREAK"
        PomodoroMode.LongBreak -> "LONG_BREAK"
        PomodoroMode.OverTime -> "OVERTIME"
    }

    private companion object {
        const val TAG = "PomodoroRecorder"
        const val SECONDS_PER_MINUTE = 60
        const val GUEST_OWNER_ID = 0L
    }
}
