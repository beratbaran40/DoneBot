package com.todoapp.mobile.data.source.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.todoapp.mobile.data.model.entity.GroupSubtaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupSubtaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<GroupSubtaskEntity>)

    @Query("SELECT * FROM group_subtasks WHERE remote_task_id = :remoteTaskId ORDER BY order_index ASC")
    suspend fun getByTask(remoteTaskId: Long): List<GroupSubtaskEntity>

    @Query("SELECT * FROM group_subtasks WHERE remote_task_id = :remoteTaskId ORDER BY order_index ASC")
    fun observeByTask(remoteTaskId: Long): Flow<List<GroupSubtaskEntity>>

    @Query("SELECT * FROM group_subtasks WHERE remote_task_id IN (:remoteTaskIds) ORDER BY order_index ASC")
    fun observeByTasks(remoteTaskIds: List<Long>): Flow<List<GroupSubtaskEntity>>

    @Query("SELECT * FROM group_subtasks WHERE remote_task_id IN (:remoteTaskIds) ORDER BY order_index ASC")
    suspend fun getByTasks(remoteTaskIds: List<Long>): List<GroupSubtaskEntity>

    @Query("DELETE FROM group_subtasks WHERE remote_task_id = :remoteTaskId")
    suspend fun deleteByTask(remoteTaskId: Long)

    /**
     * Replaces a task's steps in one transaction so a refresh never renders a half-empty checklist.
     * Not @Transaction-annotated on purpose — it is a default method, and Room only weaves
     * transactions into abstract ones; callers run it inside the repository's own transaction.
     */
    suspend fun replaceForTask(remoteTaskId: Long, entities: List<GroupSubtaskEntity>) {
        deleteByTask(remoteTaskId)
        if (entities.isNotEmpty()) insertAll(entities)
    }

    @Query("DELETE FROM group_subtasks")
    suspend fun deleteAll()
}
