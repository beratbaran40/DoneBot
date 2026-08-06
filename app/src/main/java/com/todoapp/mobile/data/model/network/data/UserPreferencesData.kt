package com.todoapp.mobile.data.model.network.data

import kotlinx.serialization.Serializable

@Serializable
data class UserPreferencesData(
    val pushEnabled: Boolean,
    /**
     * NotificationType names the user has muted. Defaulted so a client that reaches a backend
     * predating per-type preferences still parses the response.
     */
    val disabledTypes: Set<String> = emptySet(),
)
