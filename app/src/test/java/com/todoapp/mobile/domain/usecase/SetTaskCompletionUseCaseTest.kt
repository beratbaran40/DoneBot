package com.todoapp.mobile.domain.usecase

import com.todoapp.mobile.domain.analytics.AnalyticsHelper
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.Task
import com.todoapp.mobile.domain.repository.TaskRepository
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Pins the §7.2 completion invariant: recurring tasks must toggle their per-day row
 * (setInstanceCompletion) and NEVER the base isCompleted (updateTaskCompletion), which would stick
 * true forever. Non-recurring tasks take the base-row path. Pure JVM — no Robolectric needed.
 */
class SetTaskCompletionUseCaseTest {
    private val repository = mockk<TaskRepository>(relaxed = true)
    private val analyticsHelper = mockk<AnalyticsHelper>(relaxed = true)
    private val useCase = SetTaskCompletionUseCase(repository, analyticsHelper)

    @Test
    fun `completing a recurring task hits the per-day path, never the base row`() = runTest {
        useCase(task(id = 7L, recurrence = Recurrence.DAILY), completed = true)

        coVerify(exactly = 1) {
            repository.setInstanceCompletion(taskId = 7L, date = DAY, completed = true)
        }
        coVerify(exactly = 0) { repository.updateTaskCompletion(any(), any()) }
    }

    @Test
    fun `un-completing a recurring task still hits the per-day path`() = runTest {
        useCase(task(id = 7L, recurrence = Recurrence.WEEKLY), completed = false)

        coVerify(exactly = 1) {
            repository.setInstanceCompletion(taskId = 7L, date = DAY, completed = false)
        }
        coVerify(exactly = 0) { repository.updateTaskCompletion(any(), any()) }
    }

    @Test
    fun `completing a non-recurring task writes the base row`() = runTest {
        useCase(task(id = 3L, recurrence = Recurrence.NONE), completed = true)

        coVerify(exactly = 1) { repository.updateTaskCompletion(id = 3L, isCompleted = true) }
        coVerify(exactly = 0) { repository.setInstanceCompletion(any(), any(), any()) }
    }

    @Test
    fun `completing a task logs the task_completed analytics event`() = runTest {
        useCase(task(id = 3L, recurrence = Recurrence.NONE), completed = true)

        verify(exactly = 1) { analyticsHelper.logTaskCompleted() }
    }

    @Test
    fun `un-completing a task does not log task_completed`() = runTest {
        useCase(task(id = 7L, recurrence = Recurrence.DAILY), completed = false)

        verify(exactly = 0) { analyticsHelper.logTaskCompleted() }
    }

    private companion object {
        val DAY: LocalDate = LocalDate.of(2026, 7, 4)

        fun task(
            id: Long,
            recurrence: Recurrence,
        ) = Task(
            id = id,
            title = "task",
            description = null,
            date = DAY,
            timeStart = LocalTime.of(9, 0),
            timeEnd = LocalTime.of(10, 0),
            isCompleted = false,
            isSecret = false,
            recurrence = recurrence,
        )
    }
}
