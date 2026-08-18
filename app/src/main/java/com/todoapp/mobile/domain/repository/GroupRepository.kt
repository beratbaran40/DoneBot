package com.todoapp.mobile.domain.repository

import com.todoapp.mobile.data.model.network.data.GroupData
import com.todoapp.mobile.data.model.network.data.GroupSummaryDataList
import com.todoapp.mobile.data.model.network.request.CreateGroupRequest
import com.todoapp.mobile.domain.model.Group
import com.todoapp.mobile.domain.model.GroupActivity
import com.todoapp.mobile.domain.model.GroupMember
import com.todoapp.mobile.domain.model.GroupTask
import com.todoapp.mobile.domain.model.Subtask
import com.todoapp.mobile.domain.model.Task
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

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

    /**
     * Completes (or un-completes) **one occurrence** of a recurring group task. Shared across the
     * group: whoever ticks it first completes that day for everyone, so this takes no user id.
     *
     * Non-recurring group tasks keep using [updateGroupTaskStatus] — the flat `isCompleted` flag is
     * still the right model when there is only ever one occurrence.
     */
    suspend fun setGroupTaskDayCompletion(
        groupId: Long,
        taskId: Long,
        date: LocalDate,
        completed: Boolean,
    ): Result<Unit>

    /** Ids of the group tasks already completed on [date] — one query per list surface, not per card. */
    fun observeGroupTasksDoneOn(date: LocalDate): Flow<Set<Long>>

    /** Steps of a group task, ordered. Empty ⇒ the task is not staged. */
    fun observeGroupSubtasks(taskId: Long): Flow<List<Subtask>>

    /**
     * Ticks one step of a group task. Takes [steps] — the task's whole current step set — because the
     * update endpoint reconciles by replacement: sending only the changed one would delete the rest.
     */
    suspend fun setGroupSubtaskCompletion(
        groupId: Long,
        taskId: Long,
        steps: List<Subtask>,
        subtaskId: Long,
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
        /** `Recurrence` name. Null leaves the stored frequency alone. */
        recurrence: String? = null,
        recurrenceInterval: Int? = null,
        /** Weekday CSV, WEEKLY only. */
        recurrenceByDay: String? = null,
        /**
         * Epoch day of the routine's scheduled end. Null means "leave it alone", like every other
         * field here — there is no way to CLEAR it from this endpoint, which is why moving a start
         * past an existing end has to carry a new end along with it rather than dropping one.
         */
        recurrenceUntil: Long? = null,
        /** Second-of-day, like every other reminder-time field on the wire. */
        reminderTimes: List<Int>? = null,
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
