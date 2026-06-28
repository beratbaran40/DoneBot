package com.todoapp.mobile.data.source.remote.datasource

import com.todoapp.mobile.data.model.network.data.GroupActivityDataList
import com.todoapp.mobile.data.model.network.data.GroupData
import com.todoapp.mobile.data.model.network.data.GroupSummaryDataList
import com.todoapp.mobile.data.model.network.data.GroupTaskData
import com.todoapp.mobile.data.model.network.data.GroupTaskListData
import com.todoapp.mobile.data.model.network.request.CreateGroupRequest
import com.todoapp.mobile.data.model.network.request.GroupTaskRequest
import com.todoapp.mobile.data.model.network.request.GroupTaskUpdateRequest
import com.todoapp.mobile.data.model.network.request.InviteMemberRequest
import com.todoapp.mobile.data.model.network.request.TransferOwnershipRequest
import com.todoapp.mobile.data.model.network.request.UpdateGroupRequest

interface GroupRemoteDataSource {
    suspend fun createGroup(request: CreateGroupRequest): Result<GroupData>

    suspend fun getGroups(): Result<GroupSummaryDataList>

    suspend fun deleteGroup(id: Long): Result<Unit>

    suspend fun getGroupDetail(id: Long): Result<GroupData>

    suspend fun updateGroup(
        id: Long,
        request: UpdateGroupRequest,
    ): Result<Unit>

    suspend fun createGroupTask(
        groupId: Long,
        request: GroupTaskRequest,
    ): Result<GroupTaskData>

    suspend fun inviteMember(request: InviteMemberRequest): Result<Unit>

    suspend fun removeMember(
        id: Long,
        userId: Long,
    ): Result<Unit>

    suspend fun leaveGroup(id: Long): Result<Unit>

    suspend fun transferOwnership(
        id: Long,
        request: TransferOwnershipRequest,
    ): Result<Unit>

    suspend fun getGroupActivity(id: Long): Result<GroupActivityDataList>

    suspend fun getGroupTasks(id: Long): Result<GroupTaskListData>

    suspend fun deleteGroupTask(
        groupId: Long,
        taskId: Long,
    ): Result<Unit>

    suspend fun updateGroupTask(
        groupId: Long,
        taskId: Long,
        request: GroupTaskUpdateRequest,
    ): Result<GroupTaskData>
}
