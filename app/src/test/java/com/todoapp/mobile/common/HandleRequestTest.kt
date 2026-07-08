package com.todoapp.mobile.common

import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

/**
 * Locks the edge-vs-backend split on 5xx responses: a 502/503/504 whose body is NOT our JSON
 * envelope comes from an intermediary (Render's edge during a cold start / deploy window) and maps
 * to [DomainException.ServerUnreachable] with requestNeverReachedServer=true; the backend's own
 * enveloped 503s (e.g. the vertex_unavailable marker) must keep mapping to [DomainException.Server]
 * so ChatViewModel's message-marker matching keeps working.
 */
class HandleRequestTest {
    private val htmlBody = "<html><body>Bad Gateway</body></html>"
    private val vertexUnavailableEnvelope =
        """{"code":503,"message":"[vertex_unavailable] AI is temporarily unavailable","data":null}"""

    private fun errorBody(content: String) = content.toResponseBody("text/html".toMediaTypeOrNull())

    @Test
    fun `edge 502 with non-envelope body maps to ServerUnreachable safe to auto-retry`() = runTest {
        val result = handleRequest<String> { Response.error(502, errorBody(htmlBody)) }
        val error = result.exceptionOrNull()
        assertTrue(error is DomainException.ServerUnreachable)
        assertTrue((error as DomainException.ServerUnreachable).requestNeverReachedServer)
    }

    @Test
    fun `edge 503 with empty body maps to ServerUnreachable`() = runTest {
        val result = handleRequest<String> { Response.error(503, errorBody("")) }
        assertTrue(result.exceptionOrNull() is DomainException.ServerUnreachable)
    }

    @Test
    fun `backend 503 with vertex_unavailable envelope stays Server and keeps the marker`() = runTest {
        val result = handleRequest<String> { Response.error(503, errorBody(vertexUnavailableEnvelope)) }
        val error = result.exceptionOrNull()
        assertTrue(error is DomainException.Server)
        assertTrue(error!!.message!!.contains("vertex_unavailable"))
    }

    @Test
    fun `500 with any body stays Server regardless of envelope`() = runTest {
        val result = handleRequest<String> { Response.error(500, errorBody(htmlBody)) }
        assertTrue(result.exceptionOrNull() is DomainException.Server)
    }

    @Test
    fun `404 and 401 keep their dedicated types`() = runTest {
        assertTrue(
            handleRequest<String> { Response.error(404, errorBody(htmlBody)) }
                .exceptionOrNull() is DomainException.NotFound,
        )
        assertTrue(
            handleRequest<String> { Response.error(401, errorBody(htmlBody)) }
                .exceptionOrNull() is DomainException.Unauthorized,
        )
    }

    @Test
    fun `handleEmptyRequest applies the same edge-vs-backend split`() = runTest {
        val edge = handleEmptyRequest { Response.error(502, errorBody(htmlBody)) }
        assertTrue(edge.exceptionOrNull() is DomainException.ServerUnreachable)

        val backend = handleEmptyRequest { Response.error(503, errorBody(vertexUnavailableEnvelope)) }
        val backendError = backend.exceptionOrNull()
        assertTrue(backendError is DomainException.Server)
        assertEquals("[vertex_unavailable] AI is temporarily unavailable", backendError!!.message)
    }

    @Test
    fun `handleEmptyRequest still treats any 2xx as success`() = runTest {
        val result = handleEmptyRequest { Response.success(null) }
        assertTrue(result.isSuccess)
    }
}
