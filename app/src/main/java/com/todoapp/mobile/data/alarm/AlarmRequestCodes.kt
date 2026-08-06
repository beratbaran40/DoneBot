package com.todoapp.mobile.data.alarm

import com.todoapp.mobile.domain.alarm.MAX_REMINDER_SLOTS

/**
 * The request code an alarm's PendingIntent is filed under. Pure arithmetic, extracted from
 * [AlarmSchedulerImpl] so the one property that matters can actually be tested: **two different
 * (task, slot) pairs must never produce the same code.**
 *
 * A collision is invisible and destructive — `FLAG_UPDATE_CURRENT` silently replaces the other
 * alarm, so one task stops reminding and nothing in the app reports a problem.
 *
 * The bases carve out disjoint ranges of the signed 32-bit space:
 * - `0x0100_0000` recurring personal, slot 0 (kept unshifted for pre-multi-reminder compatibility)
 * - `0x0200_0000` one-shot personal
 * - `0x1000_0000` recurring personal, slots 1..7
 * - `0x4000_0000` group, all slots
 *
 * Group tasks need their own range because their ids come from the **server** while personal ids are
 * local Room ids: both start at 1 and would otherwise overwrite each other.
 */
internal fun recurringAlarmRequestCode(
    taskId: Long,
    slot: Int,
    isGroupTask: Boolean,
): Int = when {
    isGroupTask -> (GROUP_REQUEST_BASE + taskId * MAX_REMINDER_SLOTS + slot).toInt()
    slot == 0 -> (RECURRING_TASK_REQUEST_BASE + taskId).toInt()
    else -> (MULTI_REMINDER_REQUEST_BASE + taskId * MAX_REMINDER_SLOTS + slot).toInt()
}

/**
 * The one-shot ("remind me N minutes before") alarm of a personal task.
 *
 * Derived from the task id alone so scheduling the same task twice REPLACES the armed alarm instead
 * of adding a second one — and, just as importantly, so that cancelling needs nothing but the id.
 * The id must be the **local Room id**: passing a task that has not been inserted yet gives every
 * such task the code of id 0, and each new one silently unschedules the last.
 */
internal fun oneShotAlarmRequestCode(taskId: Long): Int = (TASK_REQUEST_BASE + taskId).toInt()

internal const val TASK_REQUEST_BASE = 0x0200_0000L
internal const val RECURRING_TASK_REQUEST_BASE = 0x0100_0000L
internal const val MULTI_REMINDER_REQUEST_BASE = 0x1000_0000L
internal const val GROUP_REQUEST_BASE = 0x4000_0000L
