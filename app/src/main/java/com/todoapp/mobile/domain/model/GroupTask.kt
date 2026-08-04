package com.todoapp.mobile.domain.model

import androidx.compose.runtime.Immutable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@Immutable
data class GroupTask(
    val id: Long,
    val title: String,
    val description: String?,
    val isCompleted: Boolean,
    val priority: String?,
    val dueDate: Long?,
    val assignee: GroupMember?,
    val isAllDay: Boolean = false,
    val timeStart: LocalTime? = null,
    val timeEnd: LocalTime? = null,
    val photoUrls: List<String> = emptyList(),
    val groupId: Long? = null,
    val locationName: String? = null,
    val locationAddress: String? = null,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    /**
     * Everything below brings a group task level with a personal one. A group task is the same
     * `TaskEntity` on the server, so these were always storable — only the client dropped them.
     * The defaults describe the flat, one-off, stepless task a group task used to be limited to.
     */
    val category: TaskCategory = TaskCategory.PERSONAL,
    val customCategoryName: String? = null,
    val recurrence: Recurrence = Recurrence.NONE,
    /** Fire every N periods of [recurrence]. 1 = every period. Meaningless when recurrence is NONE. */
    val recurrenceInterval: Int = 1,
    /** WEEKLY only: the weekdays to fire on. Empty = the anchor's own weekday. */
    val recurrenceByDay: Set<DayOfWeek> = emptySet(),
    /** The routine's scheduled last day, inclusive. Null = open-ended. */
    val recurrenceUntil: LocalDate? = null,
    /** Absolute times of day this task reminds at, applied to every occurrence. */
    val reminderTimes: List<LocalTime> = emptyList(),
    /** Ordered steps. Non-empty ⇒ this group task is "staged". */
    val subtasks: List<Subtask> = emptyList(),
    /**
     * Whether **the day being viewed** is already done, for a recurring group task. Shared across the
     * group: whoever ticks it first completes that occurrence for everyone, so this deliberately says
     * nothing about *who* did it. Meaningless when [recurrence] is NONE — [isCompleted] rules there.
     */
    val isCompletedToday: Boolean = false,
)

/**
 * The group task's repeat rule as one value object, so it feeds the exact same [RecurrenceRule.firesOn],
 * next-fire and day-N-of-M helpers the personal side uses — no parallel implementation to drift.
 */
val GroupTask.recurrenceRule: RecurrenceRule
    get() = RecurrenceRule(recurrence, recurrenceInterval, recurrenceByDay, recurrenceUntil)

/** The task's start day, derived from the epoch-millis [GroupTask.dueDate] the backend speaks. */
val GroupTask.startDate: LocalDate?
    get() = dueDate?.let {
        java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    }

/**
 * Whether this task belongs on [date] — one occurrence of a routine, or the single day of a one-off.
 *
 * Surfaces used to compare the due date for equality, which is right for a one-off and silently
 * wrong for everything else: a daily group chore showed up on its start day only. Delegates to the
 * same pure [RecurrenceRule.firesOn] the personal side uses, so the two can't drift.
 */
fun GroupTask.firesOnDate(date: LocalDate): Boolean {
    val anchor = startDate ?: return false
    // firesOn already answers `anchor == day` for NONE, so one-offs need no special case here.
    return recurrenceRule.firesOn(anchor = anchor, day = date)
}
