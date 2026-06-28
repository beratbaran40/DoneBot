package com.todoapp.mobile.data.source.remote.datasource

import com.todoapp.mobile.common.DomainException
import com.todoapp.mobile.common.handleEmptyRequest
import com.todoapp.mobile.common.handleRequest
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
import com.todoapp.mobile.data.model.network.response.ErrorResponse
import com.todoapp.mobile.data.source.remote.api.ToDoApi
import kotlinx.serialization.json.Json
import javax.inject.Inject

class GroupRemoteDataSourceImpl
@Inject
constructor(
    private val todoApi: ToDoApi,
) : GroupRemoteDataSource {
    override suspend fun createGroup(request: CreateGroupRequest): Result<GroupData> = handleRequest {
        todoApi.createGroup(
            request,
        )
    }

    override suspend fun getGroups(): Result<GroupSummaryDataList> = handleRequest { todoApi.getGroups() }

    override suspend fun deleteGroup(id: Long): Result<Unit> = handleEmptyRequest { todoApi.deleteGroup(id) }

    override suspend fun getGroupDetail(id: Long): Result<GroupData> = handleRequest { todoApi.getGroupDetail(id) }

    override suspend fun updateGroup(
        id: Long,
        request: UpdateGroupRequest,
    ): Result<Unit> = runCatching {
        val response = todoApi.updateGroup(request)
        if (!response.isSuccessful) {
            val msg =
                response
                    .errorBody()
                    ?.string()
                    ?.let { runCatching { Json.decodeFromString<ErrorResponse>(it).message }.getOrNull() }
                    ?: response.message() ?: "Failed to update group"
            throw DomainException.Server(msg)
        }
    }

    override suspend fun createGroupTask(
        groupId: Long,
        request: GroupTaskRequest,
    ): Result<GroupTaskData> = handleRequest {
        todoApi.createGroupTask(groupId, request)
    }

    override suspend fun inviteMember(request: InviteMemberRequest): Result<Unit> = handleEmptyRequest {
        todoApi.inviteMember(request)
    }

    override suspend fun removeMember(
        id: Long,
        userId: Long,
    ): Result<Unit> = handleEmptyRequest { todoApi.removeMember(id, userId) }

    override suspend fun leaveGroup(id: Long): Result<Unit> = handleEmptyRequest { todoApi.leaveGroup(id) }

    override suspend fun transferOwnership(
        id: Long,
        request: TransferOwnershipRequest,
    ): Result<Unit> = handleEmptyRequest {
        todoApi.transferOwnership(id, request)
    }

    override suspend fun getGroupActivity(id: Long): Result<GroupActivityDataList> = handleRequest {
        todoApi.getGroupActivity(id)
    }

    override suspend fun getGroupTasks(id: Long): Result<GroupTaskListData> = handleRequest {
        todoApi.getGroupTasks(
            id,
        )
    }

    override suspend fun deleteGroupTask(
        groupId: Long,
        taskId: Long,
    ): Result<Unit> = handleEmptyRequest { todoApi.deleteGroupTask(groupId, taskId) }

    override suspend fun updateGroupTask(
        groupId: Long,
        taskId: Long,
        request: GroupTaskUpdateRequest,
    ): Result<GroupTaskData> = handleRequest { todoApi.updateGroupTask(groupId, taskId, request) }
}
