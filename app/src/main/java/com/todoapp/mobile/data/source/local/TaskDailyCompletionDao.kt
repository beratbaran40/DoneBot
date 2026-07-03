package com.todoapp.mobile.data.source.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.todoapp.mobile.data.model.entity.TaskDailyCompletionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDailyCompletionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TaskDailyCompletionEntity)

    @Query("DELETE FROM task_daily_completions WHERE task_id = :taskId AND date = :date")
    suspend fun delete(taskId: Long, date: Long)

    // Hide PENDING_DELETE rows: a locally-uncompleted instance whose completed=false hasn't reached the
    // server yet must read as not-completed in the UI, not as still-checked.
    @Query("SELECT * FROM task_daily_completions WHERE date = :date AND sync_status != 'PENDING_DELETE'")
    fun observeForDate(date: Long): Flow<List<TaskDailyCompletionEntity>>

    @Query(
        "SELECT * FROM task_daily_completions WHERE date BETWEEN :start AND :end AND sync_status != 'PENDING_DELETE'",
    )
    fun observeRange(start: Long, end: Long): Flow<List<TaskDailyCompletionEntity>>

    @Query("DELETE FROM task_daily_completions")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<TaskDailyCompletionEntity>)

    @Query("SELECT * FROM task_daily_completions WHERE task_id = :taskId AND date = :date")
    suspend fun get(taskId: Long, date: Long): TaskDailyCompletionEntity?

    @Query("SELECT * FROM task_daily_completions WHERE sync_status != 'SYNCED'")
    suspend fun getPending(): List<TaskDailyCompletionEntity>

    @Query("SELECT * FROM task_daily_completions WHERE sync_status = 'SYNCED' AND date BETWEEN :start AND :end")
    suspend fun getSyncedInWindow(start: Long, end: Long): List<TaskDailyCompletionEntity>

    @Query("UPDATE task_daily_completions SET sync_status = 'SYNCED' WHERE task_id = :taskId AND date = :date")
    suspend fun markSynced(taskId: Long, date: Long)
}
