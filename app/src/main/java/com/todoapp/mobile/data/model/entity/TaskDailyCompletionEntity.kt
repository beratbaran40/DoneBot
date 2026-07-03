package com.todoapp.mobile.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "task_daily_completions",
    primaryKeys = ["task_id", "date"],
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["date"])],
)
data class TaskDailyCompletionEntity(
    @ColumnInfo(name = "task_id") val taskId: Long,
    @ColumnInfo(name = "date") val date: Long,
    @ColumnInfo(name = "completed_at") val completedAt: Long,
    // Durable sync state. A failed push used to be lost (fire-and-forget); completions/uncompletions now
    // carry a pending state that the push step replays. PENDING_DELETE is a soft-deleted "uncomplete" that
    // still owes the server a completed=false, so the observe queries hide it. defaultValue lets Room
    // auto-migrate the new NOT NULL column.
    @ColumnInfo(name = "sync_status", defaultValue = "SYNCED") val syncStatus: SyncStatus = SyncStatus.SYNCED,
)
