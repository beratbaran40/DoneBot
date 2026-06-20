package com.todoapp.mobile.ui.common.components

/** Neutral member view model for the shared assignee picker — fed by both GroupDetail and the hub. */
data class AssigneeUi(
    val userId: Long,
    val displayName: String,
    val avatarUrl: String?,
    val initials: String,
)
