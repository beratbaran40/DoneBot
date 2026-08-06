package com.todoapp.mobile.ui.details

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.Task
import com.todoapp.mobile.domain.repository.PendingPhotoRepository
import com.todoapp.mobile.domain.repository.TaskRepository
import com.todoapp.mobile.domain.usecase.SetTaskCompletionUseCase
import com.todoapp.mobile.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * `isDirty` is the only thing standing between an edit and the Save button, and it is derived from a
 * field-by-field diff. Every field the diff forgets is a field the user can change on screen and
 * never keep — the control responds, and Save stays greyed out with nothing explaining why.
 *
 * The extended repeat rule was in exactly that state: saved by buildUpdatedTask, invisible to the
 * dirty check. These tests pin each of those four fields.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DetailsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val taskRepository = mockk<TaskRepository>(relaxed = true)
    private val pendingPhotoRepository = mockk<PendingPhotoRepository>(relaxed = true)
    private val setTaskCompletion = mockk<SetTaskCompletionUseCase>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)

    private val routine = Task(
        id = TASK_ID,
        title = "Vitamin",
        description = null,
        date = LocalDate.of(2026, 8, 6),
        timeStart = LocalTime.of(8, 0),
        timeEnd = LocalTime.of(8, 30),
        isCompleted = false,
        isSecret = false,
        recurrence = Recurrence.WEEKLY,
        recurrenceInterval = 1,
        recurrenceByDay = setOf(DayOfWeek.MONDAY),
        recurrenceUntil = LocalDate.of(2026, 9, 6),
        reminderTimes = listOf(LocalTime.of(8, 0)),
    )

    @Before
    fun setUp() {
        coEvery { taskRepository.getTaskById(TASK_ID) } returns routine
        coEvery { taskRepository.getReminderTimes(TASK_ID) } returns routine.reminderTimes
        every { taskRepository.observeSubtasks(any()) } returns flowOf(emptyList())
        every { taskRepository.observeSubtasksForDay(any(), any()) } returns flowOf(emptyList())
    }

    private fun viewModel() = DetailsViewModel(
        taskRepository = taskRepository,
        pendingPhotoRepository = pendingPhotoRepository,
        setTaskCompletion = setTaskCompletion,
        context = context,
        savedStateHandle = SavedStateHandle(mapOf("taskId" to TASK_ID)),
    )

    private fun DetailsViewModel.success() = uiState.value as DetailsContract.UiState.Success

    @Test
    fun `a freshly loaded task is not dirty`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val vm = viewModel()
        advanceUntilIdle()

        // The baseline the rest of these depend on: if loading alone reported a change, Save would be
        // lit before the user touched anything and every assertion below would be meaningless.
        assertFalse(vm.success().isDirty)
    }

    @Test
    fun `changing the repeat interval enables save`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onAction(DetailsContract.UiAction.OnIntervalChange(3))
        advanceUntilIdle()

        assertTrue("interval change must mark the form dirty", vm.success().isDirty)
    }

    @Test
    fun `changing the weekday set enables save`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onAction(DetailsContract.UiAction.OnWeekdayToggle(DayOfWeek.FRIDAY))
        advanceUntilIdle()

        assertTrue("weekday change must mark the form dirty", vm.success().isDirty)
    }

    @Test
    fun `changing the end date enables save`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onAction(DetailsContract.UiAction.OnRecurrenceUntilChange(LocalDate.of(2026, 12, 31)))
        advanceUntilIdle()

        assertTrue("end-date change must mark the form dirty", vm.success().isDirty)
    }

    @Test
    fun `moving the start past the end drops the end instead of crossing them`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val vm = viewModel()
        advanceUntilIdle()

        // Routine runs 6 Aug → 6 Sep. Push the start to December.
        vm.onAction(DetailsContract.UiAction.OnDialogDateSelect(LocalDate.of(2026, 12, 1)))
        advanceUntilIdle()

        // firesOn rejects every day before the anchor and every day after `until`, so a crossed
        // pair leaves nothing: the task saves, syncs, and appears on no day anywhere.
        assertNull(vm.success().recurrenceUntil)
    }

    @Test
    fun `an end still ahead of the new start is kept`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onAction(DetailsContract.UiAction.OnDialogDateSelect(LocalDate.of(2026, 8, 20)))
        advanceUntilIdle()

        // The guard is about a crossing, not about touching the date at all.
        assertEquals(LocalDate.of(2026, 9, 6), vm.success().recurrenceUntil)
    }

    @Test
    fun `adding a reminder time enables save`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onAction(DetailsContract.UiAction.OnReminderTimeAdd(LocalTime.of(20, 0)))
        advanceUntilIdle()

        assertTrue("reminder-time change must mark the form dirty", vm.success().isDirty)
    }

    private companion object {
        const val TASK_ID = 42L
    }
}
