package com.todoapp.mobile.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class GroupActivity(
    val id: Long,
    val type: String,
    val actorName: String,
    val actorAvatarUrl: String?,
    val description: String,
    val timestamp: Long,
    val taskTitle: String?,
    val targetName: String?,
)
