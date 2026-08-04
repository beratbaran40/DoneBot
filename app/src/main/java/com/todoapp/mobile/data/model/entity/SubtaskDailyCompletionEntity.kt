package com.todoapp.mobile.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Per-day completion of a single step, for tasks that are BOTH recurring and staged ("every morning:
 * water, vitamin, stretch"). The exact twin of [TaskDailyCompletionEntity], one level down.
 *
 * Why a table and not [SubtaskEntity.isCompleted]: that flag is a single boolean, so on a recurring
 * task the step checked on day 1 would read as done forever. A recurring parent's own "done" state
 * already lives in `task_daily_completions` for the same reason; steps need the same treatment, and
 * checking the last step of a day is what writes the parent's row for that day.
 *
 * For a non-recurring staged task nothing is written here — `subtasks.is_completed` stays the truth.
 */
@Entity(
    tableName = "subtask_daily_completions",
    primaryKeys = ["subtask_id", "date"],
    foreignKeys = [
        ForeignKey(
            entity = SubtaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["subtask_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["date"]),
        Index(value = ["task_id", "date"]),
    ],
)
data class SubtaskDailyCompletionEntity(
    @ColumnInfo(name = "subtask_id") val subtaskId: Long,
    /**
     * Denormalized parent id so the per-day "2 of 3 steps" count is one indexed query with no JOIN.
     * Deliberately NOT a second foreign key — a single CASCADE path (through `subtasks`) keeps delete
     * ordering unambiguous when a whole task is tombstoned.
     */
    @ColumnInfo(name = "task_id") val taskId: Long,
    /** Epoch day of the occurrence this completion belongs to. */
    @ColumnInfo(name = "date") val date: Long,
    @ColumnInfo(name = "completed_at") val completedAt: Long,
    /**
     * Mirrors task_daily_completions. Defaults to SYNCED because step-level completions are not sent
     * to the backend yet — nothing should ever queue. Flip to PENDING_* when the endpoint lands.
     */
    @ColumnInfo(name = "sync_status", defaultValue = "SYNCED") val syncStatus: SyncStatus = SyncStatus.SYNCED,
)
