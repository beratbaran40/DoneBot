package com.todoapp.mobile.common

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
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
    fun `5xx maps to Server so workers keep retrying transient failures`() {
        assertTrue(DomainException.fromThrowable(httpException(500)) is DomainException.Server)
    }

    @Test
    fun `unknown host and socket timeout map to NoInternet`() {
        assertTrue(DomainException.fromThrowable(UnknownHostException()) is DomainException.NoInternet)
        assertTrue(DomainException.fromThrowable(SocketTimeoutException()) is DomainException.NoInternet)
    }
}
