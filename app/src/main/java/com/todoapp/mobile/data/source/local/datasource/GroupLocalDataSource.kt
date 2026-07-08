package com.todoapp.mobile.data.source.local.datasource

import com.todoapp.mobile.data.model.entity.GroupEntity
import kotlinx.coroutines.flow.Flow

interface GroupLocalDataSource {
    fun observeAll(): Flow<List<GroupEntity>>

    /** Insert honoring the unique remote_id index; returns -1 when an equal remote_id row exists. */
    suspend fun insertIgnoring(group: GroupEntity): Long

    suspend fun getByRemoteId(remoteId: Long): GroupEntity?

    suspend fun delete(group: GroupEntity)

    suspend fun deleteAll(group: GroupEntity)

    suspend fun update(group: GroupEntity)

    suspend fun getGroupById(id: Long): GroupEntity?

    suspend fun getGroupByName(name: String): GroupEntity

    suspend fun updateOrderIndex(
        id: Long,
        orderIndex: Int,
    )

    suspend fun updateOrderIndices(updates: List<Pair<Long, Int>>)

    fun getAllGroupsOrdered(): Flow<List<GroupEntity>>
}
