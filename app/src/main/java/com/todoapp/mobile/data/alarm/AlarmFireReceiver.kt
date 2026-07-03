package com.todoapp.mobile.data.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.todoapp.mobile.data.notification.NotificationService
import com.todoapp.mobile.domain.alarm.AlarmScheduler
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.ui.overlay.OverlayService
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject

/**
 * Bridges AlarmManager → service start. Broadcast receivers are exempt from the Android 12+
 * background-service-start restrictions, so this lets the alarm fire its overlay/notification
 * even when the app process has been killed (swiped from recents).
 */
@AndroidEntryPoint
class AlarmFireReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent?) {
        intent ?: return
        val target = intent.getStringExtra(EXTRA_FIRE_TARGET).orEmpty()
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        Timber.tag(TAG).d("alarm fired target=%s", target)

        val serviceIntent = when (target) {
            FIRE_TARGET_OVERLAY -> Intent(context, OverlayService::class.java).apply {
                putExtra(
                    OverlayService.INTENT_EXTRA_COMMAND_SHOW_OVERLAY,
                    intent.getStringExtra(OverlayService.INTENT_EXTRA_COMMAND_SHOW_OVERLAY).orEmpty(),
                )
                putExtra(
                    OverlayService.INTENT_EXTRA_LONG,
                    intent.getLongExtra(OverlayService.INTENT_EXTRA_LONG, 0L),
                )
                putExtra(
                    OverlayService.INTENT_EXTRA_OVERLAY_TYPE,
                    intent.getStringExtra(OverlayService.INTENT_EXTRA_OVERLAY_TYPE)
                        ?: OverlayService.OVERLAY_TYPE_TASK,
                )
                if (taskId > 0) putExtra(OverlayService.INTENT_EXTRA_TASK_ID, taskId)
            }
            FIRE_TARGET_NOTIFICATION -> Intent(context, NotificationService::class.java).apply {
                putExtra(
                    NotificationService.INTENT_EXTRA_MESSAGE,
                    intent.getStringExtra(NotificationService.INTENT_EXTRA_MESSAGE).orEmpty(),
                )
                putExtra(
                    NotificationService.INTENT_EXTRA_LONG,
                    intent.getLongExtra(NotificationService.INTENT_EXTRA_LONG, 0L),
                )
                if (taskId > 0) putExtra(NotificationService.INTENT_EXTRA_TASK_ID, taskId)
            }
            else -> {
                Timber.tag(TAG).w("Unknown fire target=%s; dropping alarm", target)
                return
            }
        }

        runCatching {
            // startForegroundService is exempt from background-start checks when invoked from
            // a broadcast receiver; the receiving service must promote itself within 5s.
            ContextCompat.startForegroundService(context, serviceIntent)
        }.onFailure { Timber.tag(TAG).w(it, "startForegroundService failed for target=%s", target) }

        rescheduleNextInstanceIfRecurring(intent)
    }

    /**
     * Compat path: V2 sends EXTRA_RECURRENCE; V1 sent EXTRA_IS_DAILY=true. Honor both for one
     * release so users with already-armed daily alarms don't lose their next firing after upgrade.
     */
    private fun rescheduleNextInstanceIfRecurring(intent: Intent) {
        val recurrenceName = intent.getStringExtra(EXTRA_RECURRENCE)
        val recurrence = when {
            !recurrenceName.isNullOrEmpty() -> Recurrence.fromStorage(recurrenceName)
            intent.getBooleanExtra(EXTRA_IS_DAILY, false) -> Recurrence.DAILY
            else -> return
        }
        if (recurrence == Recurrence.NONE) return

        val taskId = intent.getLongExtra(EXTRA_DAILY_TASK_ID, -1L)
        val hour = intent.getIntExtra(EXTRA_DAILY_HOUR, -1)
        val minute = intent.getIntExtra(EXTRA_DAILY_MINUTE, -1)
        val message = intent.getStringExtra(EXTRA_DAILY_MESSAGE).orEmpty()
        val anchorEpochDay = intent.getLongExtra(EXTRA_ANCHOR_EPOCH_DAY, -1L)
        val anchor = if (anchorEpochDay >= 0) LocalDate.ofEpochDay(anchorEpochDay) else LocalDate.now()

        if (taskId < 0 || hour !in 0..23 || minute !in 0..59) return

        runCatching {
            alarmScheduler.scheduleRecurring(taskId, recurrence, anchor, hour, minute, message)
        }.onFailure { Timber.tag(TAG).w(it, "failed to re-arm recurring alarm taskId=%d", taskId) }
    }

    companion object {
        const val ACTION_FIRE: String = "com.todoapp.mobile.alarm.action.FIRE"
        const val EXTRA_FIRE_TARGET: String = "com.todoapp.mobile.alarm.extra.FIRE_TARGET"
        const val FIRE_TARGET_OVERLAY: String = "OVERLAY"
        const val FIRE_TARGET_NOTIFICATION: String = "NOTIFICATION"

        /** Task id carried on the fire broadcast so the notification/overlay can deep-link to it. §5.8 */
        const val EXTRA_TASK_ID: String = "com.todoapp.mobile.alarm.extra.TASK_ID"

        // V2 keys.
        const val EXTRA_RECURRENCE: String = "com.todoapp.mobile.alarm.extra.RECURRENCE"
        const val EXTRA_ANCHOR_EPOCH_DAY: String = "com.todoapp.mobile.alarm.extra.ANCHOR_EPOCH_DAY"

        // V1 compat keys (still read so already-armed daily alarms re-schedule after upgrade).
        const val EXTRA_IS_DAILY: String = "com.todoapp.mobile.alarm.extra.IS_DAILY"
        const val EXTRA_DAILY_TASK_ID: String = "com.todoapp.mobile.alarm.extra.DAILY_TASK_ID"
        const val EXTRA_DAILY_HOUR: String = "com.todoapp.mobile.alarm.extra.DAILY_HOUR"
        const val EXTRA_DAILY_MINUTE: String = "com.todoapp.mobile.alarm.extra.DAILY_MINUTE"
        const val EXTRA_DAILY_MESSAGE: String = "com.todoapp.mobile.alarm.extra.DAILY_MESSAGE"

        private const val TAG: String = "AlarmFireReceiver"
    }
}
