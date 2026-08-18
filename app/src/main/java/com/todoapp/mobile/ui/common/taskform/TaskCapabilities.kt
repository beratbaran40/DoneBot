package com.todoapp.mobile.ui.common.taskform

import androidx.compose.runtime.Immutable
import com.todoapp.mobile.domain.model.GroupTask
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.Task
import com.todoapp.mobile.domain.model.TaskType

/**
 * What a task can actually do, independent of the label it wears.
 *
 * The three classic types are mutually exclusive only by convention — `Task` has always been able to
 * carry a recurrence AND steps at once. Field visibility asks these questions directly rather than
 * comparing against [TaskType], so a combination shows the right sections without every gate
 * growing a second arm, and adding a capability later doesn't touch the gates at all.
 *
 * These stay DERIVED even now that the type itself is stored: a declaration says what the user set
 * out to make, while the form has to render what the row currently is.
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
     *
     * The cost of that — a task the user declared CUSTOM reporting itself as a ROUTINE — is paid by
     * [Task.declaredType] instead, which this function is now only the fallback for.
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
 * The same three questions asked of a group task.
 *
 * A group task has carried the full rule — frequency, interval, weekdays, scheduled end, reminder
 * times and steps — since it reached parity with a personal one, but nothing on the group screens
 * ever asked what shape that made it, so a group routine, staged goal and custom task all rendered
 * as the same anonymous row.
 *
 * There is no group equivalent of `Task.declaredType`, and deliberately so: the group task cache is
 * wiped and re-inserted wholesale on every sync, so a local-only column would not survive the next
 * pull — and a declaration only one member's phone could see would render the same task with two
 * different badges. It waits on the backend field.
 */
fun GroupTask.capabilities(): TaskCapabilities = TaskCapabilities(
    recurs = recurrence != Recurrence.NONE,
    hasSteps = subtasks.isNotEmpty(),
    hasMultipleReminders = reminderTimes.size > 1,
)

/**
 * The best guess at a task's shape from its data alone. [TaskCapabilities.isCustom] wins so a
 * combined task never masquerades as one of the classic three — the old derivation silently reported
 * a recurring staged task as STAGED, hiding its schedule.
 *
 * Only ever reached for a task with no [Task.declaredType]: one created before the column existed, or
 * one that arrived from the server (the wire does not carry the declaration yet). Prefer
 * [Task.resolvedType].
 */
fun derivedTaskType(caps: TaskCapabilities): TaskType = when {
    caps.isCustom -> TaskType.CUSTOM
    caps.hasSteps -> TaskType.STAGED
    caps.recurs -> TaskType.ROUTINE
    else -> TaskType.ONE_TIME
}

/**
 * The type to show for this task: what the user declared, or — failing that — what the data implies.
 *
 * The single place the fallback is spelled. Keeping it to one expression is the point: a second copy
 * would be free to drift, and the drift would only ever show up as a wrong badge on somebody's task.
 */
fun Task.resolvedType(): TaskType = declaredType ?: derivedTaskType(capabilities())
