package com.todoapp.mobile.domain.model

import androidx.compose.runtime.Immutable

/**
 * One day's focus total, exactly matching the DAO's `GROUP BY local_date` projection.
 *
 * Grouped on the **device-local** day, not UTC: a session at 01:30 belongs to the day the user
 * experienced, and the alternative would move it whenever the row round-trips through the server.
 */
@Immutable
data class PomodoroDayStat(
    /** Epoch day, device-local. */
    val date: Long,
    /** Sum of elapsed seconds — never planned seconds. */
    val focusSeconds: Long,
    val completedSessions: Int,
)

/**
 * One sitting's totals, read back from the recorded rows.
 *
 * The minute fields are `Int` to match the three arguments `Screen.PomodoroSummary` already carries, so
 * the Summary screen can overwrite its seeded state without a conversion boundary.
 */
@Immutable
data class PomodoroRunSummary(
    val focusSessions: Int,
    val totalFocusMinutes: Int,
    val totalBreakMinutes: Int,
)
