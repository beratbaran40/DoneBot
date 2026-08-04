// Detekt without type resolution mis-flags two patterns in this repository:
//  - IgnoredReturnValue on `Flow<T> = source.map { it.toDomain() }` (the result IS the return)
//  - UnusedPrivateMember on private extension fns (e.g. GroupData.toEntity) that ARE called
@file:Suppress("IgnoredReturnValue", "UnusedPrivateMember")

package com.todoapp.mobile.data.repository

import com.todoapp.mobile.common.DomainException
import com.todoapp.mobile.data.mapper.toDomain
import com.todoapp.mobile.data.mapper.toDomainSubtasks
import com.todoapp.mobile.data.mapper.toEntity
import com.todoapp.mobile.data.mapper.toGroupTask
import com.todoapp.mobile.data.mapper.toSubtaskEntities
import com.todoapp.mobile.data.model.entity.GroupEntity
import com.todoapp.mobile.data.model.entity.GroupSubtaskEntity
import com.todoapp.mobile.data.model.entity.GroupTaskDailyCompletionEntity
import com.todoapp.mobile.data.model.network.data.GroupData
import com.todoapp.mobile.data.model.network.data.GroupMemberData
import com.todoapp.mobile.data.model.network.data.GroupSummaryData
import com.todoapp.mobile.data.model.network.data.GroupSummaryDataList
import com.todoapp.mobile.data.model.network.request.CreateGroupRequest
import com.todoapp.mobile.data.model.network.request.GroupTaskUpdateRequest
import com.todoapp.mobile.data.model.network.request.InviteMemberRequest
import com.todoapp.mobile.data.model.network.request.ReportContentRequest
import com.todoapp.mobile.data.model.network.request.SubtaskRequest
import com.todoapp.mobile.data.model.network.request.TaskDailyCompletionRequest
import com.todoapp.mobile.data.model.network.request.TransferOwnershipRequest
import com.todoapp.mobile.data.model.network.request.UpdateGroupRequest
import com.todoapp.mobile.data.source.local.GroupSubtaskDao
import com.todoapp.mobile.data.source.local.GroupTaskDailyCompletionDao
import com.todoapp.mobile.data.source.local.datasource.GroupActivityLocalDataSource
import com.todoapp.mobile.data.source.local.datasource.GroupLocalDataSource
import com.todoapp.mobile.data.source.local.datasource.GroupMemberLocalDataSource
import com.todoapp.mobile.data.source.local.datasource.GroupTaskLocalDataSource
import com.todoapp.mobile.data.source.local.datasource.TaskLocalDataSource
import com.todoapp.mobile.data.source.remote.datasource.GroupRemoteDataSource
import com.todoapp.mobile.data.source.remote.datasource.TaskRemoteDataSource
import com.todoapp.mobile.di.IoDispatcher
import com.todoapp.mobile.domain.alarm.AlarmScheduler
import com.todoapp.mobile.domain.alarm.MAX_REMINDER_SLOTS
import com.todoapp.mobile.domain.model.Group
import com.todoapp.mobile.domain.model.GroupActivity
import com.todoapp.mobile.domain.model.GroupMember
import com.todoapp.mobile.domain.model.GroupTask
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.Subtask
import com.todoapp.mobile.domain.model.Task
import com.todoapp.mobile.domain.model.recurrenceRule
import com.todoapp.mobile.domain.model.startDate
import com.todoapp.mobile.domain.repository.GroupRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDate
import javax.inject.Inject

