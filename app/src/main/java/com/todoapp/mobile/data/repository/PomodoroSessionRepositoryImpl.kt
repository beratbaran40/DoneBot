package com.todoapp.mobile.data.repository

import com.todoapp.mobile.common.DomainException
import com.todoapp.mobile.data.model.entity.PomodoroSessionEntity
import com.todoapp.mobile.data.model.entity.SyncStatus
import com.todoapp.mobile.data.model.network.request.PomodoroSessionDto
import com.todoapp.mobile.data.source.local.PomodoroSessionDao
import com.todoapp.mobile.data.source.remote.datasource.PomodoroRemoteDataSource
import com.todoapp.mobile.domain.model.PomodoroDayStat
import com.todoapp.mobile.domain.model.PomodoroRunSummary
import com.todoapp.mobile.domain.repository.PomodoroSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PomodoroSessionRepositoryImpl
@Inject
constructor(
    private val dao: PomodoroSessionDao,
    private val remote: PomodoroRemoteDataSource,
    private val dataStoreHelper: DataStoreHelper,
    private val clock: Clock,
) : PomodoroSessionRepository {

    /** Own mutex, not the task sync's — see [pushPending]. */
    private val pushMutex = Mutex()

    @Volatile
    private var lastBackfillAt: Long = 0L

    override fun observeFocusByDay(start: LocalDate, end: LocalDate): Flow<List<PomodoroDayStat>> = dao.observeFocusByDay(start.toEpochDay(), end.toEpochDay())

    override fun observeRun(clientRunId: String): Flow<PomodoroRunSummary> = dao.observeRun(clientRunId).map { rows ->
        val focus = rows.filter { it.mode == FOCUS }
        PomodoroRunSummary(
            // Sessions actually completed, not sessions the run planned. The three navigation
            // arguments this replaces counted the queue, so a run cut short still reported its full
            // intended length.
            focusSessions = focus.count { it.completed },
            totalFocusMinutes = focus.sumOf { it.elapsedSeconds } / SECONDS_PER_MINUTE,
            totalBreakMinutes = rows.filterNot { it.mode == FOCUS }
                .sumOf { it.elapsedSeconds } / SECONDS_PER_MINUTE,
        )
    }

    /**
     * Uploads pending rows in batches of fifty, under its **own** mutex.
     *
     * Not `TaskRepositoryImpl.syncMutex`: different table, no ordering dependency between the two, and
     * sharing would queue every pomodoro push behind a full task sync.
     */
    override suspend fun pushPending(): Result<Unit> = pushMutex.withLock {
        val pending = dao.getPending(PUSH_LIMIT)
        if (pending.isEmpty()) return@withLock Result.success(Unit)

        val retryable = mutableListOf<Throwable>()
        pending.chunked(CHUNK_SIZE).forEach { chunk ->
            remote.upload(chunk.map { it.toDto() })
                .onSuccess { dao.markSynced(chunk.map { row -> row.clientSessionId }) }
                .onFailure { error -> handleChunkFailure(error, chunk, retryable) }
        }
        if (retryable.isEmpty()) Result.success(Unit) else Result.failure(retryable.first())
    }

    /**
     * Decides what a failed chunk means for the rows in it.
     *
     * `NotFound` is treated as retryable here, which is a **deliberate departure** from
     * `TaskRepositoryImpl.isRetryable` — there a 404 means the row is permanently gone, so re-pushing is
     * pointless. Here it means "this endpoint does not exist on the backend this client is talking to",
     * the staged-rollout case of a new app against an older server. Dropping the rows would lose them
     * forever; leaving them pending costs one wasted call per sync until the backend catches up.
     *
     * A genuine 400 is the only case where rows are abandoned, and they are marked synced so a poisoned
     * batch cannot block every later one. Fifty rows bounds the loss.
     */
    private suspend fun handleChunkFailure(
        error: Throwable,
        chunk: List<PomodoroSessionEntity>,
        retryable: MutableList<Throwable>,
    ) {
        when {
            error is DomainException.NoInternet ||
                error is DomainException.Server ||
                error is DomainException.ServerUnreachable ||
                error is DomainException.Unauthorized -> retryable += error

            error is DomainException.NotFound -> {
                Timber.tag(TAG).w(error, "pomodoro upload endpoint missing; leaving ${chunk.size} rows pending")
            }

            else -> {
                Timber.tag(TAG).e(error, "dropping poisoned pomodoro batch of ${chunk.size}")
                dao.markSynced(chunk.map { it.clientSessionId })
            }
        }
    }

    /**
     * Six-hour cooldown so every caller can fire this without coordinating — the sign-in collector may
     * emit more than once per launch, and re-downloading a year of sessions each time would be rude to a
     * database that scales to zero. Mirrors the 60-second cooldown on task fetches, longer because this
     * history barely moves.
     */
    override suspend fun backfill(fromEpochDay: Long, toEpochDay: Long): Result<Unit> {
        val now = clock.millis()
        if (now - lastBackfillAt < BACKFILL_COOLDOWN_MILLIS) return Result.success(Unit)

        return remote.list(fromEpochDay, toEpochDay).map { data ->
            // Downloaded rows are stamped with the current owner here rather than waiting for the next
            // claim pass; arriving as owner 0 would leave server-confirmed rows looking like guest rows.
            val ownerId = dataStoreHelper.observeUser().first()?.id ?: GUEST_OWNER_ID
            val entities = data.items.map { it.toEntity(ownerId) }
            // IGNORE on the unique client_session_id makes this safe against rows the device is still
            // holding as PENDING_CREATE; markSynced then settles those too.
            dao.insertAll(entities)
            dao.markSynced(entities.map { it.clientSessionId })
            lastBackfillAt = now
        }
    }

    /**
     * No one-time "claimed" flag, deliberately unlike [JournalRepositoryImpl].
     *
     * Sign-out deletes these rows, so a user genuinely can produce a second batch of guest rows
     * (sign out → run a pomodoro → sign in). A one-shot flag would strand those forever. `WHERE
     * owner_user_id = 0` is already a cheap no-op when there is nothing to claim.
     */
    override suspend fun claimOrphansForCurrentUser() {
        val ownerId = dataStoreHelper.observeUser().first()?.id ?: return
        if (ownerId == GUEST_OWNER_ID) return
        dao.claimOrphans(ownerId)
    }

    override suspend fun deleteAllLocal() {
        dao.deleteAll()
    }

    private fun PomodoroSessionEntity.toDto() = PomodoroSessionDto(
        clientSessionId = clientSessionId,
        clientRunId = clientRunId,
        sessionIndex = sessionIndex,
        mode = mode,
        plannedSeconds = plannedSeconds,
        elapsedSeconds = elapsedSeconds,
        completed = completed,
        startedAt = startedAt,
        endedAt = endedAt,
        localDate = localDate,
        tzOffsetMinutes = tzOffsetMinutes,
    )

    private fun PomodoroSessionDto.toEntity(ownerId: Long) = PomodoroSessionEntity(
        clientSessionId = clientSessionId,
        clientRunId = clientRunId,
        sessionIndex = sessionIndex,
        mode = mode,
        plannedSeconds = plannedSeconds,
        elapsedSeconds = elapsedSeconds,
        completed = completed,
        startedAt = startedAt,
        endedAt = endedAt,
        localDate = localDate,
        tzOffsetMinutes = tzOffsetMinutes ?: 0,
        ownerUserId = ownerId,
        syncStatus = SyncStatus.SYNCED,
    )

    private companion object {
        const val TAG = "PomodoroSync"
        const val FOCUS = "FOCUS"
        const val SECONDS_PER_MINUTE = 60
        const val GUEST_OWNER_ID = 0L
        const val PUSH_LIMIT = 500
        const val CHUNK_SIZE = 50
        const val BACKFILL_COOLDOWN_MILLIS = 6L * 60 * 60 * 1000
    }
}
