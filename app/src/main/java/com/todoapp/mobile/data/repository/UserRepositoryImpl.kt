package com.todoapp.mobile.data.repository

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.todoapp.mobile.common.handleEmptyRequest
import com.todoapp.mobile.common.handleRequest
import com.todoapp.mobile.data.model.network.data.AuthResponseData
import com.todoapp.mobile.data.model.network.data.FCMTokenResponseData
import com.todoapp.mobile.data.model.network.data.RefreshTokenData
import com.todoapp.mobile.data.model.network.data.UserData
import com.todoapp.mobile.data.model.network.request.ChangePasswordRequest
import com.todoapp.mobile.data.model.network.request.FCMTokenRequest
import com.todoapp.mobile.data.model.network.request.FcmTokenDeleteRequest
import com.todoapp.mobile.data.model.network.request.ForgotPasswordRequest
import com.todoapp.mobile.data.model.network.request.GoogleLoginRequest
import com.todoapp.mobile.data.model.network.request.LoginRequest
import com.todoapp.mobile.data.model.network.request.RefreshTokenRequest
import com.todoapp.mobile.data.model.network.request.RegisterRequest
import com.todoapp.mobile.data.model.network.request.ResetPasswordRequest
import com.todoapp.mobile.data.model.network.request.UpdateUserRequest
import com.todoapp.mobile.data.source.remote.api.ToDoApi
import com.todoapp.mobile.data.source.remote.api.TodoAuthApi
import com.todoapp.mobile.domain.repository.AuthEvent
import com.todoapp.mobile.domain.repository.AuthRepository
import com.todoapp.mobile.domain.repository.FCMTokenPreferences
import com.todoapp.mobile.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import timber.log.Timber
import javax.inject.Inject

