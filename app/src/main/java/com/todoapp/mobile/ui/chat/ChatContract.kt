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
            val toolInFlight: String? = null,
        ) : UiState
    }

    enum class ChatError {
        GENERIC,
        BLOCKED,
        OFFLINE,
        LOOP_OVERFLOW,
        RATE_LIMITED,
        NOT_AUTHENTICATED,
    }

    sealed interface UiAction {
        data class OnDraftChanged(val text: String) : UiAction
        data object OnSendClicked : UiAction
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
