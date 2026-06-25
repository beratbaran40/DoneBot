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

    suspend fun getPushEnabled(): Result<Boolean>

    suspend fun setPushEnabled(enabled: Boolean): Result<Boolean>

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
