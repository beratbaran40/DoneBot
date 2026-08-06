package com.todoapp.mobile.data.model.network.request

import kotlinx.serialization.Serializable

@Serializable
data class UpdateUserPreferencesRequest(
    val pushEnabled: Boolean,
    /** Null leaves the stored per-type choices untouched; a set replaces them wholesale. */
    val disabledTypes: Set<String>? = null,
)
