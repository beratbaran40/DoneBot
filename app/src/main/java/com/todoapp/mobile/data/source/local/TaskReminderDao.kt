package com.todoapp.mobile.data.source.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.todoapp.mobile.data.model.entity.TaskReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskReminderDao {
    @Query("SELECT * FROM task_reminders WHERE task_id = :taskId ORDER BY slot ASC")
    fun observeByTask(taskId: Long): Flow<List<TaskReminderEntity>>

    @Query("SELECT * FROM task_reminders WHERE task_id = :taskId ORDER BY slot ASC")
    suspend fun getByTask(taskId: Long): List<TaskReminderEntity>

    /** Every reminder in the DB, for the boot-time reschedule sweep. */
    @Query("SELECT * FROM task_reminders ORDER BY task_id ASC, slot ASC")
    suspend fun getAll(): List<TaskReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reminders: List<TaskReminderEntity>)

    @Query("DELETE FROM task_reminders WHERE task_id = :taskId")
    suspend fun deleteByTask(taskId: Long)

    @Query("DELETE FROM task_reminders")
    suspend fun deleteAll()
}
