package com.todoapp.mobile.data.mapper

import com.todoapp.mobile.data.model.entity.SubtaskEntity
import com.todoapp.mobile.data.model.entity.SyncStatus
import com.todoapp.mobile.domain.model.Subtask

fun SubtaskEntity.toDomain(): Subtask = Subtask(
    id = id,
    parentTaskId = parentTaskId,
    title = title,
    isCompleted = isCompleted,
    orderIndex = orderIndex,
)

fun Subtask.toEntity(
    parentTaskId: Long = this.parentTaskId,
    orderIndex: Int = this.orderIndex,
    syncStatus: SyncStatus = SyncStatus.PENDING_CREATE,
): SubtaskEntity = SubtaskEntity(
    id = id,
    title = title,
    parentTaskId = parentTaskId,
    isCompleted = isCompleted,
    orderIndex = orderIndex,
    syncStatus = syncStatus,
)
