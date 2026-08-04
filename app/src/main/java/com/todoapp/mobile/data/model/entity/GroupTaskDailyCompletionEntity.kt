package com.todoapp.mobile.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * One completed occurrence of a **recurring group task**: "the 10 August run of this chore is done".
 *
 * There is deliberately no `user_id`. A group task's occurrence is completed for the whole group by
 * whoever ticks it first — the shared semantics the feature was designed around — so the row answers
 * "is this day done" and nothing else. The server still records *who* did it (its own table keeps a
 * user id); the client simply has no surface that asks.
 *
 * Keyed by the server task id for the same reason as [GroupSubtaskEntity]: local `group_tasks` rows
 * are deleted and re-inserted on every sync, so a local-id key would lose completions each refresh.
 */
@Entity(tableName = "group_task_daily_completions", primaryKeys = ["remote_task_id", "date"])
data class GroupTaskDailyCompletionEntity(
    @ColumnInfo(name = "remote_task_id") val remoteTaskId: Long,
    /** Epoch day, matching `task_daily_completions.date` and the wire format. */
    @ColumnInfo(name = "date") val date: Long,
)
