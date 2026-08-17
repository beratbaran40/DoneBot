package com.todoapp.mobile.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One completed or abandoned pomodoro interval, written once when the interval ends and never updated.
 *
 * The existing `pomodoro` table is **settings**, not history: five columns, one row, no timestamps. This
 * is the history it never had.
 *
 * No foreign key. There is no local parent table to point at — [ownerUserId] is a scope value, not a
 * relationship, and a guest's rows legitimately carry 0 until the first sign-in claims them.
 */
@Entity(
    tableName = "pomodoro_sessions",
    indices = [
        // Idempotency: makes the local insert safe to repeat, which matters when sign-in backfill
        // re-downloads a row the device is still holding as PENDING_CREATE.
        Index(value = ["client_session_id"], unique = true),
        // The Activity screen groups by device-local day.
        Index(value = ["local_date"]),
        // The push step scans for what still owes the server.
        Index(value = ["sync_status"]),
    ],
)
data class PomodoroSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "client_session_id") val clientSessionId: String,
    @ColumnInfo(name = "client_run_id") val clientRunId: String,
    @ColumnInfo(name = "session_index") val sessionIndex: Int,
    /**
     * `String`, not [com.todoapp.mobile.domain.engine.PomodoroMode]. That enum carries an `OverTime`
     * member which must never reach a row, and `SyncStatusConverter` is the only enum TypeConverter this
     * database has. The conversion happens at the recorder boundary instead.
     */
    @ColumnInfo(name = "mode") val mode: String,
    @ColumnInfo(name = "planned_seconds") val plannedSeconds: Int,
    /** What actually ran. Every focus-time figure sums this; nothing sums [plannedSeconds]. */
    @ColumnInfo(name = "elapsed_seconds") val elapsedSeconds: Int,
    @ColumnInfo(name = "completed") val completed: Boolean,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long,
    /** Epoch day in the device's own zone at [endedAt] — what the in-app statistics group by. */
    @ColumnInfo(name = "local_date") val localDate: Long,
    @ColumnInfo(name = "tz_offset_minutes") val tzOffsetMinutes: Int,
    /** 0 = guest / unclaimed. Sign-out already deletes these rows; this is the belt to that braces. */
    @ColumnInfo(name = "owner_user_id", defaultValue = "0") val ownerUserId: Long = 0L,
    @ColumnInfo(name = "sync_status", defaultValue = "PENDING_CREATE")
    val syncStatus: SyncStatus = SyncStatus.PENDING_CREATE,
)
