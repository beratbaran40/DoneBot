package com.todoapp.mobile.data.source.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.todoapp.mobile.data.model.entity.PomodoroSessionEntity
import com.todoapp.mobile.domain.model.PomodoroDayStat
import kotlinx.coroutines.flow.Flow

@Dao
interface PomodoroSessionDao {
    /**
     * `IGNORE` on the unique `client_session_id` makes the local write idempotent, which is what lets
     * sign-in backfill re-insert a row the device is still holding as PENDING_CREATE without colliding.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: PomodoroSessionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<PomodoroSessionEntity>)

    @Query("SELECT * FROM pomodoro_sessions WHERE sync_status != 'SYNCED' ORDER BY ended_at ASC LIMIT :limit")
    suspend fun getPending(limit: Int): List<PomodoroSessionEntity>

    @Query("UPDATE pomodoro_sessions SET sync_status = 'SYNCED' WHERE client_session_id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    /** Attaches rows produced while signed out to whoever signs in first. */
    @Query("UPDATE pomodoro_sessions SET owner_user_id = :ownerId WHERE owner_user_id = 0")
    suspend fun claimOrphans(ownerId: Long)

    @Query("DELETE FROM pomodoro_sessions")
    suspend fun deleteAll()

    /**
     * Per-day focus totals for the Activity screen, grouped on the **device-local** day so a session
     * that ran at 01:30 stays on the day the user experienced it.
     */
    @Query(
        """
        SELECT local_date AS date,
               SUM(elapsed_seconds) AS focusSeconds,
               SUM(CASE WHEN completed THEN 1 ELSE 0 END) AS completedSessions
        FROM pomodoro_sessions
        WHERE mode = 'FOCUS' AND local_date BETWEEN :start AND :end
        GROUP BY local_date
        """,
    )
    fun observeFocusByDay(start: Long, end: Long): Flow<List<PomodoroDayStat>>

    /** The rows of one sitting, for the Summary screen to total from persisted truth. */
    @Query("SELECT * FROM pomodoro_sessions WHERE client_run_id = :runId ORDER BY session_index ASC")
    fun observeRun(runId: String): Flow<List<PomodoroSessionEntity>>
}
