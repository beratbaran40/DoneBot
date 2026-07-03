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
        // Scrub PII (emails / bearer tokens / JWTs) before the message leaves the device as a
        // Crashlytics breadcrumb. Defense-in-depth: nothing logs raw PII today, but this makes a
        // future careless `Timber.e("... $email")` safe by default. See redactLogMessage below.
        crashlytics.log("${priorityLabel(priority)}/${tag ?: "App"}: ${redactLogMessage(message)}")
        // recordException(t) still forwards the throwable's own message/stack. The audit found no
        // PII-carrying throwables; framework exception messages are safe to record as non-fatals.
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

private val EMAIL_REGEX = Regex("""[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}""")
private val BEARER_REGEX = Regex("""(?i)Bearer\s+[A-Za-z0-9._\-]+""")
private val JWT_REGEX = Regex("""eyJ[A-Za-z0-9._\-]{10,}""")

/**
 * Masks personally-identifiable / secret values in a log message so they never reach Crashlytics.
 * Pure and side-effect-free so it can be unit-tested on the JVM without Firebase.
 *
 * Bearer is replaced before the bare-JWT pass so "Bearer eyJ..." collapses to a single redaction.
 */
internal fun redactLogMessage(message: String): String = message
    .replace(BEARER_REGEX, "Bearer [redacted]")
    .replace(JWT_REGEX, "[token]")
    .replace(EMAIL_REGEX, "[email]")
