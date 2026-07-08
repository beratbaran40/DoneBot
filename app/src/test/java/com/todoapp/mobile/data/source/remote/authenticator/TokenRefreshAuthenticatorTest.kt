package com.todoapp.mobile.data.source.remote.authenticator

import com.todoapp.mobile.common.DomainException
import com.todoapp.mobile.domain.repository.AuthRepository
import com.todoapp.mobile.domain.repository.SessionPreferences
import com.todoapp.mobile.domain.repository.TaskSyncRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.sync.Mutex
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertNull
import org.junit.Test
import javax.inject.Provider

/**
 * Locks the authenticator's logout contract through the ServerUnreachable taxonomy change:
 * only a server-rejected refresh (Unauthorized) may force-logout; every transient failure shape a
 * cold-starting backend produces (timeout → ServerUnreachable, offline → NoInternet) must keep the
 * session and simply fail the original request.
 */
class TokenRefreshAuthenticatorTest {
    private val sessionPreferences = mockk<SessionPreferences> {
        coEvery { getAccessToken() } returns "access-token"
        coEvery { getRefreshToken() } returns "refresh-token"
    }
    private val authRepository = mockk<AuthRepository>()
    private val taskSyncRepository = mockk<TaskSyncRepository>(relaxed = true)

    private val authenticator = TokenRefreshAuthenticator(
        sessionPreferences = sessionPreferences,
        mutex = Mutex(),
        authRepository = authRepository,
        taskSyncRepositoryProvider = Provider { taskSyncRepository },
    )

    // A 401 whose request carries the SAME token as storage, so the idempotency shortcut doesn't
    // kick in and the authenticator actually attempts a refresh.
    private fun unauthorizedResponse(): Response {
        val request = Request.Builder()
            .url("https://example.com/tasks")
            .header("Authorization", "Bearer access-token")
            .build()
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .build()
    }

    @Test
    fun `cold-start refresh timeout keeps the session - no forceLogout`() {
        coEvery { authRepository.refresh(any()) } returns
            Result.failure(DomainException.ServerUnreachable("timeout", requestNeverReachedServer = false))

        assertNull(authenticator.authenticate(null, unauthorizedResponse()))

        coVerify(exactly = 0) { authRepository.forceLogout() }
    }

    @Test
    fun `offline refresh failure keeps the session - no forceLogout`() {
        coEvery { authRepository.refresh(any()) } returns Result.failure(DomainException.NoInternet())

        assertNull(authenticator.authenticate(null, unauthorizedResponse()))

        coVerify(exactly = 0) { authRepository.forceLogout() }
    }

    @Test
    fun `server-rejected refresh with a stored token forces logout`() {
        coEvery { authRepository.refresh(any()) } returns Result.failure(DomainException.Unauthorized())
        coEvery { authRepository.forceLogout() } returns Result.success(Unit)

        assertNull(authenticator.authenticate(null, unauthorizedResponse()))

        coVerify(exactly = 1) { authRepository.forceLogout() }
    }
}
