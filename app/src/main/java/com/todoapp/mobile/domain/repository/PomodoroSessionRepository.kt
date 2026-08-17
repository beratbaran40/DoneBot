package com.todoapp.mobile.domain.repository

import com.todoapp.mobile.domain.model.PomodoroDayStat
import com.todoapp.mobile.domain.model.PomodoroRunSummary
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Recorded focus sessions.
 *
 * Distinct from [PomodoroRepository], which owns the single-row *settings* table. That naming collision
 * is unfortunate but the tables really are unrelated: one holds what the user configured, this one holds
 * what actually happened.
 */
interface PomodoroSessionRepository {
    /** Per-day focus totals over a device-local date range, for the Activity screen. */
    fun observeFocusByDay(start: LocalDate, end: LocalDate): Flow<List<PomodoroDayStat>>

    /** Totals for one sitting, so the Summary screen can report persisted truth rather than counters. */
    fun observeRun(clientRunId: String): Flow<PomodoroRunSummary>

    /** Uploads everything not yet synced, in batches. Safe to call repeatedly. */
    suspend fun pushPending(): Result<Unit>

    /**
     * Downloads the account's sessions for a device-local day range and merges them in.
     *
     * Self-throttled, so callers do not have to reason about how often they fire.
     */
    suspend fun backfill(fromEpochDay: Long, toEpochDay: Long): Result<Unit>

    /** Attaches rows produced while signed out to the user who just signed in. */
    suspend fun claimOrphansForCurrentUser()

    /** Sign-out. Runs after the engine has been silenced, never before. */
    suspend fun deleteAllLocal()
}