// LongParameterList: this repository fronts five local tables, two remote sources and the alarm
// scheduler, and every one is a real collaborator. Bundling them into a wrapper type would exist
// only to satisfy the counter, not to make anything clearer.
@Suppress("LargeClass", "LongParameterList")
class GroupRepositoryImpl
@Inject
constructor(
    private val groupRemoteDataSource: GroupRemoteDataSource,
    private val groupLocalDataSource: GroupLocalDataSource,
    private val groupTaskLocalDataSource: GroupTaskLocalDataSource,
    private val groupMemberLocalDataSource: GroupMemberLocalDataSource,
    private val groupActivityLocalDataSource: GroupActivityLocalDataSource,
    private val taskRemoteDataSource: TaskRemoteDataSource,
    private val taskLocalDataSource: TaskLocalDataSource,
    private val alarmScheduler: AlarmScheduler,
    private val groupSubtaskDao: GroupSubtaskDao,
    private val groupTaskDailyCompletionDao: GroupTaskDailyCompletionDao,
    private val todoApi: com.todoapp.mobile.data.source.remote.api.ToDoApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : GroupRepository {
    // Single-flights every local write keyed on remote_id (sync loop + createGroup): concurrent
    // getGroups callers (VM resume, pull-to-refresh, FCM force refresh on its own scope) must not
    // interleave check-then-insert on the same group.
    private val groupsSyncMutex = Mutex()

    @Volatile private var cachedGroups: GroupSummaryDataList? = null

    @Volatile private var groupsCachedAt: Long = 0L
    private val groupTasksSyncedAt = java.util.concurrent.ConcurrentHashMap<Long, Long>()
    private val cachedGroupDetail = java.util.concurrent.ConcurrentHashMap<Long, GroupData>()
    private val groupDetailCachedAt = java.util.concurrent.ConcurrentHashMap<Long, Long>()
    private val cachedGroupActivity = java.util.concurrent.ConcurrentHashMap<Long, List<GroupActivity>>()
    private val groupActivityCachedAt = java.util.concurrent.ConcurrentHashMap<Long, Long>()
    private val cachedGroupTasks = java.util.concurrent.ConcurrentHashMap<Long, List<GroupTask>>()
    private val groupTasksCachedAt = java.util.concurrent.ConcurrentHashMap<Long, Long>()

    override suspend fun createGroup(request: CreateGroupRequest): Result<GroupData> = groupRemoteDataSource
        .createGroup(request)
        .onSuccess { remote ->
            // Insert-if-absent under the sync mutex: a concurrent getGroups sync may have already
            // persisted this group from the summaries payload (which also carries role/counts —
            // fresher than this create response), so never overwrite and never double-insert.
            groupsSyncMutex.withLock {
                val entity = remote.toEntity()
                if (entity.remoteId?.let { groupLocalDataSource.getByRemoteId(it) } == null) {
                    groupLocalDataSource.insertIgnoring(withInitializedOrder(entity))
                }
            }
            invalidateGroupsCache()
        }

    override suspend fun getGroups(force: Boolean): Result<GroupSummaryDataList> {
        if (!force) {
            cachedGroups?.let {
                if (System.currentTimeMillis() - groupsCachedAt < GROUPS_CACHE_TTL_MS) {
                    return Result.success(it)
                }
            }
        }
        return groupRemoteDataSource
            .getGroups()
            .onSuccess { result ->
                val entities =
                    result.groups
                        .distinctBy { it.id } // defense: a backend payload repeating a group must not fan out locally
                        .map { summary ->
                            summary.toEntity()
                        }
                syncRemoteGroupsWithLocal(entities)
                cachedGroups = result
                groupsCachedAt = System.currentTimeMillis()
            }
    }

    private fun invalidateGroupsCache() {
        cachedGroups = null
        groupsCachedAt = 0L
    }

    private fun invalidateGroupCache(groupId: Long) {
        cachedGroupDetail.remove(groupId)
        groupDetailCachedAt.remove(groupId)
        cachedGroupActivity.remove(groupId)
        groupActivityCachedAt.remove(groupId)
        cachedGroupTasks.remove(groupId)
        groupTasksCachedAt.remove(groupId)
        groupTasksSyncedAt.remove(groupId)
    }

    // Wipes every per-group cache entry. invalidateGroupCache only evicts a single id, so on logout
    // these maps would otherwise keep growing — and (since this is a @Singleton that survives a
    // logout/login) the next user could read the previous user's cached group data.
    private fun clearAllGroupCaches() {
        cachedGroupDetail.clear()
        groupDetailCachedAt.clear()
        cachedGroupActivity.clear()
        groupActivityCachedAt.clear()
        cachedGroupTasks.clear()
        groupTasksCachedAt.clear()
        groupTasksSyncedAt.clear()
    }

    override suspend fun deleteGroup(id: Long): Result<Unit> {
        val localEntity =
            groupLocalDataSource.getGroupById(id) ?: return Result.failure(Exception("Group not found"))
        val remoteId = checkNotNull(localEntity.remoteId) {
            "Synced group ${localEntity.id} is missing remoteId"
        }
        return groupRemoteDataSource
            .deleteGroup(remoteId)
            .onSuccess {
                groupLocalDataSource.delete(localEntity)
                invalidateGroupCache(remoteId)
                invalidateGroupsCache()
            }
    }

    override suspend fun deleteGroupByRemoteId(remoteId: Long): Result<Unit> = groupRemoteDataSource
        .deleteGroup(remoteId)
        .onSuccess {
            val allGroups = groupLocalDataSource.getAllGroupsOrdered().first()
            val localEntity = allGroups.find { it.remoteId == remoteId }
            if (localEntity != null) {
                groupLocalDataSource.delete(localEntity)
            }
            invalidateGroupCache(remoteId)
            invalidateGroupsCache()
        }

    override suspend fun deleteAllLocalGroups(): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            groupTaskLocalDataSource.deleteAll()
            // Explicit, because these two are keyed by the SERVER task id and carry no foreign key —
            // exactly what lets them survive a sync refresh, and exactly what stops the group_tasks
            // wipe above from cascading into them. Miss this and account B inherits A's ticks.
            groupSubtaskDao.deleteAll()
            groupTaskDailyCompletionDao.deleteAll()
            groupMemberLocalDataSource.deleteAll()
            groupActivityLocalDataSource.deleteAll()
            val all = groupLocalDataSource.getAllGroupsOrdered().first()
            all.forEach { groupLocalDataSource.delete(it) }
            invalidateGroupsCache()
            clearAllGroupCaches()
        }
    }

    override fun observeAllGroups(): Flow<List<Group>> = groupLocalDataSource.getAllGroupsOrdered().map { list ->
        list.map { it.toDomain() }
    }

    override suspend fun reorderGroups(
        fromIndex: Int,
        toIndex: Int,
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            if (fromIndex == toIndex) return@runCatching

            val current = groupLocalDataSource.getAllGroupsOrdered().first()

            if (fromIndex !in current.indices || toIndex !in current.indices) {
                return@runCatching
            }

            val reordered =
                current.toMutableList().apply {
                    add(toIndex, removeAt(fromIndex))
                }

            val start = minOf(fromIndex, toIndex)
            val end = maxOf(fromIndex, toIndex)

            val updates =
                (start..end).map { index ->
                    reordered[index].id to index
                }

            groupLocalDataSource.updateOrderIndices(updates)
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { t -> Result.failure(DomainException.fromThrowable(t)) },
        )
    }

    // Serialized (mutex) + keyed on a fresh remote_id lookup per row. The previous shared-snapshot
    // "find or insert" raced: screen resume, pull-to-refresh, and FCM force refreshes run this
    // concurrently, and two callers could both miss a brand-new group (e.g. a just-accepted invite)
    // and insert it twice. The unique index on groups.remote_id (+ insertIgnoring) is the floor
    // beneath this lock.
    private suspend fun syncRemoteGroupsWithLocal(remoteEntities: List<GroupEntity>) = groupsSyncMutex.withLock {
        remoteEntities.forEach { remote ->
            val existing = remote.remoteId?.let { groupLocalDataSource.getByRemoteId(it) }
            if (existing == null) {
                groupLocalDataSource.insertIgnoring(withInitializedOrder(remote))
            } else {
                groupLocalDataSource.update(
                    existing.copy(
                        name = remote.name,
                        description = remote.description,
                        role = remote.role,
                        memberCount = remote.memberCount,
                        pendingTaskCount = remote.pendingTaskCount,
                        createdAt = remote.createdAt,
                    ),
                )
            }
        }
    }

    private suspend fun withInitializedOrder(entity: GroupEntity): GroupEntity = if (entity.orderIndex != 0) {
        entity
    } else {
        val current = groupLocalDataSource.getAllGroupsOrdered().first()
        val nextIndex = (current.maxOfOrNull { it.orderIndex } ?: -1) + 1
        entity.copy(orderIndex = nextIndex)
    }

    override suspend fun getGroupDetail(
        groupId: Long,
        force: Boolean,
    ): Result<GroupData> {
        if (!force) {
            val cached = cachedGroupDetail[groupId]
            val cachedAt = groupDetailCachedAt[groupId] ?: 0L
            if (cached != null && System.currentTimeMillis() - cachedAt < GROUP_DETAIL_TTL_MS) {
                return Result.success(cached)
            }
        }
        val remote = groupRemoteDataSource.getGroupDetail(groupId)
        if (remote.isSuccess) {
            remote.onSuccess { data ->
                val localGroup =
                    groupLocalDataSource
                        .getAllGroupsOrdered()
                        .first()
                        .find { it.remoteId == groupId }
                if (localGroup != null) {
                    persistMembersLocally(localGroup.id, data.members)
                }
                cachedGroupDetail[groupId] = data
                groupDetailCachedAt[groupId] = System.currentTimeMillis()
            }
            return remote
        }
        val localGroup =
            groupLocalDataSource
                .getAllGroupsOrdered()
                .first()
                .find { it.remoteId == groupId } ?: return remote
        val cachedMembers = groupMemberLocalDataSource.getByGroupIdOnce(localGroup.id)
        return Result.success(
            GroupData(
                id = groupId,
                name = localGroup.name,
                description = localGroup.description,
                createdAt = localGroup.createdAt,
                updatedAt = localGroup.createdAt,
                members =
                cachedMembers.map { entity ->
                    GroupMemberData(
                        userId = entity.userId,
                        displayName = entity.displayName,
                        email = entity.email,
                        avatarUrl = entity.avatarUrl,
                        role = entity.role,
                        joinedAt = entity.joinedAt,
                    )
                },
            ),
        )
    }

    private suspend fun persistMembersLocally(
        localGroupId: Long,
        members: List<GroupMemberData>,
    ) {
        val entities = members.map { it.toEntity(localGroupId) }
        groupMemberLocalDataSource.replaceAll(localGroupId, entities)
    }

    override suspend fun updateGroup(
        groupId: Long,
        name: String,
        description: String,
    ): Result<Unit> = groupRemoteDataSource
        .updateGroup(
            groupId,
            UpdateGroupRequest(id = groupId, name = name, description = description),
        ).onSuccess {
            invalidateGroupCache(groupId)
            invalidateGroupsCache()
        }

    override suspend fun createGroupTask(
        groupId: Long,
        task: Task,
        priority: String?,
        assignedToUserId: Long?,
    ): Result<Long> = taskRemoteDataSource
        .addTask(
            task,
            familyGroupId = groupId,
            assignedToUserId = assignedToUserId,
            priority = priority,
        ).map { it.id }
        .onSuccess {
            invalidateGroupCache(groupId)
            syncGroupTasks(groupId)
        }

    override suspend fun deleteGroupTask(
        groupId: Long,
        taskId: Long,
    ): Result<Unit> = groupRemoteDataSource
        .deleteGroupTask(groupId, taskId)
        .onSuccess {
            groupTaskLocalDataSource.deleteByRemoteId(taskId)
            invalidateGroupCache(groupId)
        }

    override suspend fun setGroupTaskDayCompletion(
        groupId: Long,
        taskId: Long,
        date: LocalDate,
        completed: Boolean,
    ): Result<Unit> {
        val day = date.toEpochDay()
        // Local first so the checkbox answers immediately; the group cache is invalidated either way
        // so a failed push is corrected by the next refresh rather than silently diverging.
        if (completed) {
            groupTaskDailyCompletionDao.upsert(GroupTaskDailyCompletionEntity(remoteTaskId = taskId, date = day))
        } else {
            groupTaskDailyCompletionDao.delete(remoteTaskId = taskId, date = day)
        }
        return runCatching {
            // The same endpoint personal tasks use — a group task is the same server row, and its
            // authorization now accepts any member of the group rather than only the creator.
            todoApi.setTaskDailyCompletion(
                taskId,
                TaskDailyCompletionRequest(date = day, completed = completed),
            )
            Unit
        }.onSuccess { invalidateGroupCache(groupId) }
    }

    override fun observeGroupTasksDoneOn(date: LocalDate): Flow<Set<Long>> = groupTaskDailyCompletionDao
        .observeDoneTaskIdsForDate(date.toEpochDay())
        .map { it.toSet() }

    override fun observeGroupSubtasks(taskId: Long): Flow<List<Subtask>> = groupSubtaskDao
        .observeByTask(taskId)
        .map { it.toDomainSubtasks() }

    override suspend fun setGroupSubtaskCompletion(
        groupId: Long,
        taskId: Long,
        steps: List<Subtask>,
        subtaskId: Long,
        isCompleted: Boolean,
    ): Result<Unit> {
        val next = steps.map { if (it.id == subtaskId) it.copy(isCompleted = isCompleted) else it }
        return groupRemoteDataSource
            .updateGroupTask(
                groupId = groupId,
                taskId = taskId,
                // The server reconciles by remoteId, so every step must carry its own — a null id
                // there would insert a duplicate and orphan the original.
                request = GroupTaskUpdateRequest(
                    subtasks = next.map {
                        SubtaskRequest(
                            remoteId = it.id,
                            title = it.title,
                            isCompleted = it.isCompleted,
                        )
                    },
                ),
            ).map { }
            .onSuccess {
                groupSubtaskDao.replaceForTask(
                    taskId,
                    next.mapIndexed { index, step ->
                        GroupSubtaskEntity(
                            remoteId = step.id,
                            remoteTaskId = taskId,
                            title = step.title,
                            isCompleted = step.isCompleted,
                            orderIndex = if (step.orderIndex >= 0) step.orderIndex else index,
                        )
                    },
                )
                invalidateGroupCache(groupId)
            }
    }

    override suspend fun updateGroupTaskStatus(
        groupId: Long,
        taskId: Long,
        groupTask: GroupTask,
        isCompleted: Boolean,
    ): Result<Unit> = groupRemoteDataSource
        .updateGroupTask(
            groupId = groupId,
            taskId = taskId,
            request = GroupTaskUpdateRequest(isCompleted = isCompleted),
        ).map { }
        .onSuccess {
            groupTaskLocalDataSource.updateCompletion(remoteId = taskId, isCompleted = isCompleted)
            invalidateGroupCache(groupId)
        }

    @Suppress("LongParameterList")
    override suspend fun updateGroupTask(
        groupId: Long,
        taskId: Long,
        title: String,
        description: String?,
        dueDate: Long?,
        priority: String?,
        assignedToUserId: Long?,
        isAllDay: Boolean?,
        timeStart: Long?,
        timeEnd: Long?,
        locationName: String?,
        locationAddress: String?,
        locationLat: Double?,
        locationLng: Double?,
        clearLocation: Boolean,
    ): Result<Unit> {
        val localGroup = groupLocalDataSource.getAllGroupsOrdered().first().find { it.remoteId == groupId }
        val assigneeMember =
            if (localGroup != null && assignedToUserId != null) {
                groupMemberLocalDataSource.getByGroupIdOnce(localGroup.id).find { it.userId == assignedToUserId }
            } else {
                null
            }
        return groupRemoteDataSource
            .updateGroupTask(
                groupId = groupId,
                taskId = taskId,
                request =
                GroupTaskUpdateRequest(
                    title = title,
                    description = description,
                    dueDate = dueDate,
                    isAllDay = isAllDay,
                    timeStart = timeStart,
                    timeEnd = timeEnd,
                    priority = priority,
                    assigneeId = assignedToUserId,
                    clearAssignee = assignedToUserId == null,
                    locationName = locationName,
                    locationAddress = locationAddress,
                    locationLat = locationLat,
                    locationLng = locationLng,
                    clearLocation = clearLocation,
                ),
            ).map { }
            .onSuccess {
                groupTaskLocalDataSource.updateTask(
                    remoteId = taskId,
                    title = title,
                    description = description,
                    dueDate = dueDate,
                    priority = priority,
                    assigneeUserId = assignedToUserId,
                    assigneeDisplayName = assigneeMember?.displayName,
                    assigneeAvatarUrl = assigneeMember?.avatarUrl,
                )
                invalidateGroupCache(groupId)
            }
    }

    override suspend fun assignGroupTask(
        groupId: Long,
        taskId: Long,
        userId: Long,
    ): Result<Unit> = groupRemoteDataSource
        .updateGroupTask(
            groupId = groupId,
            taskId = taskId,
            request = GroupTaskUpdateRequest(assigneeId = userId),
        ).map { }
        .onSuccess {
            invalidateGroupCache(groupId)
            syncGroupTasks(groupId)
        }

    override suspend fun unassignGroupTask(
        groupId: Long,
        taskId: Long,
    ): Result<Unit> {
        val existing = groupTaskLocalDataSource.getByRemoteId(taskId)
        return groupRemoteDataSource
            .updateGroupTask(
                groupId = groupId,
                taskId = taskId,
                request = GroupTaskUpdateRequest(clearAssignee = true),
            ).map { }
            .onSuccess {
                if (existing != null) {
                    groupTaskLocalDataSource.updateTask(
                        remoteId = taskId,
                        title = existing.title,
                        description = existing.description,
                        dueDate = existing.dueDate,
                        priority = existing.priority,
                        assigneeUserId = null,
                        assigneeDisplayName = null,
                        assigneeAvatarUrl = null,
                    )
                }
                invalidateGroupCache(groupId)
                syncGroupTasks(groupId)
            }
    }

    override suspend fun uploadTaskPhoto(
        taskId: Long,
        bytes: ByteArray,
        mimeType: String,
    ): Result<String> {
        val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val part = okhttp3.MultipartBody.Part.createFormData("file", "photo.jpg", body)
        return com.todoapp.mobile.common
            .handleRequest { todoApi.uploadTaskPhoto(taskId, part) }
            .map { it.url }
    }

    override suspend fun deleteTaskPhoto(
        taskId: Long,
        photoId: Long,
    ): Result<Unit> = com.todoapp.mobile.common.handleRequest {
        todoApi.deleteTaskPhoto(taskId, photoId)
    }

    override suspend fun uploadGroupAvatar(
        groupId: Long,
        bytes: ByteArray,
        mimeType: String,
    ): Result<Unit> {
        val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val part = okhttp3.MultipartBody.Part.createFormData("file", "group-avatar.jpg", body)
        return com.todoapp.mobile.common
            .handleRequest { todoApi.uploadGroupAvatar(groupId, part) }
            .map { }
            .onSuccess {
                invalidateGroupCache(groupId)
                invalidateGroupsCache()
            }
    }

    override suspend fun searchGroupTasksAcrossGroups(query: String): Result<List<Pair<Group, List<GroupTask>>>> = withContext(
        ioDispatcher
    ) {
        runCatching {
            val groups = groupLocalDataSource.getAllGroupsOrdered().first()
            val matchingTasks = groupTaskLocalDataSource.searchAll(query).first()
            val tasksByLocalGroupId = matchingTasks.groupBy { it.localGroupId }
            groups
                .filter { entity ->
                    val nameMatches =
                        entity.name.contains(query, ignoreCase = true) ||
                            entity.description.contains(query, ignoreCase = true)
                    nameMatches || tasksByLocalGroupId.containsKey(entity.id)
                }.map { entity ->
                    val group = entity.toDomain()
                    val tasks =
                        (tasksByLocalGroupId[entity.id] ?: emptyList())
                            .map { it.toDomain() }
                    group to tasks
                }
        }
    }

    override suspend fun getGroupMembers(groupId: Long): Result<List<GroupMember>> {
        val detailResult = groupRemoteDataSource.getGroupDetail(groupId)
        if (detailResult.isSuccess) {
            val data = detailResult.getOrThrow()
            val localGroup =
                groupLocalDataSource
                    .getAllGroupsOrdered()
                    .first()
                    .find { it.remoteId == groupId }
            if (localGroup != null) {
                persistMembersLocally(localGroup.id, data.members)
            }
            return Result.success(data.members.map { it.toGroupMember() })
        }
        val localGroup =
            groupLocalDataSource
                .getAllGroupsOrdered()
                .first()
                .find { it.remoteId == groupId } ?: return detailResult.map { emptyList() }
        return runCatching {
            groupMemberLocalDataSource.getByGroupIdOnce(localGroup.id).map { it.toDomain() }
        }
    }

    override fun observeGroupMembers(localGroupId: Long): Flow<List<GroupMember>> = groupMemberLocalDataSource.observeByGroupId(
        localGroupId
    ).map { entities ->
        entities.map { it.toDomain() }
    }

    override fun observeGroupActivity(localGroupId: Long): Flow<List<GroupActivity>> = groupActivityLocalDataSource.observeByGroupId(
        localGroupId
    ).map { entities ->
        entities.map { it.toDomain() }
    }

    override suspend fun inviteMember(
        groupId: Long,
        email: String,
    ): Result<Unit> = groupRemoteDataSource
        .inviteMember(InviteMemberRequest(groupId = groupId, email = email))
        .onSuccess { invalidateGroupCache(groupId) }

    override suspend fun removeMember(
        groupId: Long,
        userId: Long,
    ): Result<Unit> = groupRemoteDataSource
        .removeMember(groupId, userId)
        .onSuccess { invalidateGroupCache(groupId) }

    override suspend fun reportContent(
        groupId: Long,
        targetType: String,
        targetUserId: Long?,
        targetRef: String?,
        reason: String?,
    ): Result<Unit> = groupRemoteDataSource.reportContent(
        groupId,
        ReportContentRequest(
            targetType = targetType,
            targetUserId = targetUserId,
            targetRef = targetRef,
            reason = reason,
        ),
    )

    override suspend fun leaveGroup(groupId: Long): Result<Unit> = groupRemoteDataSource
        .leaveGroup(groupId)
        .onSuccess {
            val localEntity = groupLocalDataSource.getAllGroupsOrdered().first().find { it.remoteId == groupId }
            if (localEntity != null) {
                groupLocalDataSource.delete(localEntity)
            }
            invalidateGroupCache(groupId)
            invalidateGroupsCache()
        }

    override suspend fun transferOwnership(
        groupId: Long,
        userId: Long,
    ): Result<Unit> = groupRemoteDataSource
        .transferOwnership(groupId, TransferOwnershipRequest(userId))
        .onSuccess {
            invalidateGroupCache(groupId)
            invalidateGroupsCache()
        }

    override suspend fun getGroupActivity(
        groupId: Long,
        force: Boolean,
    ): Result<List<GroupActivity>> {
        if (!force) {
            val cached = cachedGroupActivity[groupId]
            val cachedAt = groupActivityCachedAt[groupId] ?: 0L
            if (cached != null && System.currentTimeMillis() - cachedAt < GROUP_DETAIL_TTL_MS) {
                return Result.success(cached)
            }
        }
        val remote = groupRemoteDataSource.getGroupActivity(groupId)
        if (remote.isSuccess) {
            remote.onSuccess { data ->
                val localGroup =
                    groupLocalDataSource
                        .getAllGroupsOrdered()
                        .first()
                        .find { it.remoteId == groupId }
                if (localGroup != null) {
                    val entities = data.activities.map { it.toEntity(localGroup.id) }
                    groupActivityLocalDataSource.replaceAll(localGroup.id, entities)
                }
            }
            return remote
                .map { data -> data.activities.map { it.toGroupActivity() } }
                .onSuccess { mapped ->
                    cachedGroupActivity[groupId] = mapped
                    groupActivityCachedAt[groupId] = System.currentTimeMillis()
                }
        }
        val localGroup =
            groupLocalDataSource
                .getAllGroupsOrdered()
                .first()
                .find { it.remoteId == groupId } ?: return remote.map { emptyList() }
        return runCatching {
            groupActivityLocalDataSource.getByGroupIdOnce(localGroup.id).map { it.toDomain() }
        }
    }

    override suspend fun getGroupTasks(
        groupId: Long,
        force: Boolean,
    ): Result<List<GroupTask>> {
        if (!force) {
            val cached = cachedGroupTasks[groupId]
            val cachedAt = groupTasksCachedAt[groupId] ?: 0L
            if (cached != null && System.currentTimeMillis() - cachedAt < GROUP_DETAIL_TTL_MS) {
                return Result.success(cached)
            }
        }
        val remote =
            taskRemoteDataSource.getTasks(familyGroupId = groupId).map { data ->
                data.tasks.map { it.toGroupTask() }
            }
        if (remote.isSuccess) {
            remote.onSuccess { tasks ->
                persistGroupTasksLocally(remoteGroupId = groupId, tasks = tasks)
                cachedGroupTasks[groupId] = tasks
                groupTasksCachedAt[groupId] = System.currentTimeMillis()
            }
            return remote
        }
        // Fallback to locally cached tasks
        val localGroup =
            groupLocalDataSource
                .getAllGroupsOrdered()
                .first()
                .find { it.remoteId == groupId } ?: return remote
        return runCatching {
            groupTaskLocalDataSource.observeByGroupId(localGroup.id).first().map { it.toDomain() }.withSteps()
        }
    }

    override fun observeGroupTasks(localGroupId: Long): Flow<List<GroupTask>> = groupTaskLocalDataSource.observeByGroupId(
        localGroupId
    ).map { entities ->
        entities.map { it.toDomain() }.withSteps()
    }

    override fun observeAllGroupTasks(): Flow<List<GroupTask>> = groupTaskLocalDataSource.observeAll().map { entities ->
        entities.map { it.toDomain() }.withSteps()
    }

    /**
     * Fills in the steps, which live in their own table and so cannot come out of the row mapper.
     * One query for the whole list rather than one per card — a group with 40 tasks would otherwise
     * issue 40 reads every time the list recomposed.
     */
    private suspend fun List<GroupTask>.withSteps(): List<GroupTask> {
        if (isEmpty()) return this
        val stepsByTask = groupSubtaskDao
            .getByTasks(map { it.id })
            .groupBy { it.remoteTaskId }
        // Nothing staged in this group — skip rebuilding every object for no change.
        if (stepsByTask.isEmpty()) return this
        return map { task ->
            val steps = stepsByTask[task.id].orEmpty()
            if (steps.isEmpty()) task else task.copy(subtasks = steps.toDomainSubtasks())
        }
    }

    override suspend fun syncGroupTasks(
        remoteGroupId: Long,
        force: Boolean,
    ): Result<Unit> = withContext(ioDispatcher) {
        val lastSync = groupTasksSyncedAt[remoteGroupId] ?: 0L
        if (!force && System.currentTimeMillis() - lastSync < GROUP_TASKS_TTL_MS) {
            return@withContext Result.success(Unit)
        }
        runCatching {
            val tasks =
                taskRemoteDataSource
                    .getTasks(familyGroupId = remoteGroupId)
                    .getOrThrow()
                    .tasks
                    .map { it.toGroupTask() }
            persistGroupTasksLocally(remoteGroupId = remoteGroupId, tasks = tasks)
            syncGroupTaskCompletions(tasks)
            groupTasksSyncedAt[remoteGroupId] = System.currentTimeMillis()
        }
    }

    /**
     * Pulls the completed occurrences of the group's recurring tasks. The endpoint is the personal
     * one — a group task is the same server row — and it now returns rows written by *any* member,
     * which is what makes completion shared. `TaskRepositoryImpl`'s own pull drops these ids because
     * they map to no personal task, so this is the only path that keeps them.
     */
    private suspend fun syncGroupTaskCompletions(tasks: List<GroupTask>) {
        val recurringIds = tasks.filter { it.recurrence != Recurrence.NONE }.map { it.id }.toSet()
        if (recurringIds.isEmpty()) return
        val today = LocalDate.now()
        val from = today.minusDays(COMPLETION_WINDOW_PAST_DAYS).toEpochDay()
        val to = today.plusDays(COMPLETION_WINDOW_FUTURE_DAYS).toEpochDay()
        runCatching {
            val items = todoApi.getTaskDailyCompletions(from, to).body()?.data?.items.orEmpty()
            val rows = items
                .filter { it.taskId in recurringIds }
                .map { GroupTaskDailyCompletionEntity(remoteTaskId = it.taskId, date = it.date) }
            // Full-window reconcile: a row the server no longer has was un-ticked on another device.
            recurringIds.forEach { taskId ->
                val serverDates = rows.filter { it.remoteTaskId == taskId }.map { it.date }.toSet()
                groupTaskDailyCompletionDao.getDatesInRange(taskId, from, to)
                    .filterNot { it in serverDates }
                    .forEach { groupTaskDailyCompletionDao.delete(taskId, it) }
            }
            if (rows.isNotEmpty()) groupTaskDailyCompletionDao.insertAll(rows)
        }
    }

    private suspend fun persistGroupTasksLocally(
        remoteGroupId: Long,
        tasks: List<GroupTask>,
    ) {
        val localGroup =
            groupLocalDataSource
                .getAllGroupsOrdered()
                .first()
                .find { it.remoteId == remoteGroupId } ?: return
        val entities = tasks.map { it.toEntity(localGroupId = localGroup.id, remoteGroupId = remoteGroupId) }
        // Read the previous id set BEFORE the wipe below: a task another member deleted is simply
        // absent from `tasks`, and its alarm re-arms itself from its own extras forever unless it is
        // cancelled here. Nothing else in the app ever learns that id existed.
        val previousRemoteIds = groupTaskLocalDataSource
            .observeByGroupId(localGroup.id)
            .first()
            .mapNotNull { it.remoteId }
            .toSet()
        groupTaskLocalDataSource.deleteByGroupId(localGroup.id)
        groupTaskLocalDataSource.insertAll(entities)
        // Steps live in their own table keyed by the SERVER id, precisely because the wholesale
        // delete above churns local ids on every refresh.
        tasks.forEach { groupSubtaskDao.replaceForTask(it.id, it.toSubtaskEntities()) }
        val remoteIds = tasks.map { it.id }
        if (remoteIds.isNotEmpty()) taskLocalDataSource.deleteByRemoteIds(remoteIds)
        rearmGroupAlarms(previousRemoteIds = previousRemoteIds, tasks = tasks)
    }

    /**
     * Group reminders are local alarms on every member's device — there is no server-side scheduler.
     * Re-armed here because a sync is the only moment the client sees the whole current picture: what
     * the group has now, and (via [previousRemoteIds]) what it no longer has.
     *
     * Cancel-then-arm rather than diffing per slot: a task whose reminder times shrank from three to
     * two must not leave the third armed, and a blanket cancel is far cheaper to reason about than
     * tracking which slots were live last time.
     */
    private fun rearmGroupAlarms(previousRemoteIds: Set<Long>, tasks: List<GroupTask>) {
        val currentIds = tasks.map { it.id }.toSet()
        (previousRemoteIds - currentIds).forEach { alarmScheduler.cancelRecurring(it, isGroupTask = true) }
        tasks.forEach { task ->
            alarmScheduler.cancelRecurring(task.id, isGroupTask = true)
            if (task.recurrence == Recurrence.NONE) return@forEach
            val anchor = task.startDate ?: return@forEach
            task.reminderTimes.take(MAX_REMINDER_SLOTS).forEachIndexed { slot, time ->
                alarmScheduler.scheduleRecurring(
                    taskId = task.id,
                    rule = task.recurrenceRule,
                    anchorDate = anchor,
                    hour = time.hour,
                    minute = time.minute,
                    message = task.title,
                    slot = slot,
                    isGroupTask = true,
                )
            }
        }
    }

    private fun GroupData.toEntity(): GroupEntity = GroupEntity(
        remoteId = id,
        name = name,
        description = description,
        createdAt = createdAt,
    )

    private fun GroupSummaryData.toEntity(): GroupEntity = GroupEntity(
        remoteId = id,
        name = name,
        description = description,
        createdAt = createdAt,
        role = role,
        memberCount = memberCount,
        pendingTaskCount = pendingTaskCount,
    )

    private fun GroupMemberData.toGroupMember(): GroupMember = GroupMember(
        userId = userId,
        displayName = displayName,
        email = email,
        avatarUrl = avatarUrl,
        role = role,
        joinedAt = joinedAt,
    )

    private fun com.todoapp.mobile.data.model.network.data.GroupActivityData.toGroupActivity(): GroupActivity = GroupActivity(
        id = id,
        type = type,
        actorName = actorName,
        actorAvatarUrl = actorAvatarUrl,
        description = description,
        timestamp = timestamp,
        taskTitle = taskTitle,
        targetName = targetName,
    )

    private companion object {
        const val GROUPS_CACHE_TTL_MS = 60_000L
        const val GROUP_TASKS_TTL_MS = 60_000L
        const val GROUP_DETAIL_TTL_MS = 15_000L

        // Same window TaskRepositoryImpl reconciles personal completions over, so the two stay in step.
        const val COMPLETION_WINDOW_PAST_DAYS = 30L
        const val COMPLETION_WINDOW_FUTURE_DAYS = 7L
    }
}
