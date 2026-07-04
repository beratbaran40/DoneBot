package com.todoapp.mobile.data.analytics

import android.content.Context
import androidx.core.os.bundleOf
import com.google.firebase.analytics.FirebaseAnalytics
import com.todoapp.mobile.domain.analytics.AnalyticsHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase-backed [AnalyticsHelper]. Every logEvent is dropped natively by the SDK when the §7.3 opt-out
 * has disabled analytics collection, so no per-event consent check is needed here. Booleans are logged as
 * 1/0 longs so they stay summable/averagable in BigQuery/Analytics.
 */
@Singleton
class FirebaseAnalyticsHelper
@Inject
constructor(
    @ApplicationContext context: Context,
) : AnalyticsHelper {
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)

    override fun logSignUp() {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, null)
    }

    override fun logLogin() {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, null)
    }

    override fun logTaskCreated(hasDue: Boolean, recurrence: String) {
        firebaseAnalytics.logEvent(
            EVENT_TASK_CREATED,
            bundleOf(PARAM_HAS_DUE to hasDue.toBinaryLong(), PARAM_RECURRENCE to recurrence),
        )
    }

    override fun logTaskCompleted() {
        firebaseAnalytics.logEvent(EVENT_TASK_COMPLETED, null)
    }

    override fun logPomodoroCompleted(durationMinutes: Long) {
        firebaseAnalytics.logEvent(EVENT_POMODORO_COMPLETED, bundleOf(PARAM_DURATION to durationMinutes))
    }

    override fun logChatMessageSent(localIntent: Boolean, refused: Boolean, roundTrips: Int) {
        firebaseAnalytics.logEvent(
            EVENT_CHAT_MESSAGE_SENT,
            bundleOf(
                PARAM_LOCAL_INTENT to localIntent.toBinaryLong(),
                PARAM_REFUSED to refused.toBinaryLong(),
                PARAM_ROUND_TRIPS to roundTrips.toLong(),
            ),
        )
    }

    override fun logGroupCreated() {
        firebaseAnalytics.logEvent(EVENT_GROUP_CREATED, null)
    }

    override fun logScreenView(route: String) {
        firebaseAnalytics.logEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            bundleOf(FirebaseAnalytics.Param.SCREEN_NAME to route),
        )
    }

    private fun Boolean.toBinaryLong(): Long = if (this) 1L else 0L

    private companion object {
        const val EVENT_TASK_CREATED = "task_created"
        const val EVENT_TASK_COMPLETED = "task_completed"
        const val EVENT_POMODORO_COMPLETED = "pomodoro_completed"
        const val EVENT_CHAT_MESSAGE_SENT = "chat_message_sent"
        const val EVENT_GROUP_CREATED = "group_created"

        const val PARAM_HAS_DUE = "has_due"
        const val PARAM_RECURRENCE = "recurrence"
        const val PARAM_DURATION = "duration"
        const val PARAM_LOCAL_INTENT = "local_intent"
        const val PARAM_REFUSED = "refused"
        const val PARAM_ROUND_TRIPS = "round_trips"
    }
}
