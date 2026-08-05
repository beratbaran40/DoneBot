package com.todoapp.mobile.data.model.network.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Body for `POST /chat/message`. The server is stateless; the client resends history each turn. */
@Serializable
data class ChatMessageRequest(
    @SerialName("prompt") val prompt: String,
    /** ISO 639-1 language code: "en" or "tr". Server falls back to "en" when missing. */
    @SerialName("locale") val locale: String,
    @SerialName("history") val history: List<ChatHistoryTurn> = emptyList(),
    /**
     * Activity health points in HALF-heart units (0..24 — twelve hearts). Device-derived, so the
     * server can't compute it; it renders one `Health: 6½/12 hearts` line into the `[Context]` block.
     * Null when the value couldn't be read, and the server then omits that line rather than guessing.
     */
    @SerialName("healthHalfHearts") val healthHalfHearts: Int? = null,
)

@Serializable
data class ChatHistoryTurn(
    /** "user" or "assistant" */
    @SerialName("role") val role: String,
    @SerialName("content") val content: String,
)
