package com.todoapp.mobile.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single step ("adım") of a staged task. Belongs to exactly one personal [TaskEntity]; deleting the
 * parent cascades to its subtasks. `remoteId`/`syncStatus` are carried for a future backend sync — the
 * server has no subtask table yet, so today these rows live device-side only.
 */
@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_task_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("parent_task_id")],
)
data class SubtaskEntity(
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "parent_task_id") val parentTaskId: Long,
    @ColumnInfo(name = "is_completed", defaultValue = "0") val isCompleted: Boolean = false,
    @ColumnInfo(name = "order_index", defaultValue = "0") val orderIndex: Int = 0,
    @ColumnInfo(name = "sync_status", defaultValue = "PENDING_CREATE")
    val syncStatus: SyncStatus = SyncStatus.PENDING_CREATE,
    @ColumnInfo(name = "remote_id") val remoteId: Long? = null,
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0L,
)
