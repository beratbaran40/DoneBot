package com.todoapp.mobile.common

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class DomainExceptionTest {
    private fun httpException(code: Int): HttpException = HttpException(
        Response.error<Any>(code, "err".toResponseBody("application/json".toMediaTypeOrNull())),
    )

    @Test
    fun `404 maps to NotFound so the push pipeline can tombstone`() {
        assertTrue(DomainException.fromThrowable(httpException(404)) is DomainException.NotFound)
    }

    @Test
    fun `401 maps to Unauthorized`() {
        assertTrue(DomainException.fromThrowable(httpException(401)) is DomainException.Unauthorized)
    }

    @Test
    fun `403 maps to Unauthorized to match the handleRequest convention`() {
        assertTrue(DomainException.fromThrowable(httpException(403)) is DomainException.Unauthorized)
    }

    @Test
    fun `500 maps to Server so workers keep retrying transient failures`() {
        assertTrue(DomainException.fromThrowable(httpException(500)) is DomainException.Server)
    }

    @Test
    fun `502 and 503 map to ServerUnreachable but are never marked safe to auto-retry`() {
        // Without the body we can't prove the 5xx came from the edge rather than the backend.
        listOf(502, 503, 504).forEach { code ->
            val mapped = DomainException.fromThrowable(httpException(code))
            assertTrue("code $code", mapped is DomainException.ServerUnreachable)
            assertFalse("code $code", (mapped as DomainException.ServerUnreachable).requestNeverReachedServer)
        }
    }

    @Test
    fun `unknown host maps to NoInternet - DNS failure means the device is offline`() {
        assertTrue(DomainException.fromThrowable(UnknownHostException()) is DomainException.NoInternet)
    }

    @Test
    fun `socket timeout maps to ServerUnreachable, not safe to auto-retry`() {
        // The server may have finished processing after the client stopped waiting — an automatic
        // resend could double-run whatever the request did (chat tool writes, task creation).
        val mapped = DomainException.fromThrowable(SocketTimeoutException("timeout"))
        assertTrue(mapped is DomainException.ServerUnreachable)
        assertFalse((mapped as DomainException.ServerUnreachable).requestNeverReachedServer)
    }

    @Test
    fun `callTimeout InterruptedIOException maps to ServerUnreachable, not safe to auto-retry`() {
        val mapped = DomainException.fromThrowable(InterruptedIOException("timeout"))
        assertTrue(mapped is DomainException.ServerUnreachable)
        assertFalse((mapped as DomainException.ServerUnreachable).requestNeverReachedServer)
    }

    @Test
    fun `connect refusal maps to ServerUnreachable and IS safe to auto-retry`() {
        // TCP connect never completed, so the request provably never reached the backend.
        val mapped = DomainException.fromThrowable(ConnectException("Connection refused"))
        assertTrue(mapped is DomainException.ServerUnreachable)
        assertTrue((mapped as DomainException.ServerUnreachable).requestNeverReachedServer)
    }
}
