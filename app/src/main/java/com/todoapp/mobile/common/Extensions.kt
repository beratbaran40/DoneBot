package com.todoapp.mobile.common

import android.database.sqlite.SQLiteException
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.todoapp.mobile.data.model.network.response.BaseResponse
import com.todoapp.mobile.data.model.network.response.ErrorResponse
import com.todoapp.mobile.domain.engine.PomodoroMode
import com.todoapp.mobile.ui.pomodoro.ModeColorKey
import com.todoapp.mobile.ui.pomodoro.PomodoroModeUi
import com.todoapp.mobile.ui.pomodoro.PomodoroModeUiPreset
import com.todoapp.uikit.theme.TDTheme
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import retrofit2.Response
import timber.log.Timber
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.UnknownHostException

fun <T> MutableList<T>.move(
    fromIndex: Int,
    toIndex: Int,
) {
    if (fromIndex == toIndex) return
    val item = removeAt(fromIndex)
    add(toIndex, item)
}

fun String.maskTitle(): String = this.first() + "*".repeat(this.length - 1)

fun String.maskDescription(): String {
    if (length <= 3) return this
    return take(3) + "*".repeat(length - 3)
}

suspend fun <T> handleRequest(request: suspend () -> Response<BaseResponse<T?>>): Result<T> {
    return try {
        val response = request()

        if (response.isSuccessful.not()) {
            val errorBody = response.errorBody()?.string()
            Timber.tag("HttpError").d(errorBody.toString())
            val parsed =
                errorBody?.let {
                    runCatching { Json.decodeFromString<ErrorResponse>(it) }.getOrNull()
                }
            val message = parsed?.message ?: response.message() ?: "Something went wrong"
            Timber.tag("HttpError").d(message)
            parsed?.errorCode?.toOAuthAccountException()?.let { return Result.failure(it) }
            return when (response.code()) {
                404 -> Result.failure(DomainException.NotFound())
                401, 403 -> Result.failure(DomainException.Unauthorized())
                // A 5xx whose body is not our JSON envelope comes from an intermediary (Render's edge
                // during a cold start / deploy window) — Spring never saw the request, so retrying can't
                // double-process it. The backend's own 503s (e.g. vertex_unavailable) DO parse and must
                // stay Server so message-marker matching keeps working.
                502, 503, 504 ->
                    if (parsed == null) {
                        Result.failure(DomainException.ServerUnreachable(message, requestNeverReachedServer = true))
                    } else {
                        Result.failure(DomainException.Server(message))
                    }
                else -> Result.failure(DomainException.Server(message))
            }
        }

        val body =
            response.body()
                ?: return Result.failure(DomainException.Server("Empty response"))

        val data =
            body.data
                ?: return Result.failure(DomainException.Server(body.message))

        Result.success(data)
    } catch (t: Throwable) {
        if (t is CancellationException) throw t
        Timber.tag("HttpError").w(t, "Original exception: ${t.javaClass.simpleName}: ${t.message}")
        Result.failure(DomainException.fromThrowable(t))
    }
}

// For endpoints whose successful response carries no payload (DELETE, mark-read, etc.).
// `handleRequest` treats a null `data` as failure, which breaks these — the server returns
// 2xx with `data: null`, the call surfaces as Result.failure, and the local entity never
// gets removed. Use this helper instead so any 2xx is success regardless of body.
suspend fun handleEmptyRequest(request: suspend () -> Response<BaseResponse<Unit?>>): Result<Unit> {
    return try {
        val response = request()
        if (response.isSuccessful) return Result.success(Unit)
        val errorBody = response.errorBody()?.string()
        Timber.tag("HttpError").d(errorBody.toString())
        val parsed =
            errorBody?.let {
                runCatching { Json.decodeFromString<ErrorResponse>(it) }.getOrNull()
            }
        val message = parsed?.message ?: response.message() ?: "Something went wrong"
        Timber.tag("HttpError").d(message)
        when (response.code()) {
            404 -> Result.failure(DomainException.NotFound())
            401, 403 -> Result.failure(DomainException.Unauthorized())
            // Same edge-vs-backend split as handleRequest: non-envelope 5xx = intermediary, safe to retry.
            502, 503, 504 ->
                if (parsed == null) {
                    Result.failure(DomainException.ServerUnreachable(message, requestNeverReachedServer = true))
                } else {
                    Result.failure(DomainException.Server(message))
                }
            else -> Result.failure(DomainException.Server(message))
        }
    } catch (t: Throwable) {
        if (t is CancellationException) throw t
        Timber.tag("HttpError").w(t, "Original exception: ${t.javaClass.simpleName}: ${t.message}")
        Result.failure(DomainException.fromThrowable(t))
    }
}

