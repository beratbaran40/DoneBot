package com.todoapp.mobile.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "group_tasks",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["local_group_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("local_group_id")],
)
data class GroupTaskEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "remote_id") val remoteId: Long? = null,
    @ColumnInfo(name = "local_group_id") val localGroupId: Long,
    @ColumnInfo(name = "remote_group_id") val remoteGroupId: Long,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "description") val description: String? = null,
    @ColumnInfo(name = "is_completed", defaultValue = "0") val isCompleted: Boolean = false,
    @ColumnInfo(name = "priority") val priority: String? = null,
    @ColumnInfo(name = "due_date") val dueDate: Long? = null,
    @ColumnInfo(name = "is_all_day", defaultValue = "0") val isAllDay: Boolean = false,
    @ColumnInfo(name = "time_start") val timeStart: Long? = null,
    @ColumnInfo(name = "time_end") val timeEnd: Long? = null,
    @ColumnInfo(name = "assignee_user_id") val assigneeUserId: Long? = null,
    @ColumnInfo(name = "assignee_display_name") val assigneeDisplayName: String? = null,
    @ColumnInfo(name = "assignee_avatar_url") val assigneeAvatarUrl: String? = null,
    @ColumnInfo(name = "photo_urls", defaultValue = "") val photoUrls: String = "",
    @ColumnInfo(name = "location_lat") val locationLat: Double? = null,
    @ColumnInfo(name = "location_lng") val locationLng: Double? = null,
    @ColumnInfo(name = "location_name") val locationName: String? = null,
    @ColumnInfo(name = "location_address") val locationAddress: String? = null,
    @ColumnInfo(name = "category", defaultValue = "PERSONAL") val category: String = "PERSONAL",
    @ColumnInfo(name = "custom_category_name") val customCategoryName: String? = null,
    @ColumnInfo(name = "recurrence", defaultValue = "NONE") val recurrence: String = "NONE",
    /** RRULE INTERVAL: fire every N periods of [recurrence]. 1 = every period. */
    @ColumnInfo(name = "recurrence_interval", defaultValue = "1") val recurrenceInterval: Int = 1,
    /** RRULE BYDAY as a CSV of [java.time.DayOfWeek] names; null/empty = the start date's own weekday. */
    @ColumnInfo(name = "recurrence_by_day") val recurrenceByDay: String? = null,
    /** RRULE UNTIL as an epoch day, inclusive. Null = repeats forever. */
    @ColumnInfo(name = "recurrence_until") val recurrenceUntil: Long? = null,
    /**
     * Absolute reminder times as a CSV of MINUTE-of-day values (the wire format is SECOND-of-day —
     * convert at the mapper or every reminder lands 60× off).
     *
     * A CSV here where personal tasks use a `task_reminders` table: that table exists so a reminder's
     * `slot` stays stable across edits, and no slot can be stable on this side — a group sync deletes
     * and re-inserts every local row. Group alarms are therefore swept and re-armed wholesale.
     */
    @ColumnInfo(name = "reminder_times") val reminderTimes: String? = null,
)
