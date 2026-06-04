package com.todoapp.mobile.data.source.remote.authenticator

import com.todoapp.mobile.common.DomainException
import com.todoapp.mobile.data.model.network.request.RefreshTokenRequest
import com.todoapp.mobile.domain.repository.AuthRepository
import com.todoapp.mobile.domain.repository.SessionPreferences
import com.todoapp.mobile.domain.repository.TaskSyncRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider

class TokenRefreshAuthenticator
@Inject
constructor(
    private val sessionPreferences: SessionPreferences,
    private val mutex: Mutex,
    private val authRepository: AuthRepository,
    private val taskSyncRepositoryProvider: Provider<TaskSyncRepository>,
) : Authenticator {
    override fun authenticate(
        route: Route?,
        response: Response,
    ): Request? {
        @Suppress("DEPRECATION")
        val tid = Thread.currentThread().id
        val urlPath = response.request.url.encodedPath
        val rc = responseCount(response)
        if (rc >= 2) {
            Timber.tag("AuthLogout").w("authenticate[t$tid] giving up: responseCount=$rc url=$urlPath")
            return null
        }
        return runBlocking {
            mutex.withLock {
                val currentAccessToken = sessionPreferences.getAccessToken()
                val requestAccessToken = response.request.header("Authorization")?.removePrefix("Bearer ")
                Timber.tag("AuthLogout").d(
                    "authenticate[t$tid] entered url=$urlPath rc=$rc " +
                        "reqTok=${requestAccessToken.shortHash()} storedTok=${currentAccessToken.shortHash()}",
                )
                if (!currentAccessToken.isNullOrBlank() && currentAccessToken != requestAccessToken) {
                    Timber.tag("AuthLogout").d(
                        "authenticate[t$tid] idempotency hit: stored token differs from request, retrying without refresh",
                    )
                    return@runBlocking response.request
                        .newBuilder()
                        .header("Authorization", "Bearer $currentAccessToken")
                        .build()
                }

                val storedRefreshToken = sessionPreferences.getRefreshToken()
                if (storedRefreshToken.isNullOrBlank()) {
                    Timber.tag("AuthLogout").w(
                        "authenticate[t$tid] no refresh token in storage; giving up without forceLogout",
                    )
                    return@runBlocking null
                }

                Timber.tag("AuthLogout").d(
                    "authenticate[t$tid] calling refresh with storedRefreshToken=${storedRefreshToken.shortHash()}",
                )
                val refreshed =
                    authRepository.refresh(
                        RefreshTokenRequest(storedRefreshToken),
                    )
                if (refreshed.isSuccess) {
                    val pair = refreshed.getOrThrow()
                    Timber.tag("AuthLogout").d(
                        "authenticate[t$tid] refresh OK newAccess=${pair.accessToken.shortHash()} " +
                            "newRefresh=${pair.refreshToken.shortHash()}",
                    )
                    sessionPreferences.setAccessToken(pair.accessToken)
                    sessionPreferences.setRefreshToken(pair.refreshToken)
                    sessionPreferences.setExpiresAt(pair.expiresIn)
                    runCatching { taskSyncRepositoryProvider.get().syncPendingTasks() }
                        .onFailure { Timber.tag("AuthLogout").w(it, "authenticate[t$tid] syncPendingTasks kick failed") }
                    response.request
                        .newBuilder()
                        .header("Authorization", "Bearer ${pair.accessToken}")
                        .build()
                } else {
                    val cause = refreshed.exceptionOrNull()
                    if (cause is DomainException.Unauthorized && !sessionPreferences.getRefreshToken().isNullOrBlank()) {
                        Timber.tag("AuthLogout").w(
                            cause,
                            "authenticate[t$tid] refresh rejected by server (Unauthorized); calling forceLogout",
                        )
                        authRepository.forceLogout()
                    } else {
                        Timber.tag("AuthLogout").w(
                            cause,
                            "authenticate[t$tid] transient refresh failure (cause=${cause?.javaClass?.simpleName}); keeping session",
                        )
                    }
                    null
                }
            }
        }
    }

    private fun String?.shortHash(): String = if (this.isNullOrBlank()) "<null>" else "${take(4)}…${takeLast(4)}"

    private fun responseCount(response: Response): Int {
        var r: Response? = response
        var count = 1
        while (r?.priorResponse != null) {
            count++
            r = r.priorResponse
        }
        return count
    }
}
