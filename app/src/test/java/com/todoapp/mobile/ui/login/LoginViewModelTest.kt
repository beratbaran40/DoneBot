package com.todoapp.mobile.ui.login

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.todoapp.mobile.data.repository.DataStoreHelper
import com.todoapp.mobile.domain.repository.ChatRepository
import com.todoapp.mobile.domain.repository.SessionPreferences
import com.todoapp.mobile.domain.repository.TaskSyncRepository
import com.todoapp.mobile.domain.repository.UserRepository
import com.todoapp.mobile.ui.login.LoginContract.UiAction
import com.todoapp.mobile.util.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val sessionPreferences = mockk<SessionPreferences>(relaxed = true)
    private val dataStoreHelper = mockk<DataStoreHelper>(relaxed = true)
    private val taskSyncRepository = mockk<TaskSyncRepository>(relaxed = true)
    private val chatRepository = mockk<ChatRepository>(relaxed = true)
    private val context = mockk<Context>()

    private fun buildViewModel(): LoginViewModel {
        // Any string resource lookup (validation errors) resolves to a non-null placeholder.
        every { context.getString(any<Int>()) } returns "validation error"
        return LoginViewModel(
            userRepository = userRepository,
            sessionPreferences = sessionPreferences,
            dataStoreHelper = dataStoreHelper,
            taskSyncRepository = taskSyncRepository,
            chatRepository = chatRepository,
            analyticsHelper = mockk(relaxed = true),
            // toRoute<Screen.Login>() is inline+reified, so it cannot be mocked — feed a real handle.
            savedStateHandle = SavedStateHandle(mapOf("redirectAfterLogin" to null)),
            context = context,
        )
    }

    // Slice-1 canary: proves mockk + turbine + coroutines-test wire up. Blank input must short-circuit
    // before any network call — the ViewModel sets a field error and never touches the repository.
    // (A blank email fails the first validation rule, so it never reaches android.util.Patterns —
    //  the valid-email path DOES touch Patterns and needs Robolectric; covered under §7.2.)
    @Test
    fun `blank email on login tap sets emailError and does not call repository`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val viewModel = buildViewModel()

        viewModel.onAction(UiAction.OnEmailChange(""))
        viewModel.onAction(UiAction.OnPasswordChange("password123"))
        viewModel.onAction(UiAction.OnLoginTap)

        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.emailError)
            assertTrue(state.hasSubmittedOnce)
            cancelAndConsumeRemainingEvents()
        }

        coVerify(exactly = 0) { userRepository.login(any()) }
    }
}
