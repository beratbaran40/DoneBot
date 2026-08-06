package com.todoapp.mobile.ui.chat

import androidx.compose.runtime.Immutable
import com.todoapp.mobile.domain.model.ChatMessage

object ChatContract {
    sealed interface UiState {
        data object Loading : UiState

        @Immutable
        data class Ready(
            val messages: List<ChatMessage> = emptyList(),
            val draft: String = "",
            val isThinking: Boolean = false,
            val error: ChatError? = null,
            val lastFailedPrompt: String? = null,
            val rateLimitCooldownSecondsRemaining: Int = 0,
            // Seconds until the next automatic resend of lastFailedPrompt (SERVER_WAKING only;
            // 0 = no auto-retry pending). Mutually exclusive with the rate-limit cooldown.
            val autoRetrySecondsRemaining: Int = 0,
        ) : UiState
    }

    enum class ChatError {
        GENERIC,
        BLOCKED,
        OFFLINE,
        LOOP_OVERFLOW,
        RATE_LIMITED,
        NOT_AUTHENTICATED,

        // Backend reachable but Vertex is down (503 with the vertex_unavailable marker).
        SERVER_UNAVAILABLE,

        // Device is online but the server never produced a response (cold start / deploy window /
        // timeout). Distinct from SERVER_UNAVAILABLE: here the backend itself didn't answer.
        SERVER_WAKING,
    }

    sealed interface UiAction {
        data class OnDraftChanged(val text: String) : UiAction
        data object OnSendClicked : UiAction
        data object OnStopClicked : UiAction
        data object OnClearHistory : UiAction
        data object OnRetry : UiAction
        data object OnDismissError : UiAction
        data object OnSignInTap : UiAction
        data class OnReportMessage(val message: ChatMessage) : UiAction
    }

    sealed interface UiEffect {
        data object ScrollToBottom : UiEffect
        data class ShowReportResult(val success: Boolean) : UiEffect
    }
}
