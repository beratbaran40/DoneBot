package com.todoapp.mobile.common

import android.database.sqlite.SQLiteException
import android.util.Log
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
import java.net.SocketTimeoutException
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
            Log.d("error", errorBody.toString())
            val parsed =
                errorBody?.let {
                    runCatching { Json.decodeFromString<ErrorResponse>(it) }.getOrNull()
                }
            val message = parsed?.message ?: response.message() ?: "Something went wrong"
            Log.d("error", message)
            parsed?.errorCode?.toOAuthAccountException()?.let { return Result.failure(it) }
            return when (response.code()) {
                404 -> Result.failure(DomainException.NotFound())
                401, 403 -> Result.failure(DomainException.Unauthorized())
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
        Log.e("HANDLE_REQUEST", "Original exception: ${t.javaClass.simpleName}: ${t.message}", t)
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
        Log.d("error", errorBody.toString())
        val message = errorBody
            ?.let { runCatching { Json.decodeFromString<ErrorResponse>(it).message }.getOrNull() }
            ?: response.message()
            ?: "Something went wrong"
        Log.d("error", message)
        when (response.code()) {
            404 -> Result.failure(DomainException.NotFound())
            401, 403 -> Result.failure(DomainException.Unauthorized())
            else -> Result.failure(DomainException.Server(message))
        }
    } catch (t: Throwable) {
        if (t is CancellationException) throw t
        Log.e("HANDLE_REQUEST", "Original exception: ${t.javaClass.simpleName}: ${t.message}", t)
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

        fun fromThrowable(t: Throwable): DomainException = when (t) {
            is UnknownHostException,
            is SocketTimeoutException,
            -> NoInternet()

            is HttpException -> when (t.code()) {
                HTTP_STATUS_UNAUTHORIZED, HTTP_STATUS_FORBIDDEN -> Unauthorized()
                HTTP_STATUS_NOT_FOUND -> NotFound()
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
