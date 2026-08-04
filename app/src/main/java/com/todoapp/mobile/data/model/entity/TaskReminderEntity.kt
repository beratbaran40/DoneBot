package com.todoapp.mobile.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One reminder time of a task that reminds more than once a day (a medicine course at 08:00 / 14:00 /
 * 20:00). Belongs to exactly one personal [TaskEntity]; deleting the parent cascades.
 *
 * A child table rather than a CSV column on `tasks` because [slot] has to be **stable**: it seeds the
 * alarm request code, so if slots were derived from a list index, deleting a middle reminder would
 * shift every later index and orphan an armed PendingIntent that keeps re-arming itself from its own
 * extras. A dedicated column makes the slot survive edits.
 *
 * `tasks.reminder_offset_minutes` stays authoritative for the classic single "N minutes before"
 * reminder — rows here take over only when at least one exists.
 */
@Entity(
    tableName = "task_reminders",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["task_id"]),
        Index(value = ["task_id", "slot"], unique = true),
    ],
)
data class TaskReminderEntity(
    @ColumnInfo(name = "task_id") val taskId: Long,
    /**
     * Absolute local time of day as MINUTE-of-day, matching `tasks.time_start`. The wire format uses
     * SECOND-of-day — convert at the mapper boundary or every reminder lands 60× off.
     */
    @ColumnInfo(name = "minute_of_day") val minuteOfDay: Int,
    /** 0-based, stable for the row's lifetime. Seeds the alarm request code. */
    @ColumnInfo(name = "slot", defaultValue = "0") val slot: Int = 0,
    @ColumnInfo(name = "sync_status", defaultValue = "PENDING_CREATE")
    val syncStatus: SyncStatus = SyncStatus.PENDING_CREATE,
    @ColumnInfo(name = "remote_id") val remoteId: Long? = null,
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0L,
)
