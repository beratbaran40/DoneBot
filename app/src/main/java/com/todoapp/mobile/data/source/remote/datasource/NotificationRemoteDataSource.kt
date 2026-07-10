package com.todoapp.mobile.data.source.remote.datasource

import com.todoapp.mobile.common.handleEmptyRequest
import com.todoapp.mobile.common.handleRequest
import com.todoapp.mobile.data.model.network.data.NotificationListData
import com.todoapp.mobile.data.model.network.data.UnreadCountData
import com.todoapp.mobile.data.source.remote.api.ToDoApi
import javax.inject.Inject

interface NotificationRemoteDataSource {
    suspend fun list(limit: Int, before: Long?): Result<NotificationListData>
    suspend fun markRead(id: Long): Result<Unit>
    suspend fun markAllRead(): Result<Unit>
    suspend fun delete(id: Long): Result<Unit>
    suspend fun unreadCount(): Result<UnreadCountData>
}

class NotificationRemoteDataSourceImpl @Inject constructor(
    private val api: ToDoApi,
) : NotificationRemoteDataSource {
    override suspend fun list(limit: Int, before: Long?): Result<NotificationListData> = handleRequest { api.listNotifications(limit = limit, before = before) }

    override suspend fun markRead(id: Long): Result<Unit> = handleEmptyRequest { api.markNotificationRead(id) }

    override suspend fun markAllRead(): Result<Unit> = handleEmptyRequest { api.markAllNotificationsRead() }

    override suspend fun delete(id: Long): Result<Unit> = handleEmptyRequest { api.deleteNotification(id) }

    override suspend fun unreadCount(): Result<UnreadCountData> = handleRequest { api.getUnreadNotificationCount() }
}
