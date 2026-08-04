package com.todoapp.mobile.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

@Composable
internal fun ChatErrorBanner(
    error: ChatContract.ChatError,
    lastFailedPrompt: String?,
    cooldownSecondsRemaining: Int,
    autoRetrySecondsRemaining: Int,
    onAction: (ChatContract.UiAction) -> Unit,
) {
    val message = if (error == ChatContract.ChatError.SERVER_WAKING && autoRetrySecondsRemaining > 0) {
        // An auto-resend is scheduled — show the live countdown instead of the static message.
        stringResource(R.string.chat_error_server_waking_retrying_format, autoRetrySecondsRemaining)
    } else {
        stringResource(
            when (error) {
                ChatContract.ChatError.GENERIC -> R.string.chat_error_generic
                ChatContract.ChatError.BLOCKED -> R.string.chat_error_blocked
                ChatContract.ChatError.OFFLINE -> R.string.chat_error_offline
                ChatContract.ChatError.LOOP_OVERFLOW -> R.string.chat_loop_overflow
                ChatContract.ChatError.RATE_LIMITED -> R.string.chat_error_rate_limited
                ChatContract.ChatError.NOT_AUTHENTICATED -> R.string.chat_error_guest_limited
                ChatContract.ChatError.SERVER_UNAVAILABLE -> R.string.chat_error_server_unavailable
                ChatContract.ChatError.SERVER_WAKING -> R.string.chat_error_server_waking
            },
        )
    }
    // SERVER_WAKING is a transient "hang tight" state, not a failure the user caused — warn in
    // yellow/orange instead of the red used by every real error.
    val containerColor: Color
    val contentColor: Color
    if (error == ChatContract.ChatError.SERVER_WAKING) {
        containerColor = TDTheme.colors.lightYellow
        contentColor = TDTheme.colors.orange
    } else {
        containerColor = TDTheme.colors.lightRed
        contentColor = TDTheme.colors.crossRed
    }
    val canRetry = lastFailedPrompt != null && error != ChatContract.ChatError.BLOCKED
    val retryDisabledByCooldown = error == ChatContract.ChatError.RATE_LIMITED &&
        cooldownSecondsRemaining > 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(TDTheme.shapes.medium)
            .background(containerColor)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TDText(
            text = message,
            color = contentColor,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        if (canRetry) {
            val retryLabel = if (retryDisabledByCooldown) {
                stringResource(R.string.chat_error_rate_limited_cooldown_format, cooldownSecondsRemaining)
            } else {
                stringResource(R.string.chat_retry)
            }
            TextButton(
                onClick = { onAction(ChatContract.UiAction.OnRetry) },
                enabled = !retryDisabledByCooldown,
            ) {
                TDText(
                    text = retryLabel,
                    color = if (retryDisabledByCooldown) TDTheme.colors.gray else contentColor,
                    style = TDTheme.typography.subheading1,
                )
            }
        }
        if (error == ChatContract.ChatError.NOT_AUTHENTICATED) {
            TextButton(onClick = { onAction(ChatContract.UiAction.OnSignInTap) }) {
                TDText(
                    text = stringResource(R.string.chat_sign_in),
                    color = contentColor,
                    style = TDTheme.typography.subheading1,
                )
            }
        }
        TextButton(onClick = { onAction(ChatContract.UiAction.OnDismissError) }) {
            TDText(
                text = stringResource(R.string.chat_dismiss),
                color = contentColor,
                style = TDTheme.typography.subheading1,
            )
        }
    }
}

@TDPreview
@Composable
private fun ChatErrorBannerOfflinePreview() {
    TDTheme {
        ChatErrorBanner(
            error = ChatContract.ChatError.OFFLINE,
            lastFailedPrompt = "What's on my list today?",
            cooldownSecondsRemaining = 0,
            autoRetrySecondsRemaining = 0,
            onAction = {},
        )
    }
}

@TDPreview
@Composable
private fun ChatErrorBannerServerWakingPreview() {
    TDTheme {
        ChatErrorBanner(
            error = ChatContract.ChatError.SERVER_WAKING,
            lastFailedPrompt = "What's on my list today?",
            cooldownSecondsRemaining = 0,
            autoRetrySecondsRemaining = 0,
            onAction = {},
        )
    }
}

@TDPreview
@Composable
private fun ChatErrorBannerServerWakingRetryingPreview() {
    TDTheme {
        ChatErrorBanner(
            error = ChatContract.ChatError.SERVER_WAKING,
            lastFailedPrompt = "What's on my list today?",
            cooldownSecondsRemaining = 0,
            autoRetrySecondsRemaining = 12,
            onAction = {},
        )
    }
}

@TDPreview
@Composable
private fun ChatErrorBannerRateLimitedCooldownPreview() {
    TDTheme {
        ChatErrorBanner(
            error = ChatContract.ChatError.RATE_LIMITED,
            lastFailedPrompt = "What's on my list today?",
            cooldownSecondsRemaining = 12,
            autoRetrySecondsRemaining = 0,
            onAction = {},
        )
    }
}

@TDPreview
@Composable
private fun ChatErrorBannerGuestPreview() {
    TDTheme {
        ChatErrorBanner(
            error = ChatContract.ChatError.NOT_AUTHENTICATED,
            lastFailedPrompt = null,
            cooldownSecondsRemaining = 0,
            autoRetrySecondsRemaining = 0,
            onAction = {},
        )
    }
}
