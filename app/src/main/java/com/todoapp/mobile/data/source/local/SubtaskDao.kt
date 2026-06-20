package com.todoapp.mobile.data.source.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.todoapp.mobile.data.model.entity.SubtaskEntity
import kotlinx.coroutines.flow.Flow

data class SubtaskCount(
    val taskId: Long,
    val total: Int,
    val done: Int,
)

@Dao
interface SubtaskDao {
    @Query("SELECT * FROM subtasks WHERE parent_task_id = :taskId ORDER BY order_index ASC")
    fun observeByTask(taskId: Long): Flow<List<SubtaskEntity>>

    @Query(
        "SELECT parent_task_id AS taskId, COUNT(*) AS total, " +
            "SUM(is_completed) AS done FROM subtasks GROUP BY parent_task_id",
    )
    fun observeSubtaskCounts(): Flow<List<SubtaskCount>>

    @Query("SELECT * FROM subtasks WHERE parent_task_id = :taskId ORDER BY order_index ASC")
    suspend fun getByTask(taskId: Long): List<SubtaskEntity>

    @Query("SELECT * FROM subtasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SubtaskEntity?

    @Query("SELECT COUNT(*) FROM subtasks WHERE parent_task_id = :taskId")
    suspend fun countByTask(taskId: Long): Int

    @Insert
    suspend fun insert(subtask: SubtaskEntity): Long

    @Insert
    suspend fun insertAll(subtasks: List<SubtaskEntity>)

    @Update
    suspend fun update(subtask: SubtaskEntity)

    @Delete
    suspend fun delete(subtask: SubtaskEntity)

    @Query("DELETE FROM subtasks WHERE parent_task_id = :taskId")
    suspend fun deleteByTask(taskId: Long)
}
