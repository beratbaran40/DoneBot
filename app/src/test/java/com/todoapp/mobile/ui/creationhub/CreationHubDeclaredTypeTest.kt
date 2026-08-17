package com.todoapp.mobile.ui.creationhub

import androidx.lifecycle.SavedStateHandle
import com.todoapp.mobile.domain.engine.PomodoroEngine
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.Task
import com.todoapp.mobile.domain.model.TaskType
import com.todoapp.mobile.domain.repository.GroupRepository
import com.todoapp.mobile.domain.repository.TaskRepository
import com.todoapp.mobile.ui.creationhub.CreationHubContract.TaskScope
import com.todoapp.mobile.ui.creationhub.CreationHubContract.UiAction
import com.todoapp.mobile.util.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * The card the user taps has to survive being saved.
 *
 * It did not: the type was re-derived from the row on every read, and the derivation cannot tell
 * "Custom, repeating between two dates" apart from a plain routine — a scheduled end sits beside the
 * frequency inside one rule rather than being a different shape. So the one combination the custom
 * form makes easiest came back labelled "Routine", every time.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CreationHubDeclaredTypeTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val taskRepository = mockk<TaskRepository>(relaxed = true)

    private fun viewModel(): CreationHubViewModel {
        val groupRepository = mockk<GroupRepository>(relaxed = true)
        every { groupRepository.observeAllGroups() } returns flowOf(emptyList())
        return CreationHubViewModel(
            taskRepository = taskRepository,
            groupRepository = groupRepository,
            pomodoroEngine = mockk<PomodoroEngine>(relaxed = true),
            savedStateHandle = SavedStateHandle(),
        )
    }

    private fun CreationHubViewModel.create(type: TaskType, title: String = "Antibiyotik") {
        onAction(UiAction.OnCreateTaskCardTap)
        onAction(UiAction.OnScopeSelect(TaskScope.PERSONAL))
        onAction(UiAction.OnTypeSelect(type))
        onAction(UiAction.OnTitleChange(title))
    }

    @Test
    fun `a custom task that only repeats within a span is still saved as CUSTOM`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.create(TaskType.CUSTOM)
        // Exactly what the calendar's hold-then-tap gesture produces: a start, a frequency, an end.
        vm.onAction(UiAction.OnFrequencySelect(Recurrence.DAILY))
        vm.onAction(UiAction.OnDateSelect(LocalDate.of(2026, 8, 17)))
        vm.onAction(UiAction.OnRecurrenceUntilSelect(LocalDate.of(2026, 8, 30)))
        vm.onAction(UiAction.OnCreate)
        advanceUntilIdle()

        val saved = slot<Task>()
        coVerify { taskRepository.insert(capture(saved)) }
        assertEquals(TaskType.CUSTOM, saved.captured.declaredType)
        // The data really does look like a routine — that is the whole point of storing the choice.
        assertEquals(Recurrence.DAILY, saved.captured.recurrence)
        assertEquals(LocalDate.of(2026, 8, 30), saved.captured.recurrenceUntil)
    }

    @Test
    fun `every card stamps its own type`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val cards = listOf(TaskType.ONE_TIME, TaskType.ROUTINE, TaskType.CUSTOM)
        cards.forEach { type ->
            val vm = viewModel()
            advanceUntilIdle()
            vm.create(type, title = "Task ${type.name}")
            vm.onAction(UiAction.OnCreate)
            advanceUntilIdle()
        }

        // A list, not a slot: MockK refuses a repeated verify that captures into a slot, because the
        // slot would silently keep only the last call — which is exactly the case a loop like this
        // must not accept.
        val saved = mutableListOf<Task>()
        coVerify { taskRepository.insert(capture(saved)) }
        assertEquals(cards, saved.map { it.declaredType })
    }

    @Test
    fun `a staged task stamps STAGED once it has a step`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        // STAGED is the one type that refuses to save empty, so it needs its own arrangement.
        val vm = viewModel()
        advanceUntilIdle()
        vm.create(TaskType.STAGED, title = "Tez")
        vm.onAction(UiAction.OnSubtaskChange(0, "Giriş"))
        vm.onAction(UiAction.OnCreate)
        advanceUntilIdle()

        val saved = slot<Task>()
        coVerify { taskRepository.insert(capture(saved)) }
        assertEquals(TaskType.STAGED, saved.captured.declaredType)
    }

    @Test
    fun `the photo path stamps the same type as the plain path`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        // insert and insertWithPhotos have drifted apart before — the photo path once skipped the
        // reminder rows and the alarm entirely. One builder feeds both; this pins that it still does.
        val vm = viewModel()
        advanceUntilIdle()
        vm.create(TaskType.CUSTOM)
        vm.onAction(UiAction.OnFrequencySelect(Recurrence.WEEKLY))
        vm.onAction(UiAction.OnPhotoPick(byteArrayOf(1, 2, 3), "image/jpeg"))
        vm.onAction(UiAction.OnCreate)
        advanceUntilIdle()

        val saved = slot<Task>()
        coVerify { taskRepository.insertWithPhotos(capture(saved), any()) }
        assertEquals(TaskType.CUSTOM, saved.captured.declaredType)
    }

    @Test
    fun `switching card before creating stamps the last one tapped`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.create(TaskType.CUSTOM)
        vm.onAction(UiAction.OnTypeSelect(TaskType.ROUTINE))
        vm.onAction(UiAction.OnCreate)
        advanceUntilIdle()

        val saved = slot<Task>()
        coVerify { taskRepository.insert(capture(saved)) }
        assertEquals(TaskType.ROUTINE, saved.captured.declaredType)
    }
}
