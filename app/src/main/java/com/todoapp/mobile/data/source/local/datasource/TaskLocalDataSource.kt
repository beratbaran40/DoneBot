package com.todoapp.mobile.data.source.local.datasource

import com.todoapp.mobile.data.model.entity.SubtaskEntity
import com.todoapp.mobile.data.model.entity.TaskEntity
import com.todoapp.mobile.data.source.local.DayCount
import com.todoapp.mobile.data.source.local.SubtaskCount
import kotlinx.coroutines.flow.Flow

interface TaskLocalDataSource {
    fun observeAll(): Flow<List<TaskEntity>>

    fun observeRange(
        startDate: Long,
        endDate: Long,
    ): Flow<List<TaskEntity>>

    fun observeByDate(date: Long): Flow<List<TaskEntity>>

    fun countInRange(
        startDate: Long,
        endDate: Long,
        isCompleted: Boolean,
    ): Flow<Int>

    fun observeCompletedCountsByDay(
        startDate: Long,
        endDate: Long,
    ): Flow<List<DayCount>>

    fun observePendingCountsByDay(
        startDate: Long,
        endDate: Long,
    ): Flow<List<DayCount>>

    suspend fun insert(task: TaskEntity): Long

    suspend fun delete(task: TaskEntity)

    suspend fun update(task: TaskEntity)

    suspend fun updateTaskCompletion(
        id: Long,
        isCompleted: Boolean,
    )

    suspend fun getTaskById(id: Long): TaskEntity?

    suspend fun deleteAll()

    suspend fun insertAll(tasks: List<TaskEntity>)

    fun observeByWeekAndStatus(
        startDate: Long,
        endDate: Long,
        isCompleted: Boolean,
    ): Flow<List<TaskEntity>>

    suspend fun updateOrderIndex(
        id: Long,
        orderIndex: Int,
    )

    suspend fun updateOrderIndices(orderUpdates: List<Pair<Long, Int>>)

    fun search(query: String): Flow<List<TaskEntity>>

    suspend fun deleteByRemoteIds(remoteIds: List<Long>)

    fun observeByRecurrence(recurrence: String): Flow<List<TaskEntity>>

    fun observeAllRecurringTasks(): Flow<List<TaskEntity>>

    fun observeOverdueTasks(today: Long): Flow<List<TaskEntity>>

    suspend fun shiftDatesByOneDay(taskIds: List<Long>)

    // --- Subtasks (child of the personal Task aggregate) ---

    fun observeSubtasks(taskId: Long): Flow<List<SubtaskEntity>>

    suspend fun getSubtasks(taskId: Long): List<SubtaskEntity>

    suspend fun getSubtaskById(id: Long): SubtaskEntity?

    suspend fun countSubtasks(taskId: Long): Int

    suspend fun insertSubtask(subtask: SubtaskEntity): Long

    suspend fun insertSubtasks(subtasks: List<SubtaskEntity>)

    suspend fun updateSubtask(subtask: SubtaskEntity)

    suspend fun deleteSubtask(subtask: SubtaskEntity)

    fun observeSubtaskCounts(): Flow<List<SubtaskCount>>
}
