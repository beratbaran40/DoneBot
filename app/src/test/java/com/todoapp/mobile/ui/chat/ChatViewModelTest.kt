package com.todoapp.mobile.ui.chat

import android.os.SystemClock
import com.todoapp.mobile.common.DomainException
import com.todoapp.mobile.data.ai.LocalIntentClassifier
import com.todoapp.mobile.data.network.BackendWarmUp
import com.todoapp.mobile.data.network.NetworkMonitor
import com.todoapp.mobile.data.repository.DataStoreHelper
import com.todoapp.mobile.domain.repository.ChatRepository
import com.todoapp.mobile.domain.repository.SessionPreferences
import com.todoapp.mobile.domain.repository.TaskSyncRepository
import com.todoapp.mobile.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Locks the chat error taxonomy for backend cold starts / deploy windows: a timeout while the
 * device is online must surface as SERVER_WAKING ("server is waking up"), never as OFFLINE — the
 * misleading "you're offline" message was the top closed-test complaint. Backend-enveloped Vertex
 * outages (503 + vertex_unavailable marker) must keep their dedicated SERVER_UNAVAILABLE banner.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val onlineFlow = MutableStateFlow(true)
    private val chatRepository = mockk<ChatRepository>(relaxed = true)
    private val networkMonitor = mockk<NetworkMonitor> { every { isOnline } returns onlineFlow }
    private val dataStoreHelper = mockk<DataStoreHelper>(relaxed = true)
    private val intentClassifier = mockk<LocalIntentClassifier>()
    private val taskSyncRepository = mockk<TaskSyncRepository>(relaxed = true)
    private val sessionPreferences = mockk<SessionPreferences>()
    private val backendWarmUp = mockk<BackendWarmUp>(relaxed = true)

    private var fakeElapsedMs = 0L

    @Before
    fun setUp() {
        // SystemClock is an android.jar stub that returns 0 on the JVM; a constant clock would trip
        // the 3s send cooldown on the very first send. Advance 10s per read so every gate passes.
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } answers {
            fakeElapsedMs += 10_000L
            fakeElapsedMs
        }
        onlineFlow.value = true
        coEvery { chatRepository.getMessages() } returns emptyList()
        every { chatRepository.observeMessages() } returns flowOf(emptyList())
        every { dataStoreHelper.observeChatDraft() } returns flowOf("")
        coEvery { dataStoreHelper.getPendingChatPrompt() } returns ""
        coEvery { intentClassifier.tryAnswer(any()) } returns null
        coEvery { sessionPreferences.getAccessToken() } returns "token"
    }

    @After
    fun tearDown() {
        unmockkStatic(SystemClock::class)
    }

    private fun buildViewModel(): ChatViewModel = ChatViewModel(
        chatRepository = chatRepository,
        networkMonitor = networkMonitor,
        dataStoreHelper = dataStoreHelper,
        intentClassifier = intentClassifier,
        taskSyncRepository = taskSyncRepository,
        sessionPreferences = sessionPreferences,
        analyticsHelper = mockk(relaxed = true),
        backendWarmUp = backendWarmUp,
    )

    private fun ChatViewModel.sendPrompt(prompt: String) {
        onAction(ChatContract.UiAction.OnDraftChanged(prompt))
        onAction(ChatContract.UiAction.OnSendClicked)
    }

    private fun ChatViewModel.readyState(): ChatContract.UiState.Ready = uiState.value as ChatContract.UiState.Ready

    @Test
    fun `timeout while online surfaces SERVER_WAKING with the failed prompt`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coEvery { chatRepository.sendMessage(any(), any(), any()) } returns
            Result.failure(DomainException.ServerUnreachable("timeout", requestNeverReachedServer = false))
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.sendPrompt("Anything overdue?")
        advanceUntilIdle()

        val state = viewModel.readyState()
        assertEquals(ChatContract.ChatError.SERVER_WAKING, state.error)
        assertEquals("Anything overdue?", state.lastFailedPrompt)
        assertFalse(state.isThinking)
        // Timeouts are never auto-resent: the server may have processed the request after the
        // client stopped waiting, and a resend could double-run the chat's tool writes.
        coVerify(exactly = 1) { chatRepository.sendMessage(any(), any(), any()) }
    }

    @Test
    fun `going offline mid-flight surfaces OFFLINE, not SERVER_WAKING`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coEvery { chatRepository.sendMessage(any(), any(), any()) } coAnswers {
            onlineFlow.value = false
            Result.failure(DomainException.ServerUnreachable("timeout", requestNeverReachedServer = false))
        }
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.sendPrompt("Anything overdue?")
        advanceUntilIdle()

        assertEquals(ChatContract.ChatError.OFFLINE, viewModel.readyState().error)
    }

    @Test
    fun `backend vertex_unavailable 503 keeps its dedicated SERVER_UNAVAILABLE banner`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coEvery { chatRepository.sendMessage(any(), any(), any()) } returns
            Result.failure(DomainException.Server("[vertex_unavailable] AI is temporarily unavailable"))
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.sendPrompt("Anything overdue?")
        advanceUntilIdle()

        assertEquals(ChatContract.ChatError.SERVER_UNAVAILABLE, viewModel.readyState().error)
    }

    @Test
    fun `opening chat fires a backend warm-up ping`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        buildViewModel()
        advanceUntilIdle()

        coVerify(exactly = 1) { backendWarmUp.pingIfStale(any()) }
    }
}
