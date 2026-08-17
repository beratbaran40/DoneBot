package com.todoapp.mobile.data.repository

import com.todoapp.mobile.data.source.local.PomodoroSessionDao
import com.todoapp.mobile.domain.model.PomodoroDayStat
import com.todoapp.mobile.domain.model.PomodoroRunSummary
import com.todoapp.mobile.domain.repository.PomodoroSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PomodoroSessionRepositoryImpl
@Inject
constructor(
    private val dao: PomodoroSessionDao,
    private val dataStoreHelper: DataStoreHelper,
) : PomodoroSessionRepository {

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

    private companion object {
        const val FOCUS = "FOCUS"
        const val SECONDS_PER_MINUTE = 60
        const val GUEST_OWNER_ID = 0L
    }
}
