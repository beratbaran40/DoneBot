package com.todoapp.mobile.data.repository

import com.todoapp.mobile.common.handleEmptyRequest
import com.todoapp.mobile.common.handleRequest
import com.todoapp.mobile.data.model.entity.ChatMessageEntity
import com.todoapp.mobile.data.model.network.request.ChatHistoryTurn
import com.todoapp.mobile.data.model.network.request.ChatMessageRequest
import com.todoapp.mobile.data.model.network.request.ChatReportRequest
import com.todoapp.mobile.data.model.network.response.ChatMessageResponseData
import com.todoapp.mobile.data.source.local.ChatMessageDao
import com.todoapp.mobile.data.source.remote.api.ToDoApi
import com.todoapp.mobile.domain.model.ChatMessage
import com.todoapp.mobile.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val chatMessageDao: ChatMessageDao,
    private val todoApi: ToDoApi,
) : ChatRepository {
    override fun observeMessages(): Flow<List<ChatMessage>> = chatMessageDao.observeAll().map { list -> list.map(::toDomain) }

    override suspend fun getMessages(): List<ChatMessage> = chatMessageDao.getAll().map(::toDomain)

    override suspend fun appendUserMessage(content: String): Long = chatMessageDao.insert(ChatMessageEntity(role = ROLE_USER, content = content))

    override suspend fun appendAssistantMessage(content: String): Long = chatMessageDao.insert(ChatMessageEntity(role = ROLE_MODEL, content = content))

    override suspend fun clear() = chatMessageDao.deleteAll()

    override suspend fun sendMessage(
        prompt: String,
        locale: String,
        history: List<ChatHistoryTurn>,
    ): Result<ChatMessageResponseData> = handleRequest {
        todoApi.sendChatMessage(
            ChatMessageRequest(prompt = prompt, locale = locale, history = history),
        )
    }

    override suspend fun reportMessage(content: String, reason: String?): Result<Unit> = handleEmptyRequest {
        todoApi.reportChatMessage(
            ChatReportRequest(messageContent = content, reason = reason),
        )
    }

    private fun toDomain(entity: ChatMessageEntity): ChatMessage = ChatMessage(
        id = entity.id,
        role = when (entity.role) {
            ROLE_MODEL -> ChatMessage.Role.ASSISTANT
            else -> ChatMessage.Role.USER
        },
        content = entity.content,
        createdAt = entity.createdAt,
    )

    companion object {
        const val ROLE_USER = "user"
        const val ROLE_MODEL = "model"
    }
}
