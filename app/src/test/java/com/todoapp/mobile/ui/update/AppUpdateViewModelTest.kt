package com.todoapp.mobile.ui.update

import app.cash.turbine.test
import com.todoapp.mobile.domain.update.AppUpdateChecker
import com.todoapp.mobile.domain.update.AppUpdateFlowStarter
import com.todoapp.mobile.domain.update.AppUpdateStatus
import com.todoapp.mobile.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
 *
 * The resume cases guard the other end of the same rule: the check retries when the app comes back,
 * which must not turn into re-asking a user who already answered.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppUpdateViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val checker = mockk<AppUpdateChecker>()
    private val flowStarter = mockk<AppUpdateFlowStarter>(relaxed = true)

    private fun viewModel() = AppUpdateViewModel(checker, flowStarter)

    @Test
    fun `with nothing to install the dialog stays away`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coEvery { checker.check() } returns AppUpdateStatus.NotAvailable

        val vm = viewModel()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isDialogVisible)
    }

    @Test
    fun `an available update opens the dialog`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coEvery { checker.check() } returns AppUpdateStatus.Available

        val vm = viewModel()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isDialogVisible)
    }

    @Test
    fun `not now closes it without handing off to Play`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coEvery { checker.check() } returns AppUpdateStatus.Available
        val vm = viewModel()
        advanceUntilIdle()

        vm.onAction(AppUpdateContract.UiAction.OnDismiss)

        assertFalse(vm.uiState.value.isDialogVisible)
        vm.uiEffect.test { expectNoEvents() }
    }

    @Test
    fun `update closes the dialog and hands off to Play`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coEvery { checker.check() } returns AppUpdateStatus.Available
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
        coEvery { checker.check() } returns AppUpdateStatus.Available

        val firstLaunch = viewModel()
        advanceUntilIdle()
        firstLaunch.onAction(AppUpdateContract.UiAction.OnDismiss)
        assertFalse(firstLaunch.uiState.value.isDialogVisible)

        // A new ViewModel is what the next cold start produces. The prompt has to come back: the
        // whole point is a user who keeps postponing eventually updating.
        val nextLaunch = viewModel()
        advanceUntilIdle()

        assertTrue(nextLaunch.uiState.value.isDialogVisible)
        coVerify(exactly = 2) { checker.check() }
    }

    @Test
    fun `a check that could not reach Play is retried when the app comes back`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        // Launched with no network, then the user comes back to the app on wifi.
        coEvery { checker.check() } returnsMany listOf(AppUpdateStatus.Unknown, AppUpdateStatus.Available)
        val vm = viewModel()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isDialogVisible)

        vm.onAction(AppUpdateContract.UiAction.OnAppResumed)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isDialogVisible)
        coVerify(exactly = 2) { checker.check() }
    }

    @Test
    fun `a definitive no is not asked again on resume`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coEvery { checker.check() } returns AppUpdateStatus.NotAvailable
        val vm = viewModel()
        advanceUntilIdle()

        vm.onAction(AppUpdateContract.UiAction.OnAppResumed)
        advanceUntilIdle()

        // The common case by far. Re-polling Play on every foreground would be pure waste.
        coVerify(exactly = 1) { checker.check() }
    }

    @Test
    fun `a dismissal survives a resume`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coEvery { checker.check() } returns AppUpdateStatus.Available
        val vm = viewModel()
        advanceUntilIdle()
        vm.onAction(AppUpdateContract.UiAction.OnDismiss)

        vm.onAction(AppUpdateContract.UiAction.OnAppResumed)
        advanceUntilIdle()

        // "Not now" has to mean not now for the whole launch. Backgrounding the app is not an answer,
        // and re-nagging on every foreground is how a dialog earns an uninstall.
        assertFalse(vm.uiState.value.isDialogVisible)
    }

    @Test
    fun `tapping update does not bring the dialog back on resume`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coEvery { checker.check() } returns AppUpdateStatus.Available
        val vm = viewModel()
        advanceUntilIdle()
        vm.onAction(AppUpdateContract.UiAction.OnUpdateClick)

        // Coming back from Play's screen is a resume like any other.
        vm.onAction(AppUpdateContract.UiAction.OnAppResumed)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isDialogVisible)
    }

    @Test
    fun `an update Play already started is resumed rather than re-offered`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coEvery { checker.check() } returns AppUpdateStatus.InProgress

        val vm = viewModel()
        advanceUntilIdle()

        // No dialog: the user already consented, the download is underway, and Play needs the app
        // to re-enter the flow rather than ask again.
        assertFalse(vm.uiState.value.isDialogVisible)
        vm.uiEffect.test {
            assertEquals(AppUpdateContract.UiEffect.ResumeUpdateFlow, awaitItem())
        }
    }

    @Test
    fun `the first resume does not stack a second check on the cold-start one`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coEvery { checker.check() } coAnswers {
            delay(1_000)
            AppUpdateStatus.NotAvailable
        }

        val vm = viewModel()
        // ON_RESUME fires moments after init{}, while the cold-start check is still in flight.
        vm.onAction(AppUpdateContract.UiAction.OnAppResumed)
        advanceUntilIdle()

        coVerify(exactly = 1) { checker.check() }
    }
}
