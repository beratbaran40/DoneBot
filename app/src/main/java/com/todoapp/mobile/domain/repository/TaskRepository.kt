package com.todoapp.mobile.domain.repository

import com.todoapp.mobile.data.model.entity.TaskEntity
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.Subtask
import com.todoapp.mobile.domain.model.Task
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class CompletedCountByDay(
    val date: LocalDate,
    val count: Int,
)

data class MonthlyWeekBucket(
    val weekIndex: Int,
    val rangeStart: LocalDate,
    val rangeEnd: LocalDate,
    val completed: Int,
    val pending: Int,
)

data class DailyBucket(
    val date: LocalDate,
    val completed: Int,
    val pending: Int,
)

// Aggregate-root repository: tasks + recurrence + per-day completion + sync + staged subtasks.
@Suppress("TooManyFunctions")
interface TaskRepository {
    /** Map of remote task id -> list of photo URLs. Populated on every remote sync. */
    fun observeTaskPhotoUrls(): Flow<Map<Long, List<String>>>

    fun observeRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<Task>>

    fun observeTasksByDate(date: LocalDate, includeRecurringInstances: Boolean = true): Flow<List<Task>>

    fun countCompletedTasksInAWeek(date: LocalDate, includeRecurring: Boolean = true): Flow<Int>

    fun countCompletedCountsByDayInAWeek(date: LocalDate, includeRecurring: Boolean = true): Flow<List<CompletedCountByDay>>

    fun observeCompletedCountsByDayInAWeek(date: LocalDate, includeRecurring: Boolean = true): Flow<List<Int>>

    fun observePendingCountsByDayInAWeek(date: LocalDate, includeRecurring: Boolean = true): Flow<List<Int>>

    fun observeCompletedCountsByDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
        includeRecurring: Boolean = true,
    ): Flow<Map<LocalDate, Int>>

    fun observeMonthlyWeekBuckets(
        monthStart: LocalDate,
        includeRecurring: Boolean = true,
    ): Flow<List<MonthlyWeekBucket>>

    fun countCompletedTasksInAMonth(
        monthStart: LocalDate,
        includeRecurring: Boolean = true,
    ): Flow<Int>

    fun observeDailyBucketsByDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
        includeRecurring: Boolean = true,
    ): Flow<List<DailyBucket>>

    fun observePendingTasksInAWeek(date: LocalDate, includeRecurring: Boolean = true): Flow<Int>

    fun countCompletedTasksYearToDate(date: LocalDate): Flow<Int>

    fun observePendingTasksYearToDate(date: LocalDate): Flow<Int>

    suspend fun insert(task: Task)

    suspend fun insertWithPhotos(
        task: Task,
        photos: List<Pair<ByteArray, String>>,
    ): Result<Unit>

    suspend fun delete(task: Task)

    suspend fun updateTaskCompletion(
        id: Long,
        isCompleted: Boolean,
    )

    /** Per-instance completion for any recurring task (recurrence != NONE). */
    suspend fun setInstanceCompletion(
        taskId: Long,
        date: LocalDate,
        completed: Boolean,
    )

    /**
     * Retire / un-retire a whole recurring routine from the Recurring tab. [finishedOn] = the day it
     * was finished (the routine stops firing on later days and shows completed); null = resume it.
     * No-op for non-recurring tasks.
     */
    suspend fun setRoutineFinished(
        taskId: Long,
        finishedOn: LocalDate?,
    )

    /** All tasks of a given recurrence type, ordered by anchor date then start time. */
    fun observeRecurringByType(recurrence: Recurrence): Flow<List<Task>>

    /** One-shot tasks dated before [today] that are still incomplete. */
    fun observeOverdueTasks(today: LocalDate): Flow<List<Task>>

    /** Bulk-shifts the dates of [taskIds] forward by one day. */
    suspend fun deferTasksToTomorrow(taskIds: List<Long>)

    suspend fun getTaskById(id: Long): Task?

    suspend fun fetchRemoteTask(id: Long): Result<Task>

    suspend fun refreshPhotoUrls(taskRemoteIds: List<Long>)

    suspend fun uploadTaskPhoto(
        taskId: Long,
        bytes: ByteArray,
        mimeType: String,
    ): Result<String>

    suspend fun deleteTaskPhoto(
        taskId: Long,
        photoId: Long,
    ): Result<Unit>

    suspend fun update(task: Task)

    suspend fun syncRemoteTasksWithLocal(): Result<Unit>

    suspend fun syncLocalTasksToServer(): Result<Unit>

    suspend fun findNonSyncedTasks(): List<TaskEntity>

    fun observeAllTaskEntities(): Flow<List<TaskEntity>>

    fun observeAllTasks(): Flow<List<Task>>

    suspend fun syncTask(taskEntity: TaskEntity): Result<Unit>

    suspend fun deleteAllTasks(): Result<Unit>

    suspend fun getAllTasks(): Result<Unit>

    suspend fun reorderTasksForDate(
        date: LocalDate,
        fromIndex: Int,
        toIndex: Int,
    ): Result<Unit>

    fun observeTasksByWeekAndStatus(
        date: LocalDate,
        isCompleted: Boolean,
    ): Flow<List<Task>>

    fun searchTasks(query: String): Flow<List<Task>>

    // --- Staged-task subtasks (personal tasks only) ---

    /** Observe the ordered steps ("adımlar") of a staged task. */
    fun observeSubtasks(taskId: Long): Flow<List<Subtask>>

    /** Append a new step; reopens the parent if it had been auto-completed. */
    suspend fun addSubtask(taskId: Long, title: String)

    /** Toggle a step; recomputes the parent's completion per the staged invariant. */
    suspend fun toggleSubtask(subtaskId: Long, isCompleted: Boolean)

    /** Delete a step; no-op when it is the last one (a staged task keeps ≥1 step). */
    suspend fun deleteSubtask(subtaskId: Long)

    /** Rename a step; completion is untouched so the parent invariant is unaffected. */
    suspend fun updateSubtaskTitle(subtaskId: Long, title: String)
}
