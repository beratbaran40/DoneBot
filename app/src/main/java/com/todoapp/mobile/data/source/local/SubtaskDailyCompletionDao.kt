package com.todoapp.mobile.data.source.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.todoapp.mobile.data.model.entity.SubtaskDailyCompletionEntity
import kotlinx.coroutines.flow.Flow

/** Per-task count of steps completed on one day, for the "2/3" progress on list surfaces. */
data class SubtaskDayDoneCount(
    val taskId: Long,
    val done: Int,
)

@Dao
interface SubtaskDailyCompletionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SubtaskDailyCompletionEntity)

    @Query("DELETE FROM subtask_daily_completions WHERE subtask_id = :subtaskId AND date = :date")
    suspend fun delete(subtaskId: Long, date: Long)

    @Query("SELECT subtask_id FROM subtask_daily_completions WHERE task_id = :taskId AND date = :date")
    suspend fun getDoneStepIds(taskId: Long, date: Long): List<Long>

    @Query("SELECT subtask_id FROM subtask_daily_completions WHERE task_id = :taskId AND date = :date")
    fun observeDoneStepIds(taskId: Long, date: Long): Flow<List<Long>>

    /**
     * One row per task that has any step done on [date]. Combined in Kotlin with the global
     * total-count query, the same way task_daily_completions is joined — see TaskRepositoryImpl.
     */
    @Query(
        "SELECT task_id AS taskId, COUNT(*) AS done FROM subtask_daily_completions " +
            "WHERE date = :date GROUP BY task_id",
    )
    fun observeDoneCountsForDate(date: Long): Flow<List<SubtaskDayDoneCount>>

    @Query("DELETE FROM subtask_daily_completions WHERE task_id = :taskId AND date = :date")
    suspend fun deleteForTaskDay(taskId: Long, date: Long)

    @Query("DELETE FROM subtask_daily_completions")
    suspend fun deleteAll()
}
