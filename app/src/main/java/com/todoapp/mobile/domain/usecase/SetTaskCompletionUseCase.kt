package com.todoapp.mobile.domain.usecase

import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.Task
import com.todoapp.mobile.domain.repository.TaskRepository
import javax.inject.Inject

/**
 * Single source of truth for toggling a task's completion, so every surface (Home, FilteredTasks,
 * Search, …) treats recurring tasks identically.
 *
 * Recurring tasks track completion per-day in `task_daily_completions` — their base `isCompleted`
 * flag is meaningless and must never be written, otherwise it sticks `true` forever and the task
 * can no longer be un-done. Non-recurring (incl. staged) tasks use the base-row update path, which
 * also handles the staged parent snapshot/cascade.
 *
 * The instance date is taken from [Task.date]: list queries stamp recurring instances with the day
 * they are displayed under, so toggling completes the correct occurrence.
 */
class SetTaskCompletionUseCase
@Inject
constructor(
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(
        task: Task,
        completed: Boolean,
    ) {
        if (task.recurrence != Recurrence.NONE) {
            taskRepository.setInstanceCompletion(
                taskId = task.id,
                date = task.date,
                completed = completed,
            )
        } else {
            taskRepository.updateTaskCompletion(
                id = task.id,
                isCompleted = completed,
            )
        }
    }
}
