package com.todoapp.mobile.ui.common.taskform

import androidx.compose.runtime.Immutable
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.Task

/** The task shapes. Derived from a task's data, never a stored column. */
enum class TaskFormType { ONE_TIME, ROUTINE, STAGED, CUSTOM }

/**
 * What a task can actually do, independent of the label it wears.
 *
 * The three classic types are mutually exclusive only by convention — `Task` has always been able to
 * carry a recurrence AND steps at once. Field visibility asks these questions directly rather than
 * comparing against [TaskFormType], so a combination shows the right sections without every gate
 * growing a second arm, and adding a capability later doesn't touch the gates at all.
 */
@Immutable
data class TaskCapabilities(
    val recurs: Boolean,
    val hasSteps: Boolean,
    val hasMultipleReminders: Boolean,
) {
    /**
     * A shape the classic three can't express: a combination, or a brand-new capability.
     *
     * A scheduled end (`recurrenceUntil`) used to count here, which made "every day for a month" a
     * CUSTOM task rather than a routine with a finish line. It is not a different shape — UNTIL sits
     * beside FREQ in the same rule, and `RecurrenceRule` has modelled it as such all along. Treating
     * it as one meant a routine could not be given an end without changing what it was, so the
     * calendar's span gesture had to stay locked to the custom form.
     */
    val isCustom: Boolean get() = (recurs && hasSteps) || hasMultipleReminders

    /** Completion for this task is tracked per-day in `task_daily_completions`, not on the base row. */
    val completionIsPerDay: Boolean get() = recurs

    /** Whole-task completion is derived from its steps and is never directly togglable. */
    val completionIsDerivedFromSteps: Boolean get() = hasSteps
}

fun Task.capabilities(): TaskCapabilities = TaskCapabilities(
    recurs = recurrence != Recurrence.NONE,
    // subtaskTotal covers the list surfaces, where the full subtasks list isn't loaded.
    hasSteps = subtasks.isNotEmpty() || subtaskTotal > 0,
    hasMultipleReminders = reminderTimes.size > 1,
)

/**
 * The badge/accent identity of a task. [TaskCapabilities.isCustom] wins so a combined task never
 * masquerades as one of the classic three — the old derivation silently reported a recurring staged
 * task as STAGED, hiding its schedule.
 */
fun taskFormType(caps: TaskCapabilities): TaskFormType = when {
    caps.isCustom -> TaskFormType.CUSTOM
    caps.hasSteps -> TaskFormType.STAGED
    caps.recurs -> TaskFormType.ROUTINE
    else -> TaskFormType.ONE_TIME
}
