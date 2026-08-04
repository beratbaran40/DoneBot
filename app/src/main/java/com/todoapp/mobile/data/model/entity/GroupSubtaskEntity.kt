package com.todoapp.mobile.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One step of a staged group task — the group-side mirror of [SubtaskEntity].
 *
 * Keyed by [remoteTaskId] (the server's task id) rather than the local `group_tasks.id`, and with no
 * foreign key, on purpose: `persistGroupTasksLocally` refreshes a group by deleting every local row
 * and re-inserting, so local ids churn on every sync and a CASCADE would drop these along with them.
 * The server id is the only identifier that survives a refresh.
 *
 * A table rather than a column because a step carries per-item completion that the UI toggles; the
 * `photo_urls` CSV precedent only works for values nothing ever edits in place.
 */
@Entity(
    tableName = "group_subtasks",
    indices = [
        Index(value = ["remote_task_id"]),
        Index(value = ["remote_id"], unique = true),
    ],
)
data class GroupSubtaskEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0L,
    /** The server's subtask id. Unique, so a re-sync upserts rather than duplicating. */
    @ColumnInfo(name = "remote_id") val remoteId: Long,
    @ColumnInfo(name = "remote_task_id") val remoteTaskId: Long,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "is_completed", defaultValue = "0") val isCompleted: Boolean = false,
    @ColumnInfo(name = "order_index", defaultValue = "0") val orderIndex: Int = 0,
)
