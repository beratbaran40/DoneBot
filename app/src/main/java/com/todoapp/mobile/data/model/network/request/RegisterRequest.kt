package com.todoapp.mobile.data.model.network.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    @SerialName("email") val email: String,
    @SerialName("password") val password: String,
    @SerialName("displayName") val displayName: String,
)

@Serializable
data class LoginRequest(
    @SerialName("email") val email: String,
    @SerialName("password") val password: String,
)

@Serializable
data class GoogleLoginRequest(
    @SerialName("token")
    val token: String,
)

@Serializable
data class FCMTokenRequest(
    @SerialName("token")
    val token: String,
    @SerialName("deviceId")
    val deviceId: String?,
    @SerialName("deviceName")
    val deviceName: String?,
    /**
     * The device's IANA zone. The backend's due-soon job compares group-task due times in it; without
     * one it falls back to a single configured zone, which fires the reminder off by the user's own
     * UTC offset. Sent on every token sync so it follows the user when they travel.
     */
    @SerialName("timeZone")
    val timeZone: String? = null,
)