class UserRepositoryImpl
@Inject
constructor(
    private val todoApi: ToDoApi,
    private val fcmTokenPreferences: FCMTokenPreferences,
    private val dataStoreHelper: DataStoreHelper,
) : UserRepository {
    @Volatile private var cachedUser: UserData? = null

    @Volatile private var userCachedAt: Long = 0L

    override suspend fun fcmToken(request: FCMTokenRequest): Result<FCMTokenResponseData> = handleRequest {
        todoApi.fcmToken(request)
    }

    override suspend fun syncPendingFcmToken(): Result<Unit> {
        var pendingToken = fcmTokenPreferences.getPendingToken()

        if (pendingToken.isNullOrBlank()) {
            pendingToken =
                try {
                    FirebaseMessaging.getInstance().token.await()
                } catch (e: IOException) {
                    Log.d("FCM_SYNC", "Failed to get Firebase token", e)
                    return Result.success(Unit)
                }
        }

        val lastSentToken = fcmTokenPreferences.getLastSentToken()
        val deviceId = fcmTokenPreferences.getDeviceId()
        val deviceName = fcmTokenPreferences.getDeviceName()

        if (pendingToken.isNullOrBlank()) return Result.success(Unit)

        if (pendingToken == lastSentToken) {
            fcmTokenPreferences.clearPendingToken()
            return Result.success(Unit)
        }

        val apiResult: Result<FCMTokenResponseData> =
            fcmToken(
                FCMTokenRequest(
                    token = pendingToken,
                    deviceId = deviceId,
                    deviceName = deviceName,
                ),
            )

        apiResult
            .onSuccess {
                fcmTokenPreferences.setLastSentToken(pendingToken)
                fcmTokenPreferences.clearPendingToken()
            }.onFailure { e ->
                Log.e("FCM_SYNC", "Token send FAILED. Pending token preserved.", e)
            }

        return apiResult.map {}
    }

    override suspend fun deleteFcmToken(): Result<Unit> {
        val tokenToDelete = fcmTokenPreferences.getLastSentToken()
        val backendResult: Result<Unit> =
            if (!tokenToDelete.isNullOrBlank()) {
                handleEmptyRequest { todoApi.deleteFcmToken(FcmTokenDeleteRequest(token = tokenToDelete)) }
                    .onFailure { Log.w("FCM_CLEANUP", "Backend DELETE failed", it) }
            } else {
                Result.success(Unit)
            }

        runCatching { FirebaseMessaging.getInstance().deleteToken().await() }
            .onFailure { Log.w("FCM_CLEANUP", "FirebaseMessaging.deleteToken failed", it) }

        fcmTokenPreferences.clearAll()
        return backendResult
    }

    override suspend fun register(request: RegisterRequest): Result<AuthResponseData> = handleRequest {
        todoApi.register(request)
    }

    override suspend fun login(request: LoginRequest): Result<AuthResponseData> = handleRequest {
        todoApi.login(
            request,
        )
    }

    override suspend fun googleLogin(token: String): Result<AuthResponseData> = handleRequest {
        todoApi.googleLogin(GoogleLoginRequest(token = token))
    }

    override suspend fun getUserInfo(): Result<UserData> {
        cachedUser?.let {
            if (System.currentTimeMillis() - userCachedAt < USER_CACHE_TTL_MS) {
                return Result.success(it)
            }
        }
        return handleRequest { todoApi.getUserInfo() }
            .onSuccess { rememberUser(it) }
    }

    override suspend fun updateDisplayName(displayName: String): Result<UserData> {
        return handleRequest { todoApi.updateUser(UpdateUserRequest(displayName = displayName)) }
            .onSuccess {
                dataStoreHelper.setUser(it)
                rememberUser(it)
            }
    }

    override suspend fun uploadAvatar(
        bytes: ByteArray,
        mimeType: String,
    ): Result<UserData> {
        val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", "avatar.jpg", body)
        return handleRequest { todoApi.uploadAvatar(part) }
            .onSuccess {
                dataStoreHelper.setUser(it)
                rememberUser(it)
                // Bump the avatar cache-bust token so the singleton top bar (and any other live
                // observer) refetches even when the backend returns the same avatar path.
                dataStoreHelper.bumpAvatarVersion()
            }
    }

    override suspend fun forgotPassword(email: String): Result<Unit> = handleEmptyRequest {
        todoApi.forgotPassword(ForgotPasswordRequest(email = email))
    }

    override suspend fun resetPassword(token: String, newPassword: String): Result<Unit> = handleEmptyRequest {
        todoApi.resetPassword(ResetPasswordRequest(token = token, newPassword = newPassword))
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> = handleEmptyRequest {
        todoApi.changePassword(
            ChangePasswordRequest(currentPassword = currentPassword, newPassword = newPassword),
        )
    }

    override suspend fun getPushEnabled(): Result<Boolean> = handleRequest { todoApi.getUserPreferences() }.map { it.pushEnabled }

    override suspend fun setPushEnabled(enabled: Boolean): Result<Boolean> = handleRequest {
        todoApi.updateUserPreferences(
            com.todoapp.mobile.data.model.network.request.UpdateUserPreferencesRequest(
                pushEnabled = enabled,
            ),
        )
    }.map { it.pushEnabled }

    override suspend fun deleteAccount(): Result<Unit> = handleEmptyRequest {
        todoApi.deleteAccount()
    }

    private fun rememberUser(user: UserData) {
        cachedUser = user
        userCachedAt = System.currentTimeMillis()
    }

    private companion object {
        const val USER_CACHE_TTL_MS = 60_000L
    }
}

class AuthRepositoryImpl
@Inject
constructor(
    private val authApi: TodoAuthApi,
) : AuthRepository {
    private val _events = MutableSharedFlow<AuthEvent>(replay = 0)
    override val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    override suspend fun logout(): Result<Unit> {
        _events.emit(AuthEvent.Logout)
        return Result.success(Unit)
    }

    override suspend fun forceLogout(): Result<Unit> {
        Timber.tag("AuthLogout").w("forceLogout emitted from AuthRepository")
        _events.emit(AuthEvent.ForceLogout)
        return Result.success(Unit)
    }

    override suspend fun refresh(request: RefreshTokenRequest): Result<RefreshTokenData> {
        return handleRequest { authApi.refreshToken(request) }
    }
}
