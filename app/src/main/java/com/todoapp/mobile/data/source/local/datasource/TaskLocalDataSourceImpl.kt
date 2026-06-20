package com.todoapp.mobile.data.source.local.datasource

import com.todoapp.mobile.data.model.entity.SubtaskEntity
import com.todoapp.mobile.data.model.entity.TaskEntity
import com.todoapp.mobile.data.source.local.DayCount
import com.todoapp.mobile.data.source.local.SubtaskCount
import com.todoapp.mobile.data.source.local.SubtaskDao
import com.todoapp.mobile.data.source.local.TaskDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TaskLocalDataSourceImpl
@Inject
constructor(
    private val taskDao: TaskDao,
    private val subtaskDao: SubtaskDao,
) : TaskLocalDataSource {
    override fun observeAll(): Flow<List<TaskEntity>> = taskDao.getAllTasks()

    override fun observeRange(
        startDate: Long,
        endDate: Long,
    ): Flow<List<TaskEntity>> = taskDao.loadTasksBetweenRange(startDate, endDate)

    override fun observeByDate(date: Long): Flow<List<TaskEntity>> = taskDao.getTasksByDate(date = date)

    override fun countInRange(
        startDate: Long,
        endDate: Long,
        isCompleted: Boolean,
    ): Flow<Int> = taskDao.getTaskCountInRange(
        startDate = startDate,
        endDate = endDate,
        isCompleted = isCompleted,
    )

    override fun observeCompletedCountsByDay(
        startDate: Long,
        endDate: Long,
    ): Flow<List<DayCount>> = taskDao.observeCompletedCountsByDay(startDate, endDate)

    override fun observePendingCountsByDay(
        startDate: Long,
        endDate: Long,
    ): Flow<List<DayCount>> = taskDao.observePendingCountsByDay(startDate, endDate)

    override suspend fun insert(task: TaskEntity): Long = taskDao.insert(task)

    override suspend fun delete(task: TaskEntity) {
        taskDao.delete(task)
    }

    override suspend fun update(task: TaskEntity) {
        taskDao.update(task)
    }

    override suspend fun updateTaskCompletion(
        id: Long,
        isCompleted: Boolean,
    ) {
        taskDao.updateTask(id, isCompleted)
    }

    override suspend fun getTaskById(id: Long): TaskEntity? = taskDao.getTaskById(id)

    override suspend fun deleteAll() {
        taskDao.deleteAllTasks()
    }

    override suspend fun insertAll(tasks: List<TaskEntity>) {
        taskDao.insertAll(tasks)
    }

    override fun observeByWeekAndStatus(
        startDate: Long,
        endDate: Long,
        isCompleted: Boolean,
    ): Flow<List<TaskEntity>> = taskDao.observeTasksByWeekAndStatus(startDate, endDate, isCompleted)

    override suspend fun updateOrderIndex(
        id: Long,
        orderIndex: Int,
    ) {
        taskDao.updateOrderIndex(id = id, orderIndex = orderIndex)
    }

    override suspend fun updateOrderIndices(orderUpdates: List<Pair<Long, Int>>) {
        for ((id, orderIndex) in orderUpdates) {
            taskDao.updateOrderIndex(id = id, orderIndex = orderIndex)
        }
    }

    override fun search(query: String): Flow<List<TaskEntity>> = taskDao.searchTasks(query)

    override suspend fun deleteByRemoteIds(remoteIds: List<Long>) {
        taskDao.deleteByRemoteIds(remoteIds)
    }

    override fun observeByRecurrence(recurrence: String): Flow<List<TaskEntity>> = taskDao.observeByRecurrence(recurrence)

    override fun observeAllRecurringTasks(): Flow<List<TaskEntity>> = taskDao.observeAllRecurringTasks()

    override fun observeOverdueTasks(today: Long): Flow<List<TaskEntity>> = taskDao.observeOverdueTasks(today)

    override suspend fun shiftDatesByOneDay(taskIds: List<Long>) = taskDao.shiftDatesByOneDay(taskIds)

    override fun observeSubtasks(taskId: Long): Flow<List<SubtaskEntity>> = subtaskDao.observeByTask(taskId)

    override suspend fun getSubtasks(taskId: Long): List<SubtaskEntity> = subtaskDao.getByTask(taskId)

    override suspend fun getSubtaskById(id: Long): SubtaskEntity? = subtaskDao.getById(id)

    override suspend fun countSubtasks(taskId: Long): Int = subtaskDao.countByTask(taskId)

    override suspend fun insertSubtask(subtask: SubtaskEntity): Long = subtaskDao.insert(subtask)

    override suspend fun insertSubtasks(subtasks: List<SubtaskEntity>) = subtaskDao.insertAll(subtasks)

    override suspend fun updateSubtask(subtask: SubtaskEntity) = subtaskDao.update(subtask)

    override suspend fun deleteSubtask(subtask: SubtaskEntity) = subtaskDao.delete(subtask)

    override suspend fun deleteSubtasksByTask(taskId: Long) = subtaskDao.deleteByTask(taskId)

    override fun observeSubtaskCounts(): Flow<List<SubtaskCount>> = subtaskDao.observeSubtaskCounts()
}
