package com.todoapp.mobile.data.repository

import com.todoapp.mobile.data.model.network.data.NotificationData
import com.todoapp.mobile.data.source.remote.datasource.NotificationRemoteDataSource
import com.todoapp.mobile.domain.model.Notification
import com.todoapp.mobile.domain.model.NotificationType
import com.todoapp.mobile.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val remote: NotificationRemoteDataSource,
) : NotificationRepository {
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    override val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    override val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    @Volatile private var lastFetchedAt: Long = 0L

    override suspend fun refresh(force: Boolean): Result<Unit> {
        if (!force && System.currentTimeMillis() - lastFetchedAt < CACHE_TTL_MS && _notifications.value.isNotEmpty()) {
            return Result.success(Unit)
        }
        return remote.list(limit = PAGE_SIZE, before = null).map { listData ->
            val items = listData.items.map { it.toDomain() }
            _notifications.value = items
            _unreadCount.value = items.count { !it.isRead }
            lastFetchedAt = System.currentTimeMillis()
        }
    }

    override suspend fun markRead(id: Long): Result<Unit> {
        val before = _notifications.value
        val target = before.firstOrNull { it.id == id } ?: return Result.success(Unit)
        if (target.isRead) return Result.success(Unit)
        _notifications.update { list -> list.map { if (it.id == id) it.copy(isRead = true) else it } }
        _unreadCount.update { (it - 1).coerceAtLeast(0) }
        return remote.markRead(id).onFailure {
            _notifications.value = before
            _unreadCount.value = before.count { !it.isRead }
        }
    }

    override suspend fun markAllRead(): Result<Unit> {
        val before = _notifications.value
        _notifications.update { list -> list.map { it.copy(isRead = true) } }
        _unreadCount.value = 0
        return remote.markAllRead().onFailure {
            _notifications.value = before
            _unreadCount.value = before.count { !it.isRead }
        }
    }

    override suspend fun delete(id: Long): Result<Unit> {
        val before = _notifications.value
        if (before.none { it.id == id }) return Result.success(Unit)
        _notifications.update { list -> list.filterNot { it.id == id } }
        _unreadCount.value = _notifications.value.count { !it.isRead }
        return remote.delete(id).onFailure {
            _notifications.value = before
            _unreadCount.value = before.count { !it.isRead }
        }
    }

    override suspend fun fetchUnreadCount(): Result<Int> = remote.unreadCount().map { data ->
        val count = data.count.toInt().coerceAtLeast(0)
        _unreadCount.value = count
        count
    }

    private fun NotificationData.toDomain(): Notification = Notification(
        id = id,
        type = NotificationType.fromString(type),
        title = title,
        body = body,
        payload = payload,
        isRead = isRead,
        createdAt = createdAt,
    )

    private companion object {
        const val PAGE_SIZE = 50
        const val CACHE_TTL_MS = 30_000L
    }
}
