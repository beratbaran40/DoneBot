package com.todoapp.mobile.data.mapper

import com.todoapp.mobile.data.model.entity.GroupActivityEntity
import com.todoapp.mobile.data.model.network.data.GroupActivityData
import com.todoapp.mobile.domain.model.GroupActivity

fun GroupActivityEntity.toDomain(): GroupActivity = GroupActivity(
    id = remoteId,
    type = type,
    actorName = actorName,
    actorAvatarUrl = actorAvatarUrl,
    description = description,
    timestamp = timestamp,
    taskTitle = taskTitle,
    targetName = targetName,
)

fun GroupActivityData.toEntity(localGroupId: Long): GroupActivityEntity = GroupActivityEntity(
    remoteId = id,
    localGroupId = localGroupId,
    type = type,
    actorName = actorName,
    actorAvatarUrl = actorAvatarUrl,
    description = description,
    timestamp = timestamp,
    taskTitle = taskTitle,
    targetName = targetName,
)
