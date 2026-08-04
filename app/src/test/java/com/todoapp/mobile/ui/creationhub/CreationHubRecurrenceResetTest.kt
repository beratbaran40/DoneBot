package com.todoapp.mobile.ui.creationhub

import androidx.lifecycle.SavedStateHandle
import com.todoapp.mobile.domain.alarm.AlarmScheduler
import com.todoapp.mobile.domain.engine.PomodoroEngine
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.repository.DailyPlanPreferences
import com.todoapp.mobile.domain.repository.GroupRepository
import com.todoapp.mobile.domain.repository.TaskRepository
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
            alarmScheduler = mockk<AlarmScheduler>(relaxed = true),
            dailyPlanPreferences = mockk<DailyPlanPreferences>(relaxed = true),
            pomodoroEngine = mockk<PomodoroEngine>(relaxed = true),
            ioDispatcher = mainDispatcherRule.dispatcher,
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
}
