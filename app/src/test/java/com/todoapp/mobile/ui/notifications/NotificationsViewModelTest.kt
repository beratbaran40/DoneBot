package com.todoapp.mobile.ui.notifications

import android.content.Context
import app.cash.turbine.test
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.model.Notification
import com.todoapp.mobile.domain.model.NotificationType
import com.todoapp.mobile.domain.repository.InvitationRepository
import com.todoapp.mobile.domain.repository.NotificationRepository
import com.todoapp.mobile.ui.notifications.NotificationsContract.UiAction
import com.todoapp.mobile.ui.notifications.NotificationsContract.UiEffect
import com.todoapp.mobile.ui.notifications.NotificationsContract.UiState
import com.todoapp.mobile.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val notificationsFlow = MutableStateFlow(sampleItems())
    private val repository = mockk<NotificationRepository>(relaxed = true) {
        every { notifications } returns notificationsFlow
        coEvery { refresh(any()) } returns Result.success(Unit)
    }
    private val invitationRepository = mockk<InvitationRepository>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)

    private fun buildViewModel() = NotificationsViewModel(
        repository = repository,
        invitationRepository = invitationRepository,
        context = context,
    )

    @Test
    fun `mark all read opens undo window without calling repository`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onAction(UiAction.OnMarkAllRead)
        runCurrent()

        val state = viewModel.uiState.value as UiState.Success
        assertTrue(state.pendingMarkAllRead)
        coVerify(exactly = 0) { repository.markAllRead() }
    }

    @Test
    fun `undo within window cancels commit entirely`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onAction(UiAction.OnMarkAllRead)
        runCurrent()
        viewModel.onAction(UiAction.OnUndoMarkAllRead)
        advanceUntilIdle()

        val state = viewModel.uiState.value as UiState.Success
        assertFalse(state.pendingMarkAllRead)
        coVerify(exactly = 0) { repository.markAllRead() }
    }

    @Test
    fun `window expiry commits mark all read exactly once`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onAction(UiAction.OnMarkAllRead)
        advanceUntilIdle()

        val state = viewModel.uiState.value as UiState.Success
        assertFalse(state.pendingMarkAllRead)
        coVerify(exactly = 1) { repository.markAllRead() }
    }

    @Test
    fun `failed commit emits failure toast`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coEvery { repository.markAllRead() } returns Result.failure(RuntimeException("boom"))
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onAction(UiAction.OnMarkAllRead)
            advanceUntilIdle()
            assertEquals(UiEffect.ShowToast(R.string.notifications_mark_all_failed), awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `mark all read is a no-op when nothing is unread`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        notificationsFlow.value = sampleItems().map { it.copy(isRead = true) }
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onAction(UiAction.OnMarkAllRead)
        advanceUntilIdle()

        val state = viewModel.uiState.value as UiState.Success
        assertFalse(state.pendingMarkAllRead)
        coVerify(exactly = 0) { repository.markAllRead() }
    }

    private companion object {
        fun sampleItems() = listOf(
            Notification(
                id = 1,
                type = NotificationType.TASK_DUE_SOON,
                title = "Due soon",
                body = "Pay electricity bill is due in 1 hour",
                payload = mapOf("taskTitle" to "Pay electricity bill"),
                isRead = false,
                createdAt = 1_700_000_000_000,
            ),
            Notification(
                id = 2,
                type = NotificationType.TASK_COMPLETED,
                title = "Task completed",
                body = "Ayse completed Submit weekly report",
                payload = mapOf("actorName" to "Ayse", "taskTitle" to "Submit weekly report"),
                isRead = true,
                createdAt = 1_700_000_000_000,
            ),
        )
    }
}
