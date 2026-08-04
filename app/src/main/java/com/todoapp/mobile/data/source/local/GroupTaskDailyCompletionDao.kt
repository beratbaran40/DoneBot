package com.todoapp.mobile.data.source.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.todoapp.mobile.data.model.entity.GroupTaskDailyCompletionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupTaskDailyCompletionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: GroupTaskDailyCompletionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<GroupTaskDailyCompletionEntity>)

    @Query("DELETE FROM group_task_daily_completions WHERE remote_task_id = :remoteTaskId AND date = :date")
    suspend fun delete(remoteTaskId: Long, date: Long)

    @Query(
        "SELECT EXISTS(SELECT 1 FROM group_task_daily_completions " +
            "WHERE remote_task_id = :remoteTaskId AND date = :date)",
    )
    suspend fun isDone(remoteTaskId: Long, date: Long): Boolean

    /** Task ids completed on [date] — one query for a whole list surface rather than one per card. */
    @Query("SELECT remote_task_id FROM group_task_daily_completions WHERE date = :date")
    fun observeDoneTaskIdsForDate(date: Long): Flow<List<Long>>

    @Query(
        "SELECT date FROM group_task_daily_completions " +
            "WHERE remote_task_id = :remoteTaskId AND date BETWEEN :fromDay AND :toDay",
    )
    suspend fun getDatesInRange(remoteTaskId: Long, fromDay: Long, toDay: Long): List<Long>

    @Query("DELETE FROM group_task_daily_completions")
    suspend fun deleteAll()
}
