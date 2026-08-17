package com.todoapp.mobile.domain.engine

/**
 * Where finished and abandoned intervals go to be persisted.
 *
 * The hook lives on the **engine**, not on `PomodoroViewModel`, and that placement is the point.
 * `PomodoroEngineImpl.emitIfSubscribed` drops events whenever no ViewModel is collecting, so a run the
 * user backgrounds currently records nothing at all — not the summary totals, not even the Firebase
 * `pomodoro_completed` event. The engine is also the only thing that knows `remainingMillis` and holds
 * the real [Session] the run was built from, so it is the only place that can state elapsed time and
 * duration correctly for a session DoneBot started with its own timing.
 */
interface PomodoroSessionRecorder {
    /**
     * Fire-and-forget. Must never block the caller and must never throw: it is invoked from the engine's
     * control path, where an exception would abort a session transition mid-way.
     */
    fun record(record: PomodoroSessionRecord)

    /** Called once per run, not once per session — the cue to flush whatever has accumulated. */
    fun onRunEnded()

    fun shutdown()
}

/**
 * One interval, as the engine saw it at the moment it ended.
 *
 * Immutable and built by the engine from its own fields **before it mutates them**, on the calling
 * thread. The recorder must never reach back into engine state: by the time it runs, the engine has
 * already moved on to the next session.
 */
data class PomodoroSessionRecord(
    val clientSessionId: String,
    val clientRunId: String,
    val sessionIndex: Int,
    val mode: PomodoroMode,
    val plannedSeconds: Long,
    /** What actually ran. Never equal to [plannedSeconds] unless [completed] is true. */
    val elapsedSeconds: Long,
    /** True only when the countdown reached zero on its own. */
    val completed: Boolean,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
)
