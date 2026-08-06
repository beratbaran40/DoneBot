package com.todoapp.mobile.ui.update

import app.cash.turbine.test
import com.todoapp.mobile.domain.update.AppUpdateChecker
import com.todoapp.mobile.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The behaviour worth pinning here is the one that is easiest to "fix" into something wrong: the
 * dismissal is per-launch. Nothing about it is written down anywhere, so a well-meaning change that
 * persists it to DataStore would look like an improvement and would quietly turn one "Not now" into
 * a user who is never told about an update again.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppUpdateViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val checker = mockk<AppUpdateChecker>()

    private fun viewModel() = AppUpdateViewModel(checker)

    @Test
    fun `with nothing to install the dialog stays away`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coEvery { checker.isUpdateAvailable() } returns false

        val vm = viewModel()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isDialogVisible)
    }

    @Test
    fun `an available update opens the dialog`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coEvery { checker.isUpdateAvailable() } returns true

        val vm = viewModel()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isDialogVisible)
    }

    @Test
    fun `not now closes it without handing off to Play`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coEvery { checker.isUpdateAvailable() } returns true
        val vm = viewModel()
        advanceUntilIdle()

        vm.onAction(AppUpdateContract.UiAction.OnDismiss)

        assertFalse(vm.uiState.value.isDialogVisible)
        vm.uiEffect.test { expectNoEvents() }
    }

    @Test
    fun `update closes the dialog and hands off to Play`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coEvery { checker.isUpdateAvailable() } returns true
        val vm = viewModel()
        advanceUntilIdle()

        vm.onAction(AppUpdateContract.UiAction.OnUpdateClick)

        // Closed before the hand-off, so backing out of the Play screen does not land the user back
        // on a dialog they already answered.
        assertFalse(vm.uiState.value.isDialogVisible)
        vm.uiEffect.test {
            assertEquals(AppUpdateContract.UiEffect.LaunchUpdateFlow, awaitItem())
        }
    }

    @Test
    fun `a dismissal does not outlive the launch it happened in`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coEvery { checker.isUpdateAvailable() } returns true

        val firstLaunch = viewModel()
        advanceUntilIdle()
        firstLaunch.onAction(AppUpdateContract.UiAction.OnDismiss)
        assertFalse(firstLaunch.uiState.value.isDialogVisible)

        // A new ViewModel is what the next cold start produces. The prompt has to come back: the
        // whole point is a user who keeps postponing eventually updating.
        val nextLaunch = viewModel()
        advanceUntilIdle()

        assertTrue(nextLaunch.uiState.value.isDialogVisible)
        coVerify(exactly = 2) { checker.isUpdateAvailable() }
    }
}
