package com.todoapp.mobile.ui.chat

import com.todoapp.mobile.domain.model.ChatMessage

internal object ChatPreviewData {
    val sampleMessages: List<ChatMessage> = listOf(
        ChatMessage(
            id = 1L,
            role = ChatMessage.Role.USER,
            content = "What's on my list today?",
            createdAt = 0L,
        ),
        ChatMessage(
            id = 2L,
            role = ChatMessage.Role.ASSISTANT,
            content = "You have 3 tasks today: Buy groceries (4 PM), Pay rent (anytime), and a 30-minute focus block.",
            createdAt = 0L,
        ),
        ChatMessage(
            id = 3L,
            role = ChatMessage.Role.USER,
            content = "Anything overdue?",
            createdAt = 0L,
        ),
        ChatMessage(
            id = 4L,
            role = ChatMessage.Role.ASSISTANT,
            content = "Yes — \"Renew passport\" was due last Monday and is still open.",
            createdAt = 0L,
        ),
    )

    val emptyReady: ChatContract.UiState.Ready = ChatContract.UiState.Ready()

    val populatedReady: ChatContract.UiState.Ready = ChatContract.UiState.Ready(
        messages = sampleMessages,
        draft = "",
    )

    val thinkingReady: ChatContract.UiState.Ready = ChatContract.UiState.Ready(
        messages = sampleMessages.take(3),
        draft = "",
        isThinking = true,
    )

    val typingReady: ChatContract.UiState.Ready = ChatContract.UiState.Ready(
        messages = sampleMessages.take(2),
        draft = "Plan my afternoon around finishing the report",
    )

    val errorReady: ChatContract.UiState.Ready = ChatContract.UiState.Ready(
        messages = sampleMessages,
        error = ChatContract.ChatError.OFFLINE,
    )

    val serverWakingReady: ChatContract.UiState.Ready = ChatContract.UiState.Ready(
        messages = sampleMessages,
        error = ChatContract.ChatError.SERVER_WAKING,
        lastFailedPrompt = "Anything overdue?",
    )

    val serverWakingRetryingReady: ChatContract.UiState.Ready = serverWakingReady.copy(
        autoRetrySecondsRemaining = 12,
    )

    val guestLimitedReady: ChatContract.UiState.Ready = ChatContract.UiState.Ready(
        messages = sampleMessages.take(2),
        error = ChatContract.ChatError.NOT_AUTHENTICATED,
    )
}