sealed class DomainException(
    message: String,
) : Exception(message) {
    class NoInternet : DomainException("No internet connection")

    class Unauthorized : DomainException("Unauthorized")

    // A PUT/DELETE landed on a resource the server no longer has (404). Distinct from Server so the push
    // pipeline can tombstone it (drop the local row) instead of retrying a permanently-gone row forever.
    class NotFound : DomainException("Not found")

    class Server(
        message: String,
    ) : DomainException(message)

    // The server didn't produce a usable response while the device's own connectivity is (as far as
    // we can tell) fine: TCP refusal, a timeout, or a 5xx minted by an intermediary (Render's edge
    // while the instance wakes or a deploy swaps containers). Distinct from NoInternet (device
    // offline) and Server (the backend answered with an error of its own).
    class ServerUnreachable(
        message: String,
        // true only when the request provably never reached the backend (connect refused, or an edge
        // 502/503/504 whose body isn't our JSON envelope) — the only shapes safe to auto-retry.
        // Timeouts are false: the server may have finished processing after the client gave up waiting,
        // so an automatic resend could double-run whatever the request did.
        val requestNeverReachedServer: Boolean,
    ) : DomainException(message)

    // The email belongs to an existing account that only signs in via a social provider
    // (no password). `provider` is "google"/null. Routed by errorCode, not HTTP status.
    class OAuthAccountExists(
        val provider: String?,
    ) : DomainException("Account uses social sign-in")

    class Database(
        message: String,
    ) : DomainException(message)

    class Unknown(
        cause: Throwable,
    ) : DomainException(cause.message ?: "Unknown error")

    companion object {
        private const val HTTP_STATUS_UNAUTHORIZED = 401
        private const val HTTP_STATUS_FORBIDDEN = 403
        private const val HTTP_STATUS_NOT_FOUND = 404
        private const val HTTP_STATUS_BAD_GATEWAY = 502
        private const val HTTP_STATUS_SERVICE_UNAVAILABLE = 503
        private const val HTTP_STATUS_GATEWAY_TIMEOUT = 504

        fun fromThrowable(t: Throwable): DomainException = when (t) {
            // DNS resolution failed — overwhelmingly "device offline", not "backend down".
            is UnknownHostException -> NoInternet()

            // TCP connect refused/failed: the request never left for the server, so it's safe to auto-retry.
            is ConnectException -> ServerUnreachable(t.message ?: "Cannot reach server", requestNeverReachedServer = true)

            // Covers SocketTimeoutException (a subclass) and OkHttp's callTimeout InterruptedIOException.
            // OkHttp throws the same SocketTimeoutException type for connect-, read- and TLS-phase
            // timeouts and its message differs by transport/JDK, so no timeout is ever treated as
            // "provably unprocessed" — the server may have finished the work after we stopped waiting.
            is InterruptedIOException -> ServerUnreachable(t.message ?: "Server is not responding", requestNeverReachedServer = false)

            is HttpException -> when (t.code()) {
                HTTP_STATUS_UNAUTHORIZED, HTTP_STATUS_FORBIDDEN -> Unauthorized()
                HTTP_STATUS_NOT_FOUND -> NotFound()
                // Near-dead path (endpoints return Response<T>, so non-2xx doesn't throw). Unlike
                // handleRequest we can't inspect the body here to prove the 5xx came from the edge
                // rather than the backend, so never mark it safe to auto-retry.
                HTTP_STATUS_BAD_GATEWAY, HTTP_STATUS_SERVICE_UNAVAILABLE, HTTP_STATUS_GATEWAY_TIMEOUT ->
                    ServerUnreachable("Server unavailable", requestNeverReachedServer = false)
                else -> Server("Server error")
            }

            is SQLiteException -> Database(t.message ?: "Database error")
            else -> Unknown(t)
        }
    }
}

// Maps a backend errorCode like "oauth_account_google" to the typed exception the login
// ViewModel branches on. Returns null for any non-oauth code.
private fun String.toOAuthAccountException(): DomainException.OAuthAccountExists? = if (startsWith("oauth_account")) {
    DomainException.OAuthAccountExists(
        removePrefix("oauth_account").trim('_').ifBlank { null },
    )
} else {
    null
}

@Composable
fun PomodoroModeUi.resolveTextColor(): Color = when (colorKey) {
    ModeColorKey.Focus -> TDTheme.colors.pendingGray
    ModeColorKey.ShortBreak -> TDTheme.colors.softPink
    ModeColorKey.LongBreak -> TDTheme.colors.green
    ModeColorKey.OverTime -> TDTheme.colors.red
}

fun PomodoroMode.toUiMode(): PomodoroModeUi = when (this) {
    PomodoroMode.Focus -> PomodoroModeUiPreset.Focus.value
    PomodoroMode.ShortBreak -> PomodoroModeUiPreset.ShortBreak.value
    PomodoroMode.LongBreak -> PomodoroModeUiPreset.LongBreak.value
    PomodoroMode.OverTime -> PomodoroModeUiPreset.OverTime.value
}

fun <T> ArrayDeque<T>.pollFirst(): T? = if (isEmpty()) null else removeFirst()
