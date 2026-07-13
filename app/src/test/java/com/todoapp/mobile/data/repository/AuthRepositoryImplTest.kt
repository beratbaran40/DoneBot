package com.todoapp.mobile.data.repository

import com.todoapp.mobile.common.DomainException
import com.todoapp.mobile.data.model.network.data.RefreshTokenData
import com.todoapp.mobile.data.model.network.request.RefreshTokenRequest
import com.todoapp.mobile.data.model.network.response.BaseResponse
import com.todoapp.mobile.data.source.remote.api.TodoAuthApi
import com.todoapp.mobile.domain.repository.SessionPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

/**
 * Locks Fix C of the spurious-logout hardening: [AuthRepositoryImpl.refresh] must retry EXACTLY once when
 * the refresh call is rejected as Unauthorized (upstream collapses 401 & 403 to Unauthorized) before it
 * surfaces the failure — so a transient edge-403 / momentary-401 no longer lets the OkHttp authenticator
 * force a logout. A genuinely dead refresh token still fails twice and logs out; a non-Unauthorized
 * failure (timeout/5xx) is NOT retried (those already keep the session).
 */
class AuthRepositoryImplTest {
    private val authApi = mockk<TodoAuthApi>()
    private val sessionPreferences = mockk<SessionPreferences>(relaxed = true)

    private val repository = AuthRepositoryImpl(
        authApi = authApi,
        sessionPreferences = sessionPreferences,
    )

    private val request = RefreshTokenRequest("stored-refresh-token")

    @Test
    fun `transient Unauthorized then success - session survives on the retry`() = runTest {
        coEvery { authApi.refreshToken(any()) } returnsMany listOf(errorResponse(403), successResponse())

        val result = repository.refresh(request)

        assertTrue(result.isSuccess)
        assertEquals("newAccess", result.getOrNull()?.accessToken)
        coVerify(exactly = 2) { authApi.refreshToken(any()) }
    }

    @Test
    fun `Unauthorized twice - propagates Unauthorized so the authenticator can force logout`() = runTest {
        coEvery { authApi.refreshToken(any()) } returnsMany listOf(errorResponse(401), errorResponse(401))

        val result = repository.refresh(request)

        assertTrue(result.exceptionOrNull() is DomainException.Unauthorized)
        coVerify(exactly = 2) { authApi.refreshToken(any()) }
    }

    @Test
    fun `success on first try - no retry`() = runTest {
        coEvery { authApi.refreshToken(any()) } returns successResponse()

        val result = repository.refresh(request)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { authApi.refreshToken(any()) }
    }

    @Test
    fun `non-Unauthorized failure is not retried`() = runTest {
        coEvery { authApi.refreshToken(any()) } returns errorResponse(503)

        val result = repository.refresh(request)

        assertTrue(result.exceptionOrNull() is DomainException.ServerUnreachable)
        coVerify(exactly = 1) { authApi.refreshToken(any()) }
    }

    private fun successResponse(): Response<BaseResponse<RefreshTokenData?>> = Response.success(
        BaseResponse<RefreshTokenData?>(
            code = 200,
            message = "ok",
            data = RefreshTokenData(accessToken = "newAccess", refreshToken = "newRefresh", expiresIn = 3600L),
        ),
    )

    private fun errorResponse(code: Int): Response<BaseResponse<RefreshTokenData?>> = Response.error(code, "{}".toResponseBody("application/json".toMediaTypeOrNull()))
}
