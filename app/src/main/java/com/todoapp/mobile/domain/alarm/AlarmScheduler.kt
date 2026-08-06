package com.todoapp.mobile.domain.alarm

import com.todoapp.mobile.domain.model.AlarmItem
import com.todoapp.mobile.domain.model.RecurrenceRule
import java.time.LocalDate

enum class AlarmType {
    TASK,
    DAILY_PLAN,
}

/** Hard cap on reminders per day. See AlarmSchedulerImpl for why 8 and not more. */
const val MAX_REMINDER_SLOTS = 8

interface AlarmScheduler {
    fun schedule(
        item: AlarmItem,
        type: AlarmType,
    )

    fun cancelTask(item: AlarmItem)

    /**
     * Cancels a task's one-shot alarm by id. The [AlarmItem] overload needs a fully-built item just
     * to reach the same request code, which the delete and logout paths cannot produce — the row is
     * already gone by the time they run.
     */
    fun cancelTask(taskId: Long)

    fun cancelScheduledAlarm(type: AlarmType)

    /**
     * Arms the NEXT firing of one recurring reminder slot, per [rule] anchored at [anchorDate] (the
     * date the user picked when creating). The alarm re-arms itself on fire, and stops on its own once
     * the rule is exhausted — a bounded routine needs no separate teardown.
     *
     * [slot] 0 is the task's own reminder; slots 1.. are the extra times of a multi-reminder task.
     * Caller must pass a rule whose frequency != NONE.
     *
     * [isGroupTask] picks a separate request-code namespace. It is load-bearing, not cosmetic: a
     * group task's id comes from the server while a personal one's is a local Room id, so the two
     * counters overlap and would otherwise silently overwrite each other's alarms.
     */
    @Suppress("LongParameterList")
    fun scheduleRecurring(
        taskId: Long,
        rule: RecurrenceRule,
        anchorDate: LocalDate,
        hour: Int,
        minute: Int,
        message: String,
        slot: Int = 0,
        isGroupTask: Boolean = false,
    )

    /**
     * Cancels EVERY reminder slot of a task. Sweeps the full slot range rather than the task's current
     * reminder count: editing 3 reminders down to 2 must not leave slot 2 armed, and an armed slot
     * re-arms itself from its own intent extras forever.
     */
    fun cancelRecurring(taskId: Long, isGroupTask: Boolean = false)
}
