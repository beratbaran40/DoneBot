package com.todoapp.mobile.data.model.network.data

import kotlinx.serialization.Serializable

@Serializable
data class TaskUserData(
    val userId: Long,
    val displayName: String,
)

@Serializable
data class TaskData(
    val id: Long,
    val title: String,
    val description: String?,
    val date: Long,
    val timeStart: Long,
    val timeEnd: Long,
    val isCompleted: Boolean,
    val isSecret: Boolean,
    val assignedTo: TaskUserData? = null,
    val createdBy: TaskUserData? = null,
    val familyGroupId: Long? = null,
    val priority: String? = null,
    val category: String? = null,
    val customCategoryName: String? = null,
    val recurrence: String? = null,
    val finishedOn: Long? = null,
    val clientTaskId: String? = null,
    val isAllDay: Boolean = false,
    val reminderOffsetMinutes: Long = 0L,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val locationName: String? = null,
    val locationAddress: String? = null,
    val photoUrls: List<String> = emptyList(),
    /** Ordered steps of a staged task (empty for a plain task). Synced since backend V11. */
    val subtasks: List<SubtaskData> = emptyList(),
    /**
     * Extended recurrence rule, synced since backend V19. The defaults describe the legacy
     * "every period, forever" routine, so a response from an older server decodes correctly.
     */
    val recurrenceInterval: Int = 1,
    val recurrenceByDay: String? = null,
    val recurrenceUntil: Long? = null,
    /** Absolute reminder times as SECOND-of-day; empty = the single [reminderOffsetMinutes] reminder. */
    val reminderTimes: List<Int> = emptyList(),
)

@Serializable
data class SubtaskData(
    val id: Long,
    val title: String,
    val isCompleted: Boolean = false,
    val orderIndex: Int = 0,
)

@Serializable
data class TaskListData(
    val tasks: List<TaskData>,
    val count: Int,
)

@Serializable
data class TaskDailyCompletionData(
    val taskId: Long,
    val date: Long,
    val completedAt: Long,
)

@Serializable
data class TaskDailyCompletionListData(
    val items: List<TaskDailyCompletionData>,
    val count: Int,
)
