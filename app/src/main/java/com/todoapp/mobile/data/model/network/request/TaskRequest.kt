package com.todoapp.mobile.data.model.network.request

import kotlinx.serialization.Serializable

@Serializable
data class TaskRequest(
    val id: Long? = null,
    val clientTaskId: String? = null,
    val title: String,
    val description: String?,
    val date: Long,
    val timeStart: Long,
    val timeEnd: Long,
    val isCompleted: Boolean,
    val isSecret: Boolean,
    val familyGroupId: Long? = null,
    val assignedToUserId: Long? = null,
    val priority: String? = null,
    val category: String? = null,
    val customCategoryName: String? = null,
    val recurrence: String? = null,
    val finishedOn: Long? = null,
    val isAllDay: Boolean = false,
    val reminderOffsetMinutes: Long = 0L,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val locationName: String? = null,
    val locationAddress: String? = null,
    /**
     * Ordered steps of a staged task. `null` = leave the server's steps untouched (sent for
     * non-staged tasks, so an empty list never wipes another device's steps); a non-null list =
     * reconcile the step set. The client only sends a non-null list when the task actually has steps.
     */
    val subtasks: List<SubtaskRequest>? = null,
    /** RRULE INTERVAL — fire every N periods. Only honoured by the server when [recurrenceRuleSet]. */
    val recurrenceInterval: Int? = null,
    /** RRULE BYDAY as a DayOfWeek-name CSV ("MONDAY,WEDNESDAY,FRIDAY"). */
    val recurrenceByDay: String? = null,
    /** RRULE UNTIL as an epoch day — the routine's scheduled end, inclusive. */
    val recurrenceUntil: Long? = null,
    /**
     * Absolute reminder times as **SECOND**-of-day, matching [timeStart]. Room stores MINUTE-of-day,
     * so this is converted at the mapper boundary — mixing the two is a 60x error.
     */
    val reminderTimes: List<Int>? = null,
    /**
     * Marks the four fields above as authoritative, INCLUDING their nulls (which then clear the
     * server's stored value). Always true from this client; the flag exists so a *older* build's PUT,
     * which sends none of them, can't wipe a rule it cannot represent. Backend: TaskRequest.recurrenceRuleSet.
     *
     * Consequence: whenever this is true the rule must be sent in full — a partial send resets the
     * omitted parts to their defaults.
     */
    val recurrenceRuleSet: Boolean = true,
)

@Serializable
data class SubtaskRequest(
    /** Server id of an existing step, or null for a step created on the client. */
    val remoteId: Long? = null,
    val title: String,
    val isCompleted: Boolean = false,
    val orderIndex: Int = 0,
)

@Serializable
data class TaskDailyCompletionRequest(
    val date: Long,
    val completed: Boolean,
)
