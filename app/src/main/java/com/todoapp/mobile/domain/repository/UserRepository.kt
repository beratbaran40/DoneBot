package com.todoapp.mobile.domain.repository

import com.todoapp.mobile.data.model.network.data.AuthResponseData
import com.todoapp.mobile.data.model.network.data.FCMTokenResponseData
import com.todoapp.mobile.data.model.network.data.RefreshTokenData
import com.todoapp.mobile.data.model.network.data.UserData
import com.todoapp.mobile.data.model.network.request.FCMTokenRequest
import com.todoapp.mobile.data.model.network.request.LoginRequest
import com.todoapp.mobile.data.model.network.request.RefreshTokenRequest
import com.todoapp.mobile.data.model.network.request.RegisterRequest
import kotlinx.coroutines.flow.SharedFlow

/**
 * The user's server-side push settings: the master switch, plus the NotificationType names they have
 * muted individually. A muted type still writes its in-app inbox row — muting silences the
 * interruption, not the record.
 */
data class PushPreferences(
    val enabled: Boolean,
    val mutedTypes: Set<String> = emptySet(),
)

interface UserRepository {
    suspend fun fcmToken(request: FCMTokenRequest): Result<FCMTokenResponseData>

    suspend fun syncPendingFcmToken(): Result<Unit>

    suspend fun deleteFcmToken(): Result<Unit>

    suspend fun register(request: RegisterRequest): Result<AuthResponseData>

    suspend fun login(request: LoginRequest): Result<AuthResponseData>

    suspend fun googleLogin(token: String): Result<AuthResponseData>

    suspend fun getUserInfo(): Result<UserData>

    suspend fun updateDisplayName(displayName: String): Result<UserData>

    suspend fun uploadAvatar(
        bytes: ByteArray,
        mimeType: String,
    ): Result<UserData>

    suspend fun forgotPassword(email: String): Result<Unit>

    suspend fun resetPassword(token: String, newPassword: String): Result<Unit>

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>

    suspend fun getPushPreferences(): Result<PushPreferences>

    /** [mutedTypes] null = leave the stored per-type choices alone (only the master switch moved). */
    suspend fun setPushPreferences(enabled: Boolean, mutedTypes: Set<String>? = null): Result<PushPreferences>

    suspend fun deleteAccount(): Result<Unit>

    suspend fun exportData(): Result<String>
}

interface AuthRepository {
    val events: SharedFlow<AuthEvent>

    suspend fun refresh(request: RefreshTokenRequest): Result<RefreshTokenData>

    suspend fun logout(): Result<Unit>

    suspend fun forceLogout(): Result<Unit>
}

sealed interface AuthEvent {
    data object Logout : AuthEvent

    data object ForceLogout : AuthEvent
}
