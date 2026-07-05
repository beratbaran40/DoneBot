package com.todoapp.mobile.domain.repository

import com.todoapp.mobile.data.model.network.data.GroupData
import com.todoapp.mobile.data.model.network.data.GroupSummaryDataList
import com.todoapp.mobile.data.model.network.request.CreateGroupRequest
import com.todoapp.mobile.domain.model.Group
import com.todoapp.mobile.domain.model.GroupActivity
import com.todoapp.mobile.domain.model.GroupMember
import com.todoapp.mobile.domain.model.GroupTask
import com.todoapp.mobile.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    suspend fun createGroup(request: CreateGroupRequest): Result<GroupData>

    suspend fun getGroups(force: Boolean = false): Result<GroupSummaryDataList>

    suspend fun deleteGroup(id: Long): Result<Unit>

    suspend fun deleteGroupByRemoteId(remoteId: Long): Result<Unit>

    suspend fun deleteAllLocalGroups(): Result<Unit>

    fun observeAllGroups(): Flow<List<Group>>

    suspend fun reorderGroups(
        fromIndex: Int,
        toIndex: Int,
    ): Result<Unit>

    suspend fun getGroupDetail(
        groupId: Long,
        force: Boolean = false,
    ): Result<GroupData>

    suspend fun updateGroup(
        groupId: Long,
        name: String,
        description: String,
    ): Result<Unit>

    suspend fun getGroupMembers(groupId: Long): Result<List<GroupMember>>

    suspend fun inviteMember(
        groupId: Long,
        email: String,
    ): Result<Unit>

    suspend fun removeMember(
        groupId: Long,
        userId: Long,
    ): Result<Unit>

    suspend fun reportContent(
        groupId: Long,
        targetType: String,
        targetUserId: Long? = null,
        targetRef: String? = null,
        reason: String? = null,
    ): Result<Unit>

    suspend fun leaveGroup(groupId: Long): Result<Unit>

    suspend fun transferOwnership(
        groupId: Long,
        userId: Long,
    ): Result<Unit>

    suspend fun getGroupActivity(
        groupId: Long,
        force: Boolean = false,
    ): Result<List<GroupActivity>>

    suspend fun getGroupTasks(
        groupId: Long,
        force: Boolean = false,
    ): Result<List<GroupTask>>

    suspend fun createGroupTask(
        groupId: Long,
        task: Task,
        priority: String? = null,
        assignedToUserId: Long? = null,
    ): Result<Long>

    suspend fun deleteGroupTask(
        groupId: Long,
        taskId: Long,
    ): Result<Unit>

    suspend fun updateGroupTaskStatus(
        groupId: Long,
        taskId: Long,
        groupTask: GroupTask,
        isCompleted: Boolean,
    ): Result<Unit>

    @Suppress("LongParameterList")
    suspend fun updateGroupTask(
        groupId: Long,
        taskId: Long,
        title: String,
        description: String?,
        dueDate: Long?,
        priority: String?,
        assignedToUserId: Long? = null,
        isAllDay: Boolean? = null,
        timeStart: Long? = null,
        timeEnd: Long? = null,
        locationName: String? = null,
        locationAddress: String? = null,
        locationLat: Double? = null,
        locationLng: Double? = null,
        clearLocation: Boolean = false,
    ): Result<Unit>

    suspend fun assignGroupTask(
        groupId: Long,
        taskId: Long,
        userId: Long,
    ): Result<Unit>

    suspend fun unassignGroupTask(
        groupId: Long,
        taskId: Long,
    ): Result<Unit>

    fun observeGroupTasks(localGroupId: Long): Flow<List<GroupTask>>

    fun observeAllGroupTasks(): Flow<List<GroupTask>>

    fun observeGroupMembers(localGroupId: Long): Flow<List<GroupMember>>

    fun observeGroupActivity(localGroupId: Long): Flow<List<GroupActivity>>

    suspend fun syncGroupTasks(
        remoteGroupId: Long,
        force: Boolean = false,
    ): Result<Unit>

    suspend fun searchGroupTasksAcrossGroups(query: String): Result<List<Pair<Group, List<GroupTask>>>>

    suspend fun uploadTaskPhoto(
        taskId: Long,
        bytes: ByteArray,
        mimeType: String,
    ): Result<String>

    suspend fun deleteTaskPhoto(
        taskId: Long,
        photoId: Long,
    ): Result<Unit>

    suspend fun uploadGroupAvatar(
        groupId: Long,
        bytes: ByteArray,
        mimeType: String,
    ): Result<Unit>
}
