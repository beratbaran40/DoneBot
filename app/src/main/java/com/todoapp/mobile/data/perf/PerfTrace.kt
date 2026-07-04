package com.todoapp.mobile.data.perf

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace

/**
 * Runs [block] inside a Firebase Performance custom trace named [name]. Trace creation is guarded: if
 * Firebase isn't initialised (e.g. JVM/Robolectric unit tests) or the SDK throws, [block] still runs
 * untraced. No-op-safe when collection is disabled — the SDK records nothing. Use for multi-step flows
 * a single automatic HTTP trace can't capture (e.g. a sync worker that fans out to many requests).
 */
inline fun <T> firebaseTrace(
    name: String,
    block: () -> T,
): T {
    val trace = runCatching { FirebasePerformance.getInstance().newTrace(name).apply { start() } }.getOrNull()
    return try {
        block()
    } finally {
        runCatching { trace?.stop() }
    }
}

/**
 * Cold-start → first-task-list custom trace ([TRACE_NAME]). Distinct from Firebase's automatic
 * `_app_start` (which stops at first onResume): this measures time-to-first-content. Started once per
 * process in `Application.onCreate` and stopped from Home on the first Success render. Main-thread only;
 * both ends run on the main thread. If Home is never reached (signed-out session) the trace is simply
 * never recorded. Trace creation is guarded so an uninitialised Firebase (tests) never throws.
 */
object StartupColdStartTrace {
    private var trace: Trace? = null

    fun start() {
        if (trace != null) return
        trace = runCatching {
            FirebasePerformance.getInstance().newTrace(TRACE_NAME).apply { start() }
        }.getOrNull()
    }

    fun stop() {
        runCatching { trace?.stop() }
        trace = null
    }

    private const val TRACE_NAME = "home_first_task_list"
}
