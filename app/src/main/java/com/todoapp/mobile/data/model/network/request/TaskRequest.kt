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
