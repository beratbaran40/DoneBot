package com.todoapp.mobile.domain.engine

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface PomodoroEngine {
    val state: StateFlow<PomodoroEngineState>
    val events: SharedFlow<PomodoroEvent>

    /**
     * Immutable snapshot of the queue last passed to [setSessionQueue]. Read by
     * PomodoroViewModel on screen mount to mirror an already-running session
     * (e.g. one started by DoneBot via LocalIntentClassifier) instead of
     * rebuilding from saved Pomodoro settings.
     */
    val sessionsSnapshot: List<Session>

    /**
     * Id shared by every interval of the run in flight, or null when nothing is queued. Read by
     * PomodoroViewModel so the Summary screen can look its own run up in the recorded rows instead of
     * trusting counters that only accumulate while the screen is alive.
     */
    val currentRunId: String?

    fun setSessionQueue(queue: ArrayDeque<Session>)

    fun prepare()

    fun start()

    fun pause()

    fun skip(autoStart: Boolean)

    fun finish()

    /**
     * Tears the run down without emitting anything.
     *
     * Exists because [finish] emits [PomodoroEvent.PomodoroFinished], which is correct when a run ends
     * but wrong for every other teardown: logout used to call it and could navigate a mounted Pomodoro
     * screen to the Summary in the middle of signing out.
     *
     * @param record whether the interval in flight should be persisted as abandoned. False on logout —
     *   the recorder writes on an IO scope while the sign-out chain deletes, and a row that wins that
     *   race leaks into the next account.
     */
    fun stop(record: Boolean)

    fun updateBannerVisibility(isVisible: Boolean)

    fun shutdown()

    /**
     * Silently clears all engine state (queue, counters, jobs, alarms, service)
     * without emitting any events. Called from PomodoroViewModel.init to make
     * sure no leftover state from a previous session sticks around.
     */
    fun resetState()
}

data class PomodoroEngineState(
    val remainingSeconds: Long = DEFAULT_REMAINING_SECONDS,
    val currentSessionTotalSeconds: Long = DEFAULT_REMAINING_SECONDS,
    val mode: PomodoroMode = PomodoroMode.Focus,
    val isRunning: Boolean = false,
    val isOvertime: Boolean = false,
    val isVisible: Boolean = true,
    val totalSessions: Int = 0,
    val currentSessionIndex: Int = 0,
)

sealed interface PomodoroEvent {
    data object SessionFinished : PomodoroEvent

    data object PomodoroFinished : PomodoroEvent
}

enum class PomodoroMode {
    Focus,
    ShortBreak,
    LongBreak,
    OverTime,
}

data class Session(
    val durationSeconds: Long,
    val mode: PomodoroMode,
)

private const val DEFAULT_FOCUS_MINUTES: Long = 25L
private const val SECONDS_PER_MINUTE: Long = 60L
private const val DEFAULT_REMAINING_SECONDS: Long = DEFAULT_FOCUS_MINUTES * SECONDS_PER_MINUTE
