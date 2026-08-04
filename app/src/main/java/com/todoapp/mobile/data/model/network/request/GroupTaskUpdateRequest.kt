package com.todoapp.mobile.data.model.network.request

import kotlinx.serialization.Serializable

/**
 * Partial update for a group task. Field semantics:
 *  - omitted (null) -> no change
 *  - non-null value -> set to that value
 *  - assigneeId + clearAssignee=true -> explicit unassign (omit assigneeId)
 */
@Serializable
data class GroupTaskUpdateRequest(
    val title: String? = null,
    val description: String? = null,
    val dueDate: Long? = null,
    val isAllDay: Boolean? = null,
    val timeStart: Long? = null,
    val timeEnd: Long? = null,
    val isCompleted: Boolean? = null,
    val priority: String? = null,
    val assigneeId: Long? = null,
    val clearAssignee: Boolean = false,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val locationName: String? = null,
    val locationAddress: String? = null,
    /** Set true to wipe all four location fields in one request. */
    val clearLocation: Boolean = false,
    val recurrence: String? = null,
    val recurrenceInterval: Int? = null,
    val recurrenceByDay: String? = null,
    val recurrenceUntil: Long? = null,
    /** SECOND-of-day on the wire, like every other reminder-time field. */
    val reminderTimes: List<Int>? = null,
    val category: String? = null,
    val customCategoryName: String? = null,
    /** Non-null replaces the whole step set; null leaves the existing steps untouched. */
    val subtasks: List<SubtaskRequest>? = null,
)
