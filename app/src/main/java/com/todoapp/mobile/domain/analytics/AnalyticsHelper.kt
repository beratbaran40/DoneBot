package com.todoapp.mobile.domain.analytics

/**
 * Minimal product-analytics taxonomy (§7.17): signup funnel, activation (first task), engagement
 * (completion, pomodoro, chat, groups), and screen flow. Wraps the analytics SDK so call-sites depend on
 * a domain interface, not Firebase. Event/param names are the wire contract with the analytics backend —
 * documented in docs/analytics-events.md.
 *
 * Collection is gated by the §7.3 opt-out ([com.todoapp.mobile.domain.repository.CrashAnalyticsPreferences]):
 * when the user opts out, the app calls setAnalyticsCollectionEnabled(false) and the SDK drops every event
 * below natively — so these methods need no per-call consent guard.
 */
interface AnalyticsHelper {
    fun logSignUp()

    fun logLogin()

    fun logTaskCreated(hasDue: Boolean, recurrence: String)

    fun logTaskCompleted()

    fun logPomodoroCompleted(durationMinutes: Long)

    fun logChatMessageSent(localIntent: Boolean, refused: Boolean, roundTrips: Int)

    fun logGroupCreated()

    fun logScreenView(route: String)
}
