package com.todoapp.mobile.data.mapper

import com.todoapp.mobile.data.model.entity.GroupTaskEntity
import com.todoapp.mobile.data.model.network.data.GroupMemberData
import com.todoapp.mobile.data.model.network.data.GroupTaskData
import com.todoapp.mobile.data.model.network.data.TaskData
import com.todoapp.mobile.domain.model.GroupMember
import com.todoapp.mobile.domain.model.GroupTask
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

fun TaskData.toGroupTask(): GroupTask = GroupTask(
    id = id,
    title = title,
    description = description,
    isCompleted = isCompleted,
    priority = priority,
    dueDate =
    LocalDate
        .ofEpochDay(date)
        .atTime(LocalTime.ofSecondOfDay(timeStart))
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli(),
    assignee =
    assignedTo?.let { user ->
        GroupMember(
            userId = user.userId,
            displayName = user.displayName,
            email = "",
            avatarUrl = null,
            role = "",
            joinedAt = 0L,
        )
    },
    isAllDay = isAllDay,
    timeStart = LocalTime.ofSecondOfDay(timeStart.coerceIn(0L, MAX_SECOND_OF_DAY)),
    timeEnd = LocalTime.ofSecondOfDay(timeEnd.coerceIn(0L, MAX_SECOND_OF_DAY)),
    photoUrls = photoUrls,
    locationName = locationName,
    locationAddress = locationAddress,
    locationLat = locationLat,
    locationLng = locationLng,
)

fun GroupTaskData.toDomain(): GroupTask = GroupTask(
    id = id,
    title = title,
    description = description,
    isCompleted = isCompleted,
    priority = priority,
    dueDate = dueDate,
    assignee = assignee?.toDomain(),
    photoUrls = photoUrls,
    locationName = locationName,
    locationAddress = locationAddress,
    locationLat = locationLat,
    locationLng = locationLng,
)

fun GroupMemberData.toDomain(): GroupMember = GroupMember(
    userId = userId,
    displayName = displayName,
    email = email,
    avatarUrl = avatarUrl,
    role = role,
    joinedAt = joinedAt,
)

fun GroupTaskEntity.toDomain(): GroupTask = GroupTask(
    id = remoteId ?: id,
    title = title,
    description = description,
    isCompleted = isCompleted,
    priority = priority,
    dueDate = dueDate,
    assignee =
    if (assigneeUserId != null && assigneeDisplayName != null) {
        GroupMember(
            userId = assigneeUserId,
            displayName = assigneeDisplayName,
            email = "",
            avatarUrl = assigneeAvatarUrl,
            role = "",
            joinedAt = 0L,
        )
    } else {
        null
    },
    isAllDay = isAllDay,
    timeStart = timeStart?.let { LocalTime.ofSecondOfDay(it.coerceIn(0L, MAX_SECOND_OF_DAY)) },
    timeEnd = timeEnd?.let { LocalTime.ofSecondOfDay(it.coerceIn(0L, MAX_SECOND_OF_DAY)) },
    photoUrls = photoUrls.split(',').filter { it.isNotBlank() },
    groupId = remoteGroupId,
    locationName = locationName,
    locationAddress = locationAddress,
    locationLat = locationLat,
    locationLng = locationLng,
)

fun GroupTask.toEntity(
    localGroupId: Long,
    remoteGroupId: Long,
): GroupTaskEntity = GroupTaskEntity(
    remoteId = id,
    localGroupId = localGroupId,
    remoteGroupId = remoteGroupId,
    title = title,
    description = description,
    isCompleted = isCompleted,
    priority = priority,
    dueDate = dueDate,
    isAllDay = isAllDay,
    timeStart = timeStart?.toSecondOfDay()?.toLong(),
    timeEnd = timeEnd?.toSecondOfDay()?.toLong(),
    assigneeUserId = assignee?.userId,
    assigneeDisplayName = assignee?.displayName,
    assigneeAvatarUrl = assignee?.avatarUrl,
    photoUrls = photoUrls.joinToString(","),
    locationLat = locationLat,
    locationLng = locationLng,
    locationName = locationName,
    locationAddress = locationAddress,
)

private const val MAX_SECOND_OF_DAY = 86_399L
