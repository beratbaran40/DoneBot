package com.todoapp.mobile.domain.model

import androidx.compose.runtime.Immutable
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
)
