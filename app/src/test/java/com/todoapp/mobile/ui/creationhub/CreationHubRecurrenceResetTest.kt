package com.todoapp.mobile.ui.creationhub

import androidx.lifecycle.SavedStateHandle
import com.todoapp.mobile.domain.engine.PomodoroEngine
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.repository.GroupRepository
import com.todoapp.mobile.domain.repository.TaskRepository
import com.todoapp.mobile.ui.creationhub.CreationHubContract.TaskType
import com.todoapp.mobile.ui.creationhub.CreationHubContract.UiAction
import com.todoapp.mobile.util.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * The custom form hides the recurrence-rule fields (end date, interval, weekdays, absolute reminder
 * times) as soon as the frequency is "doesn't repeat". Anything left behind is invisible to the user
 * but still live in state — the calendar kept drawing a span nobody could edit, and the edit form
 * wrote the stale end date onto the saved task.
 *
 * Switching TYPE is the same hazard by a second route: the routine form shows none of those fields,
 * so a rule furnished in the custom form used to ride across and be saved by a routine that never
 * displayed it. Moving the start date is a third — an end left behind it can never fire.
 */
class CreationHubRecurrenceResetTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(): CreationHubViewModel {
        val groupRepository = mockk<GroupRepository>()
        every { groupRepository.observeAllGroups() } returns flowOf(emptyList())
        return CreationHubViewModel(
            taskRepository = mockk<TaskRepository>(relaxed = true),
            groupRepository = groupRepository,
            pomodoroEngine = mockk<PomodoroEngine>(relaxed = true),
            savedStateHandle = SavedStateHandle(),
        )
    }

    private fun CreationHubViewModel.fillRule() {
        onAction(UiAction.OnFrequencySelect(Recurrence.WEEKLY))
        onAction(UiAction.OnIntervalChange(3))
        onAction(UiAction.OnWeekdayToggle(DayOfWeek.MONDAY))
        onAction(UiAction.OnRecurrenceUntilSelect(LocalDate.of(2026, 11, 9)))
        onAction(UiAction.OnReminderTimeAdd(LocalTime.of(9, 0)))
    }

    @Test
    fun `switching to doesn't repeat clears every recurrence-rule field`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.fillRule()

        vm.onAction(UiAction.OnFrequencySelect(Recurrence.NONE))
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(Recurrence.NONE, state.recurrence)
        assertNull("a one-off task has no end date", state.recurrenceUntil)
        assertEquals("interval falls back to the default", 1, state.recurrenceInterval)
        assertTrue("weekdays are meaningless without WEEKLY", state.recurrenceByDay.isEmpty())
        assertTrue("absolute reminder times belong to routines", state.reminderTimes.isEmpty())
    }

    @Test
    fun `switching between two repeating frequencies keeps the rule intact`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        // Only the NONE transition resets. Going WEEKLY -> DAILY must not silently drop the end date
        // the user already picked, since that field stays on screen.
        val vm = viewModel()
        advanceUntilIdle()
        vm.fillRule()

        vm.onAction(UiAction.OnFrequencySelect(Recurrence.DAILY))
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(Recurrence.DAILY, state.recurrence)
        assertEquals(LocalDate.of(2026, 11, 9), state.recurrenceUntil)
        assertEquals(3, state.recurrenceInterval)
        assertEquals(listOf(LocalTime.of(9, 0)), state.reminderTimes)
    }

    @Test
    fun `picking a type clears the rule the previous form furnished`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        // The routine form shows none of these. Carried across, the end date was still written by
        // buildTask — a routine bound by something the user was never shown and could not remove.
        val vm = viewModel()
        advanceUntilIdle()
        vm.onAction(UiAction.OnTypeSelect(TaskType.CUSTOM))
        vm.fillRule()

        vm.onAction(UiAction.OnTypeSelect(TaskType.ROUTINE))
        advanceUntilIdle()

        val state = vm.state.value
        assertNull("an end date belongs to the form that offered it", state.recurrenceUntil)
        assertEquals(1, state.recurrenceInterval)
        assertTrue(state.recurrenceByDay.isEmpty())
        assertTrue(state.reminderTimes.isEmpty())
    }

    @Test
    fun `a routine picked after the custom form still repeats`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        // CUSTOM parks the frequency on NONE. The routine chips have no NONE option, so carrying it
        // over left every chip dark and saved a "routine" that was really a one-off.
        val vm = viewModel()
        advanceUntilIdle()
        vm.onAction(UiAction.OnTypeSelect(TaskType.CUSTOM))
        advanceUntilIdle()
        assertEquals(Recurrence.NONE, vm.state.value.recurrence)

        vm.onAction(UiAction.OnTypeSelect(TaskType.ROUTINE))
        advanceUntilIdle()

        assertEquals(Recurrence.DAILY, vm.state.value.recurrence)
    }

    @Test
    fun `moving the start past the end drops the end`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        // firesOn rejects every day after the end and every day before the anchor; crossed over, the
        // two leave no day at all and the task is saved invisible.
        val vm = viewModel()
        advanceUntilIdle()
        vm.fillRule()

        vm.onAction(UiAction.OnDateSelect(LocalDate.of(2026, 12, 1)))
        advanceUntilIdle()

        assertNull("an end behind the start can never fire", vm.state.value.recurrenceUntil)
    }

    @Test
    fun `moving the start within the span keeps the end`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.fillRule()

        vm.onAction(UiAction.OnDateSelect(LocalDate.of(2026, 10, 1)))
        advanceUntilIdle()

        assertEquals(LocalDate.of(2026, 11, 9), vm.state.value.recurrenceUntil)
    }
}
