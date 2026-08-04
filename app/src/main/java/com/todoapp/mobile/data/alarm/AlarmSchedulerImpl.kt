package com.todoapp.mobile.data.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.todoapp.mobile.data.notification.NotificationService
import com.todoapp.mobile.domain.alarm.AlarmScheduler
import com.todoapp.mobile.domain.alarm.AlarmType
import com.todoapp.mobile.domain.alarm.MAX_REMINDER_SLOTS
import com.todoapp.mobile.domain.model.AlarmItem
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.RecurrenceRule
import com.todoapp.mobile.domain.model.firesOn
import com.todoapp.mobile.domain.model.toStorageCsv
import com.todoapp.mobile.ui.overlay.OverlayService
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class AlarmSchedulerImpl(
    private val context: Context,
) : AlarmScheduler {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(
        item: AlarmItem,
        type: AlarmType,
    ) {
        scheduleAt(
            triggerAtMillis = item.time
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
            pendingIntent = buildFirePendingIntent(type.getRequestCode(item), type.buildBroadcastIntent(item)),
        )
    }

    override fun cancelTask(item: AlarmItem) {
        cancelAlarm(taskRequestCode(item))
    }

    override fun cancelScheduledAlarm(type: AlarmType) {
        val requestCode =
            when (type) {
                AlarmType.TASK -> return
                AlarmType.DAILY_PLAN -> REQUEST_CODE_DAILY_PLAN
            }
        cancelAlarm(requestCode)
    }

    private fun cancelAlarm(requestCode: Int) {
        // The ACTION is load-bearing. PendingIntent matching runs Intent.filterEquals, which ignores
        // extras (so the fire-target/message/rule extras don't matter) but DOES compare the action.
        // Cancelling with an action-less intent therefore matched nothing: AlarmManager.cancel got a
        // freshly-minted PendingIntent and silently cancelled nothing, so a deleted or edited-away
        // recurring alarm stayed armed — and since AlarmFireReceiver re-arms itself from its own
        // extras, it kept firing forever. Verified on-device 2026-08-03.
        alarmManager.cancel(
            buildFirePendingIntent(
                requestCode,
                Intent(context, AlarmFireReceiver::class.java).apply { action = AlarmFireReceiver.ACTION_FIRE },
            ),
        )
    }

    private fun buildFirePendingIntent(
        requestCode: Int,
        intent: Intent,
    ): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        intent.apply { setClass(context, AlarmFireReceiver::class.java) },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun AlarmType.getRequestCode(item: AlarmItem): Int = when (this) {
        AlarmType.TASK -> taskRequestCode(item)
        AlarmType.DAILY_PLAN -> REQUEST_CODE_DAILY_PLAN
    }

    // Deduping seed: prefer a stable per-task code so re-scheduling the same task
    // (e.g. user edits the time) updates the existing PendingIntent (FLAG_UPDATE_CURRENT)
    // instead of leaving the previous alarm armed alongside the new one.
    private fun taskRequestCode(item: AlarmItem): Int = item.taskId?.let { (TASK_REQUEST_BASE + it).toInt() } ?: item.hashCode()

    private fun AlarmType.buildBroadcastIntent(item: AlarmItem): Intent = when (this) {
        AlarmType.TASK -> buildPreferredBroadcast(item, OverlayService.OVERLAY_TYPE_TASK)
        AlarmType.DAILY_PLAN -> buildPreferredBroadcast(item, OverlayService.OVERLAY_TYPE_DAILY_PLAN)
    }

    private fun buildPreferredBroadcast(
        item: AlarmItem,
        overlayType: String,
    ): Intent = if (Settings.canDrawOverlays(context)) {
        buildOverlayBroadcast(item, overlayType)
    } else {
        buildNotificationBroadcast(item)
    }

    private fun buildOverlayBroadcast(
        item: AlarmItem,
        overlayType: String,
    ): Intent = Intent(context, AlarmFireReceiver::class.java).apply {
        action = AlarmFireReceiver.ACTION_FIRE
        putExtra(AlarmFireReceiver.EXTRA_FIRE_TARGET, AlarmFireReceiver.FIRE_TARGET_OVERLAY)
        putExtra(OverlayService.INTENT_EXTRA_COMMAND_SHOW_OVERLAY, item.message)
        putExtra(OverlayService.INTENT_EXTRA_LONG, item.minutesBefore)
        putExtra(OverlayService.INTENT_EXTRA_OVERLAY_TYPE, overlayType)
        item.taskId?.let { putExtra(AlarmFireReceiver.EXTRA_TASK_ID, it) }
    }

    private fun buildNotificationBroadcast(item: AlarmItem): Intent = Intent(
        context,
        AlarmFireReceiver::class.java,
    ).apply {
        action = AlarmFireReceiver.ACTION_FIRE
        putExtra(AlarmFireReceiver.EXTRA_FIRE_TARGET, AlarmFireReceiver.FIRE_TARGET_NOTIFICATION)
        putExtra(NotificationService.INTENT_EXTRA_MESSAGE, item.message)
        putExtra(NotificationService.INTENT_EXTRA_LONG, item.minutesBefore)
        item.taskId?.let { putExtra(AlarmFireReceiver.EXTRA_TASK_ID, it) }
    }

    @Suppress("LongParameterList")
    override fun scheduleRecurring(
        taskId: Long,
        rule: RecurrenceRule,
        anchorDate: LocalDate,
        hour: Int,
        minute: Int,
        message: String,
        slot: Int,
        isGroupTask: Boolean,
    ) {
        if (rule.frequency == Recurrence.NONE) return
        // null = the rule is exhausted (its scheduled end has passed). Arming nothing is exactly how a
        // bounded routine stops itself, so this is a normal exit, not an error.
        val nextFire = computeNextFire(rule, anchorDate, LocalTime.of(hour, minute), LocalDateTime.now()) ?: return
        val intent = buildRecurringTaskBroadcast(
            taskId = taskId,
            rule = rule,
            anchorDate = anchorDate,
            hour = hour,
            minute = minute,
            message = message,
            slot = slot,
            isGroupTask = isGroupTask,
        )
        scheduleAt(
            triggerAtMillis = nextFire.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            pendingIntent = buildFirePendingIntent(recurringRequestCode(taskId, slot, isGroupTask), intent),
        )
    }

    // Falls back to inexact when SCHEDULE_EXACT_ALARM isn't granted (Android 13+ user-controlled).
    private fun scheduleAt(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        // AlarmManager fires past triggers immediately ("set me a reminder for 9 AM yesterday"
        // would pop the overlay right now). Repository call-sites already guard, but this is
        // defense-in-depth for any future scheduler path that forgets to check.
        if (triggerAtMillis <= System.currentTimeMillis()) {
            Timber.tag(TAG).d("scheduleAt: skipping past trigger=%d", triggerAtMillis)
            return
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        }.onFailure { Timber.tag(TAG).w(it, "scheduleAt failed") }
    }

    override fun cancelRecurring(taskId: Long, isGroupTask: Boolean) {
        // Full sweep, not "however many reminders the task has now" — a slot dropped by an edit would
        // otherwise stay armed and keep re-arming itself from its own extras.
        for (slot in 0 until MAX_REMINDER_SLOTS) {
            cancelAlarm(recurringRequestCode(taskId, slot, isGroupTask))
        }
    }

    /**
     * Walks forward day by day and asks the SAME [firesOn] the task list uses. One shared predicate is
     * the point: four hand-written per-frequency branches would drift from the list the moment an
     * interval or a weekday set entered the picture, and the alarm would fire on days the task isn't
     * shown (or stay silent on days it is).
     *
     * Returns null when the rule can no longer fire — previously the monthly/yearly branches called
     * `error(...)` here, which with a scheduled end would have thrown inside a BroadcastReceiver.
     */
    private fun computeNextFire(
        rule: RecurrenceRule,
        anchorDate: LocalDate,
        time: LocalTime,
        now: LocalDateTime,
    ): LocalDateTime? {
        var day = now.toLocalDate()
        repeat(MAX_FIRE_LOOKAHEAD_DAYS) {
            val candidate = day.atTime(time)
            if (candidate.isAfter(now) && rule.firesOn(anchorDate, day)) return candidate
            day = day.plusDays(1)
        }
        return null
    }

    private fun buildRecurringTaskBroadcast(
        taskId: Long,
        rule: RecurrenceRule,
        anchorDate: LocalDate,
        hour: Int,
        minute: Int,
        message: String,
        slot: Int,
        isGroupTask: Boolean,
    ): Intent {
        val base = if (Settings.canDrawOverlays(context)) {
            Intent(context, AlarmFireReceiver::class.java).apply {
                action = AlarmFireReceiver.ACTION_FIRE
                putExtra(AlarmFireReceiver.EXTRA_FIRE_TARGET, AlarmFireReceiver.FIRE_TARGET_OVERLAY)
                putExtra(OverlayService.INTENT_EXTRA_COMMAND_SHOW_OVERLAY, message)
                putExtra(OverlayService.INTENT_EXTRA_LONG, 0L)
                putExtra(OverlayService.INTENT_EXTRA_OVERLAY_TYPE, OverlayService.OVERLAY_TYPE_TASK)
            }
        } else {
            Intent(context, AlarmFireReceiver::class.java).apply {
                action = AlarmFireReceiver.ACTION_FIRE
                putExtra(AlarmFireReceiver.EXTRA_FIRE_TARGET, AlarmFireReceiver.FIRE_TARGET_NOTIFICATION)
                putExtra(NotificationService.INTENT_EXTRA_MESSAGE, message)
                putExtra(NotificationService.INTENT_EXTRA_LONG, 0L)
            }
        }
        return base.apply {
            putExtra(AlarmFireReceiver.EXTRA_RECURRENCE, rule.frequency.name)
            putExtra(AlarmFireReceiver.EXTRA_ANCHOR_EPOCH_DAY, anchorDate.toEpochDay())
            putExtra(AlarmFireReceiver.EXTRA_DAILY_TASK_ID, taskId)
            putExtra(AlarmFireReceiver.EXTRA_DAILY_HOUR, hour)
            putExtra(AlarmFireReceiver.EXTRA_DAILY_MINUTE, minute)
            putExtra(AlarmFireReceiver.EXTRA_DAILY_MESSAGE, message)
            // §5.8: also carry taskId under the generic key the receiver forwards to the service.
            putExtra(AlarmFireReceiver.EXTRA_TASK_ID, taskId)
            // V3 extras. PendingIntents armed by an older build carry none of these and WILL fire after
            // an update, so every one must decode to its legacy value when absent — see the receiver.
            putExtra(AlarmFireReceiver.EXTRA_RECURRENCE_INTERVAL, rule.interval)
            putExtra(AlarmFireReceiver.EXTRA_RECURRENCE_BY_DAY, rule.byDay.toStorageCsv())
            putExtra(
                AlarmFireReceiver.EXTRA_RECURRENCE_UNTIL_EPOCH_DAY,
                rule.until?.toEpochDay() ?: AlarmFireReceiver.NO_EPOCH_DAY,
            )
            putExtra(AlarmFireReceiver.EXTRA_REMINDER_SLOT, slot)
            // Carried so the self-re-arm on fire lands in the SAME request-code space. Without it a
            // group alarm would re-arm itself as a personal one and drift out of reach of cancel.
            putExtra(AlarmFireReceiver.EXTRA_IS_GROUP_TASK, isGroupTask)
        }
    }

    /**
     * Slot 0 deliberately keeps the pre-multi-reminder request code: an alarm armed by an older build
     * is then REPLACED (FLAG_UPDATE_CURRENT) instead of running alongside the new one.
     */
    private fun recurringRequestCode(
        taskId: Long,
        slot: Int,
        isGroupTask: Boolean = false,
    ): Int = recurringAlarmRequestCode(taskId, slot, isGroupTask)

    private companion object {
        const val REQUEST_CODE_DAILY_PLAN = 10_001

        // The request-code bases and their disjointness argument live in AlarmRequestCodes.kt, where
        // they can be tested as the pure arithmetic they are.

        // 400 days covers YEARLY across a leap year, which is the widest gap any rule can produce.
        const val MAX_FIRE_LOOKAHEAD_DAYS = 400

        const val TAG = "AlarmScheduler"
    }
}
