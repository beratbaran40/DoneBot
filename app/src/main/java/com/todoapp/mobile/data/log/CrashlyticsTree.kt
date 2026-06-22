package com.todoapp.mobile.data.log

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

/**
 * Release-only Timber tree that forwards logs to Crashlytics:
 *  - WARN and above are written as breadcrumbs (the trail shown next to a crash).
 *  - ERROR/ASSERT carrying a Throwable are also recorded as non-fatals (visible as issues in
 *    Crashlytics even when the app didn't actually crash).
 *
 * Handled WARN-level throwables (e.g. a graceful reschedule / Places-init failure) stay as
 * breadcrumbs only — recording them as non-fatals would flood the dashboard with expected noise.
 * Debug builds plant Timber.DebugTree instead (see Application.initCrashReporting).
 */
class CrashlyticsTree : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= Log.WARN

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val crashlytics = FirebaseCrashlytics.getInstance()
        tag?.let { crashlytics.setCustomKey(KEY_LAST_TAG, it) }
        crashlytics.log("${priorityLabel(priority)}/${tag ?: "App"}: $message")
        if (t != null && priority >= Log.ERROR) {
            crashlytics.recordException(t)
        }
    }

    private fun priorityLabel(priority: Int): String = when (priority) {
        Log.WARN -> "W"
        Log.ERROR -> "E"
        Log.ASSERT -> "A"
        else -> priority.toString()
    }

    private companion object {
        const val KEY_LAST_TAG = "last_log_tag"
    }
}
