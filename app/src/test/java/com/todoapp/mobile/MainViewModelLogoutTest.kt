package com.todoapp.mobile

import android.content.Context
import com.todoapp.mobile.data.repository.DataStoreHelper
import com.todoapp.mobile.domain.engine.PomodoroEngine
import com.todoapp.mobile.domain.repository.AuthEvent
import com.todoapp.mobile.domain.repository.AuthRepository
import com.todoapp.mobile.domain.repository.ChatRepository
import com.todoapp.mobile.domain.repository.GroupRepository
import com.todoapp.mobile.domain.repository.JournalRepository
import com.todoapp.mobile.domain.repository.PendingPhotoRepository
import com.todoapp.mobile.domain.repository.SessionPreferences
import com.todoapp.mobile.domain.repository.TaskRepository
import com.todoapp.mobile.domain.repository.TaskSyncRepository
import com.todoapp.mobile.domain.repository.UserRepository
import com.todoapp.mobile.navigation.CurrentRouteTracker
import com.todoapp.mobile.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * §7.12 orchestration guard. Proves [MainViewModel.clearLocalSession] — the single logout/account-switch
 * wipe path, triggered by [AuthEvent.Logout] — invokes EVERY per-user clear, including the two DataStore
 * keys (`pending_chat_prompt`, `chat_draft`) that `clearUser()` does not touch and that would otherwise
 * leak user A's blocked prompt/unsent draft into user B's session on the same device.
 *
 * This is the fast (pure-JVM/MockK) half of §7.12; the real-DB cascade + owner-scoping proof lives in the
 * instrumented AccountSwitchIsolationTest. If a future clear is added to the wipe but forgotten here, or a
 * clear is dropped, this test goes red.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelLogoutTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context = mockk<Context>(relaxed = true)
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val sessionPreferences = mockk<SessionPreferences>(relaxed = true)
    private val taskRepository = mockk<TaskRepository>(relaxed = true)
    private val groupRepository = mockk<GroupRepository>(relaxed = true)
    private val dataStoreHelper = mockk<DataStoreHelper>(relaxed = true)
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val pomodoroEngine = mockk<PomodoroEngine>(relaxed = true)
    private val taskSyncRepository = mockk<TaskSyncRepository>(relaxed = true)
    private val currentRouteTracker = mockk<CurrentRouteTracker>(relaxed = true)
    private val pendingPhotoRepository = mockk<PendingPhotoRepository>(relaxed = true)
    private val chatRepository = mockk<ChatRepository>(relaxed = true)
    private val journalRepository = mockk<JournalRepository>(relaxed = true)

    private fun buildViewModel(events: SharedFlow<AuthEvent>): MainViewModel {
        // Trigger source + the flows the init collectors subscribe to. observeUser MUST be empty so the
        // Crashlytics setUserId collector never touches FirebaseCrashlytics.getInstance() on the JVM.
        every { authRepository.events } returns events
        every { sessionPreferences.observeRefreshToken() } returns emptyFlow()
        every { dataStoreHelper.observeUser() } returns emptyFlow()
        // Result-returning suspend calls — relaxed can't synthesize a value-class Result, so stub them.
        coEvery { userRepository.getUserInfo() } returns Result.failure(RuntimeException("no network in test"))
        coEvery { userRepository.deleteFcmToken() } returns Result.success(Unit)
        coEvery { taskRepository.deleteAllTasks() } returns Result.success(Unit)
        coEvery { groupRepository.deleteAllLocalGroups() } returns Result.success(Unit)
        return MainViewModel(
            context = context,
            authRepository = authRepository,
            sessionPreferences = sessionPreferences,
            taskRepository = taskRepository,
            groupRepository = groupRepository,
            dataStoreHelper = dataStoreHelper,
            userRepository = userRepository,
            pomodoroEngine = pomodoroEngine,
            taskSyncRepository = taskSyncRepository,
            currentRouteTracker = currentRouteTracker,
            pendingPhotoRepository = pendingPhotoRepository,
            chatRepository = chatRepository,
            journalRepository = journalRepository,
            analyticsHelper = mockk(relaxed = true),
        )
    }

    @Test
    fun `logout event wipes every per-user local store including chat prompt and draft`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        // replay = 1 so the init collector still receives Logout even though it subscribes after emit.
        val events = MutableSharedFlow<AuthEvent>(replay = 1)
        events.tryEmit(AuthEvent.Logout)

        buildViewModel(events)
        advanceUntilIdle()

        coVerify(exactly = 1) { userRepository.deleteFcmToken() }
        coVerify(exactly = 1) { sessionPreferences.clear() }
        coVerify(exactly = 1) { taskRepository.deleteAllTasks() }
        coVerify(exactly = 1) { groupRepository.deleteAllLocalGroups() }
        coVerify(exactly = 1) { pomodoroEngine.finish() }
        coVerify(exactly = 1) { dataStoreHelper.clearUser() }
        coVerify(exactly = 1) { taskSyncRepository.resetCooldown() }
        coVerify(exactly = 1) { pendingPhotoRepository.clearAll() }
        coVerify(exactly = 1) { chatRepository.clear() }
        // The §7.12 leak fix: the two keys clearUser() leaves behind.
        coVerify(exactly = 1) { dataStoreHelper.setPendingChatPrompt("") }
        coVerify(exactly = 1) { dataStoreHelper.setChatDraft("") }
    }
}
