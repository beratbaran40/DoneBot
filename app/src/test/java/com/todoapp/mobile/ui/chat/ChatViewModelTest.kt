package com.todoapp.mobile.ui.chat

import android.os.SystemClock
import com.todoapp.mobile.common.DomainException
import com.todoapp.mobile.data.ai.LocalIntentClassifier
import com.todoapp.mobile.data.model.network.response.ChatMessageResponseData
import com.todoapp.mobile.data.model.network.response.ChatTurnMeta
import com.todoapp.mobile.data.network.BackendWarmUp
import com.todoapp.mobile.data.network.NetworkMonitor
import com.todoapp.mobile.data.repository.DataStoreHelper
import com.todoapp.mobile.domain.repository.ChatRepository
import com.todoapp.mobile.domain.repository.SessionPreferences
import com.todoapp.mobile.domain.repository.TaskSyncRepository
import com.todoapp.mobile.domain.usecase.ComputeHealthPointsUseCase
import com.todoapp.mobile.domain.usecase.HealthPoints
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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
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
    private val computeHealthPoints = mockk<ComputeHealthPointsUseCase>()

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
        every { computeHealthPoints() } returns flowOf(HealthPoints(halfHearts = 13, showDepletionDialog = false))
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
        computeHealthPoints = computeHealthPoints,
    )

    private fun ChatViewModel.sendPrompt(prompt: String) {
        onAction(ChatContract.UiAction.OnDraftChanged(prompt))
        onAction(ChatContract.UiAction.OnSendClicked)
    }

    private fun ChatViewModel.readyState(): ChatContract.UiState.Ready = uiState.value as ChatContract.UiState.Ready

    @Test
    fun `timeout while online surfaces SERVER_WAKING with the failed prompt`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coEvery { chatRepository.sendMessage(any(), any(), any(), any()) } returns
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
        coVerify(exactly = 1) { chatRepository.sendMessage(any(), any(), any(), any()) }
    }

    @Test
    fun `going offline mid-flight surfaces OFFLINE, not SERVER_WAKING`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coEvery { chatRepository.sendMessage(any(), any(), any(), any()) } coAnswers {
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
        coEvery { chatRepository.sendMessage(any(), any(), any(), any()) } returns
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

    // ---- Health points travel with the prompt ----

    @Test
    fun `the current half-heart count is sent with the prompt`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coEvery { chatRepository.sendMessage(any(), any(), any(), any()) } returns Result.success(successResponse())
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.sendPrompt("How am I doing?")
        advanceUntilIdle()

        // The server can't derive hearts — device-local history folded over a persisted checkpoint —
        // so if this stops travelling the bot silently loses the ability to answer about them.
        coVerify { chatRepository.sendMessage(any(), any(), any(), healthHalfHearts = 13) }
    }

    @Test
    fun `a failing health-points read never blocks the send`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        every { computeHealthPoints() } throws IllegalStateException("datastore unavailable")
        coEvery { chatRepository.sendMessage(any(), any(), any(), any()) } returns Result.success(successResponse())
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.sendPrompt("How am I doing?")
        advanceUntilIdle()

        // Degrade to "no health line", never to "no answer".
        coVerify { chatRepository.sendMessage(any(), any(), any(), healthHalfHearts = null) }
        assertFalse(viewModel.readyState().isThinking)
    }

    @Test
    fun `a failing local intent falls through to the backend instead of crashing`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        // tryAnswer runs bare inside viewModelScope, and every intent ends in a .first() on Room or
        // DataStore. Unguarded, a corrupt-DataStore IOException is an uncaught exception in a
        // coroutine with no handler — the app dies when the user taps a suggestion chip.
        coEvery { intentClassifier.tryAnswer(any()) } throws java.io.IOException("datastore corrupt")
        coEvery { chatRepository.sendMessage(any(), any(), any(), any()) } returns Result.success(successResponse())
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.sendPrompt("Kalplerim nasıl?")
        advanceUntilIdle()

        coVerify(exactly = 1) { chatRepository.sendMessage(any(), any(), any(), any()) }
        assertFalse(viewModel.readyState().isThinking)
    }

    // ---- Post-turn task sync follows the server's own verdict ----

    @Test
    fun `a read-only turn does not force a task sync`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coEvery { chatRepository.sendMessage(any(), any(), any(), any()) } returns
            Result.success(successResponse(roundTrips = 2, mutated = false, tools = listOf("getTodaysTasks")))
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.sendPrompt("What's due today?")
        advanceUntilIdle()

        // roundTrips is 2 for every tool turn including pure reads, so the old heuristic cost a full
        // task re-fetch over the network on the single most common question asked.
        coVerify(exactly = 0) { taskSyncRepository.fetchTasks(any()) }
    }

    @Test
    fun `a turn the server says it wrote forces a task sync`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coEvery { chatRepository.sendMessage(any(), any(), any(), any()) } returns
            Result.success(successResponse(roundTrips = 2, mutated = true, tools = listOf("setTaskSchedule")))
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.sendPrompt("Make the vitamin routine every other day")
        advanceUntilIdle()

        coVerify(exactly = 1) { taskSyncRepository.fetchTasks(force = true) }
    }

    @Test
    fun `the server's verdict wins over any tool name the client does not know`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        // The point of moving the decision server-side. A tool the backend added after this client
        // shipped is unrecognisable here, and the client no longer has to recognise it: the server
        // already said the turn wrote. The old client-side list would have gone stale for as long
        // as a Play release takes, and the user would have kept seeing pre-change data.
        coEvery { chatRepository.sendMessage(any(), any(), any(), any()) } returns
            Result.success(successResponse(roundTrips = 2, mutated = true, tools = listOf("setTaskPriority")))
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.sendPrompt("Make it high priority")
        advanceUntilIdle()

        coVerify(exactly = 1) { taskSyncRepository.fetchTasks(force = true) }
    }

    @Test
    fun `a backend that predates the flag falls back to the round-trip heuristic`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        // Null means "this server does not have the field", which must not read as "nothing
        // changed" — so we over-sync rather than risk missing one. A new client against a
        // not-yet-deployed backend stays correct, just chattier.
        coEvery { chatRepository.sendMessage(any(), any(), any(), any()) } returns
            Result.success(successResponse(roundTrips = 2, mutated = null))
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.sendPrompt("Add a meeting tomorrow")
        advanceUntilIdle()

        coVerify(exactly = 1) { taskSyncRepository.fetchTasks(force = true) }
    }

    // ---- Bounded auto-retry chain (only for provably-unprocessed failures) ----

    private fun neverReached() = DomainException.ServerUnreachable("edge 502", requestNeverReachedServer = true)

    private fun successResponse(
        roundTrips: Int = 1,
        mutated: Boolean? = false,
        tools: List<String> = emptyList(),
    ) = ChatMessageResponseData(
        text = "Done!",
        meta = ChatTurnMeta(
            roundTrips = roundTrips,
            refused = false,
            serverMs = 42,
            toolsCalled = tools,
            mutated = mutated,
        ),
    )

    @Test
    fun `persistent connect-refusal auto-retries 3 times then goes quiet`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coEvery { chatRepository.sendMessage(any(), any(), any(), any()) } returns Result.failure(neverReached())
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.sendPrompt("Anything overdue?")
        advanceUntilIdle()

        // Initial send + 3 auto-attempts (5s/15s/30s), then the chain stops for good.
        coVerify(exactly = 4) { chatRepository.sendMessage(any(), any(), any(), any()) }
        val state = viewModel.readyState()
        assertEquals(ChatContract.ChatError.SERVER_WAKING, state.error)
        assertEquals(0, state.autoRetrySecondsRemaining)
        assertEquals("Anything overdue?", state.lastFailedPrompt)
    }

    @Test
    fun `countdown ticks down before the next attempt`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coEvery { chatRepository.sendMessage(any(), any(), any(), any()) } returns Result.failure(neverReached())
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.sendPrompt("Anything overdue?")
        runCurrent()
        assertEquals(5, viewModel.readyState().autoRetrySecondsRemaining)

        advanceTimeBy(2_050)
        runCurrent()
        assertEquals(3, viewModel.readyState().autoRetrySecondsRemaining)
    }

    @Test
    fun `rate limit mid-chain takes over and stops the auto-retry`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coEvery { chatRepository.sendMessage(any(), any(), any(), any()) } returnsMany listOf(
            Result.failure(neverReached()),
            Result.failure(DomainException.Server("DoneBot is very busy (quota reached). [vertex_quota] Retry in 30s")),
        )
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.sendPrompt("Anything overdue?")
        advanceUntilIdle()

        // The second (auto) attempt hit the quota — its cooldown takes over; no third attempt.
        coVerify(exactly = 2) { chatRepository.sendMessage(any(), any(), any(), any()) }
        val state = viewModel.readyState()
        assertEquals(ChatContract.ChatError.RATE_LIMITED, state.error)
        assertEquals(0, state.autoRetrySecondsRemaining)
    }

    @Test
    fun `dismissing the banner mid-countdown cancels the chain`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coEvery { chatRepository.sendMessage(any(), any(), any(), any()) } returns Result.failure(neverReached())
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.sendPrompt("Anything overdue?")
        runCurrent()
        viewModel.onAction(ChatContract.UiAction.OnDismissError)
        advanceUntilIdle()

        coVerify(exactly = 1) { chatRepository.sendMessage(any(), any(), any(), any()) }
        val state = viewModel.readyState()
        assertEquals(null, state.error)
        assertEquals(0, state.autoRetrySecondsRemaining)
    }

    @Test
    fun `success on the second attempt clears the error and resets the chain`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coEvery { chatRepository.sendMessage(any(), any(), any(), any()) } returnsMany listOf(
            Result.failure(neverReached()),
            Result.success(successResponse()),
        )
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.sendPrompt("Anything overdue?")
        advanceUntilIdle()

        coVerify(exactly = 2) { chatRepository.sendMessage(any(), any(), any(), any()) }
        coVerify(exactly = 1) { chatRepository.appendAssistantMessage("Done!") }
        val state = viewModel.readyState()
        assertEquals(null, state.error)
        assertEquals(0, state.autoRetrySecondsRemaining)
        assertFalse(state.isThinking)
    }

    @Test
    fun `a new user prompt supersedes the pending chain`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val prompts = mutableListOf<String>()
        coEvery { chatRepository.sendMessage(capture(prompts), any(), any(), any()) } returnsMany listOf(
            Result.failure(neverReached()),
            Result.success(successResponse()),
        )
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.sendPrompt("first prompt")
        runCurrent()
        viewModel.sendPrompt("second prompt")
        advanceUntilIdle()

        // The old chain never fires: exactly one send per prompt, in order.
        assertEquals(listOf("first prompt", "second prompt"), prompts)
        assertEquals(null, viewModel.readyState().error)
    }
}
