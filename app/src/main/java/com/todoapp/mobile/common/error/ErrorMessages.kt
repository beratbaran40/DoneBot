package com.todoapp.mobile.common.error

import android.content.Context
import androidx.annotation.StringRes
import com.todoapp.mobile.R
import com.todoapp.mobile.common.DomainException
import java.io.IOException

/**
 * Maps a failure into a user-facing, localized message.
 *
 * The data layer already funnels every failure through [DomainException] (see
 * `handleRequest` / `DomainException.fromThrowable` in `common/Extensions.kt`), so ViewModels
 * only need to translate those typed cases into a string the user can act on — never the raw
 * `throwable.message` ("HTTP 500", "UnknownHostException"), which is meaningless to the user.
 *
 * `OAuthAccountExists` is intentionally NOT mapped here: the login flow branches on it directly
 * (it drives a "this email uses Google sign-in" UX, not a generic error).
 */
@StringRes
fun Throwable.toUserMessageRes(): Int = when (this) {
    is DomainException.NoInternet -> R.string.error_no_internet
    is DomainException.Unauthorized -> R.string.error_session_expired
    is DomainException.NotFound -> R.string.error_not_found
    is DomainException.Server -> R.string.error_server_busy
    is DomainException.Database -> R.string.error_generic
    // Raw network failure that reached the UI before the data layer wrapped it in a DomainException.
    is IOException -> R.string.error_no_internet
    else -> R.string.error_generic
}

/** Convenience: resolve [toUserMessageRes] against a context (use `@ApplicationContext`). */
fun Throwable.toUserMessage(context: Context): String = context.getString(toUserMessageRes())
