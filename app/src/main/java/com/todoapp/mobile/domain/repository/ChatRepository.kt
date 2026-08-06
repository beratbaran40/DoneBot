package com.todoapp.mobile.domain.repository

import com.todoapp.mobile.data.model.network.request.ChatHistoryTurn
import com.todoapp.mobile.data.model.network.response.ChatMessageResponseData
import com.todoapp.mobile.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeMessages(): Flow<List<ChatMessage>>

    suspend fun getMessages(): List<ChatMessage>

    suspend fun appendUserMessage(content: String): Long

    suspend fun appendAssistantMessage(content: String): Long

    suspend fun clear()

    /**
     * Sends a chat message to the server-side DoneBot proxy. The server runs
     * function calling against Vertex AI on its end; the client only deals
     * with prompt/history in/text out.
     *
     * [healthHalfHearts] is the Activity health-points bar in half-heart units. It travels with the
     * prompt because the server cannot derive it — the bar is folded from device-local completion
     * history over a persisted checkpoint — and the server renders one line of the `[Context]` block
     * from it. Null (the default) simply omits that line.
     */
    suspend fun sendMessage(
        prompt: String,
        locale: String,
        history: List<ChatHistoryTurn>,
        healthHalfHearts: Int? = null,
    ): Result<ChatMessageResponseData>

    /**
     * Flags an offensive or inappropriate assistant reply for human moderation
     * review (Google Play Generative AI policy: in-app reporting of AI content).
     */
    suspend fun reportMessage(content: String, reason: String? = null): Result<Unit>
}
