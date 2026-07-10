package com.todoapp.mobile.domain.repository

import com.todoapp.mobile.domain.model.Notification
import kotlinx.coroutines.flow.StateFlow

interface NotificationRepository {
    val notifications: StateFlow<List<Notification>>
    val unreadCount: StateFlow<Int>

    suspend fun refresh(force: Boolean = false): Result<Unit>

    suspend fun markRead(id: Long): Result<Unit>

    suspend fun markAllRead(): Result<Unit>

    suspend fun delete(id: Long): Result<Unit>

    suspend fun fetchUnreadCount(): Result<Int>
}
