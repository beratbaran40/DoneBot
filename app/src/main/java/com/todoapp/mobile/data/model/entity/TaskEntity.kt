package com.todoapp.mobile.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["date"]),
        Index(value = ["recurrence"]),
    ],
)
data class TaskEntity(
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "date") val date: Long,
    @ColumnInfo(name = "time_start") val timeStart: Long,
    @ColumnInfo(name = "time_end") val timeEnd: Long,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean,
    @ColumnInfo(name = "is_secret") val isSecret: Boolean = false,
    @ColumnInfo(name = "remote_id") val remoteId: Long? = null,
    @ColumnInfo(
        name = "sync_status",
        defaultValue = "PENDING_CREATE",
    )
    val syncStatus: SyncStatus = SyncStatus.PENDING_CREATE,
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "order_index", defaultValue = "0") val orderIndex: Int = 0,
    @ColumnInfo(name = "photo_urls", defaultValue = "") val photoUrls: String = "",
    /**
     * Minutes before timeStart at which to fire the reminder. -1 sentinel = no reminder.
     * Stored as Long (not nullable) because the Room auto-migration generator handles defaults
     * cleaner for primitive columns.
     */
    @ColumnInfo(name = "reminder_offset_minutes", defaultValue = "0") val reminderOffsetMinutes: Long = 0L,
    @ColumnInfo(name = "category", defaultValue = "PERSONAL") val category: String = "PERSONAL",
    @ColumnInfo(name = "custom_category_name") val customCategoryName: String? = null,
    @ColumnInfo(name = "recurrence", defaultValue = "NONE") val recurrence: String = "NONE",
    @ColumnInfo(name = "is_all_day", defaultValue = "0") val isAllDay: Boolean = false,
    @ColumnInfo(name = "location_lat") val locationLat: Double? = null,
    @ColumnInfo(name = "location_lng") val locationLng: Double? = null,
    @ColumnInfo(name = "location_name") val locationName: String? = null,
    @ColumnInfo(name = "location_address") val locationAddress: String? = null,
    /**
     * Epoch day on which the whole recurring routine was marked finished from the Recurring tab.
     * Null = still active. A recurring task stops firing on days AFTER this date (see
     * Recurrence.firesOn); days up to and including it keep their per-day completion state.
     */
    @ColumnInfo(name = "finished_on") val finishedOn: Long? = null,
    /** Client-generated idempotency key (UUID); null for rows created before v24. §4.12 */
    @ColumnInfo(name = "client_task_id") val clientTaskId: String? = null,
    /**
     * RRULE INTERVAL: fire every N periods of [recurrence]. 1 = every period, which is the legacy
     * behaviour every pre-v29 row keeps via the default.
     */
    @ColumnInfo(name = "recurrence_interval", defaultValue = "1") val recurrenceInterval: Int = 1,
    /**
     * RRULE BYDAY: CSV of java.time.DayOfWeek names ("MONDAY,WEDNESDAY,FRIDAY"). Null/blank = derive
     * the weekday from the anchor date, i.e. legacy WEEKLY behaviour. Only meaningful for WEEKLY.
     */
    @ColumnInfo(name = "recurrence_by_day") val recurrenceByDay: String? = null,
    /**
     * RRULE UNTIL: last epoch day the rule may fire, inclusive. Null = open-ended.
     *
     * Deliberately DISTINCT from [finishedOn]: this is the *scheduled* end picked at creation ("take
     * this for a month"), whereas finishedOn is the *manual* retire from the Recurring tab. Both are
     * honoured by Recurrence.firesOn and the earlier one wins.
     */
    @ColumnInfo(name = "recurrence_until") val recurrenceUntil: Long? = null,
    /**
     * The task shape the user declared when creating it (a `TaskType` name), or null for "never
     * declared" — every row older than this column, and every row that reached this device from the
     * server. Readers derive the type for those; see `Task.resolvedType()`.
     *
     * Nullable with NO default, unlike [category] and [recurrence] which took NOT NULL + a default.
     * Those defaults were behaviourally correct for legacy rows; here none of the four values is —
     * any of them would be a claim about a task nobody made a claim about.
     *
     * Stored as the enum's name rather than through a TypeConverter, matching [recurrence] and
     * [category]: a name written by a newer build then reads back as null on an older one instead of
     * throwing.
     */
    @ColumnInfo(name = "declared_type") val declaredType: String? = null,
)

enum class SyncStatus {
    PENDING_CREATE,
    PENDING_UPDATE,
    PENDING_DELETE,
    SYNCED,
}
