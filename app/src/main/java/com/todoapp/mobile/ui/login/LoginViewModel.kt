package com.todoapp.mobile.ui.login

import android.content.Context
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.todoapp.mobile.R
import com.todoapp.mobile.common.DomainException
import com.todoapp.mobile.common.passwordValidation.ValidationManager
import com.todoapp.mobile.data.auth.AuthModel
import com.todoapp.mobile.data.auth.AuthTokenManager
import com.todoapp.mobile.data.model.network.data.AuthResponseData
import com.todoapp.mobile.data.model.network.request.LoginRequest
import com.todoapp.mobile.data.repository.DataStoreHelper
import com.todoapp.mobile.domain.repository.ChatRepository
import com.todoapp.mobile.domain.repository.SessionPreferences
import com.todoapp.mobile.domain.repository.TaskSyncRepository
import com.todoapp.mobile.domain.repository.UserRepository
import com.todoapp.mobile.navigation.NavigationEffect
import com.todoapp.mobile.navigation.Screen
import com.todoapp.mobile.ui.login.LoginContract.UiAction
import com.todoapp.mobile.ui.login.LoginContract.UiEffect
import com.todoapp.mobile.ui.login.LoginContract.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel
@Inject
constructor(
    private val userRepository: UserRepository,
    private val authTokenManager: AuthTokenManager,
    private val sessionPreferences: SessionPreferences,
    private val dataStoreHelper: DataStoreHelper,
    private val taskSyncRepository: TaskSyncRepository,
    private val chatRepository: ChatRepository,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val redirectAfterLogin: String? = savedStateHandle.toRoute<Screen.Login>().redirectAfterLogin

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<UiEffect>()
    val uiEffect = _uiEffect.receiveAsFlow()

    private val _navEffect = Channel<NavigationEffect>(Channel.BUFFERED)
    val navEffect = _navEffect.receiveAsFlow()

    init {
        Log.d("LoginViewModel", "redirectAfterLogin = $redirectAfterLogin")
    }

    fun onAction(uiAction: UiAction) {
        when (uiAction) {
            is UiAction.OnEmailChange -> onEmailChange(uiAction.value)
            is UiAction.OnPasswordChange -> onPasswordChange(uiAction.value)
            UiAction.OnForgotPasswordTap -> navigateToForgotPassword()
            is UiAction.OnGoogleSignInTap -> _uiEffect.trySend(UiEffect.GoogleLogin)
            is UiAction.OnGoogleSignInFailed -> _uiEffect.trySend(UiEffect.ShowToast(uiAction.message))
            is UiAction.OnSuccessfulGoogleLogin -> googleLogin(uiAction.token)
            UiAction.OnLoginTap -> handleLoginClick()
            UiAction.OnPasswordVisibilityTap -> togglePasswordVisibility()
            UiAction.OnRegisterTap -> navigateToRegister()
        }
    }

    private fun googleLogin(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            userRepository
                .googleLogin(idToken)
                .onSuccess { loginData ->
                    authTokenManager.saveTokens(
                        AuthModel(
                            accessToken = loginData.accessToken,
                            refreshToken = loginData.refreshToken,
                            userId = loginData.user.id,
                            email = loginData.user.email,
                            displayName = loginData.user.displayName,
                            avatarUrl = loginData.user.avatarUrl,
                        ),
                    )
                    handleSuccessfulLogin(loginData)
                }.onFailure { error ->
                    Log.e("GOOGLE_LOGIN", "Token retrieval FAILED", error)
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEffect.trySend(UiEffect.ShowToast(error.message ?: "Google login error"))
                }
        }
    }

    private fun login() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, socialOnlyProvider = null) }

        userRepository
            .login(
                request =
                LoginRequest(
                    email = uiState.value.email,
                    password = uiState.value.password,
                ),
            ).fold(
                onSuccess = { loginData ->
                    _uiState.update { it.copy(isLoading = false) }
                    handleSuccessfulLogin(loginData)
                    userRepository
                        .syncPendingFcmToken()
                        .onFailure { Log.d("FCM_SYNC", "syncPendingFcmToken failed: ${it.message}") }
                },
                onFailure = { throwable ->
                    _uiState.update { it.copy(isLoading = false) }
                    when (throwable) {
                        is DomainException.OAuthAccountExists ->
                            _uiState.update {
                                it.copy(
                                    socialOnlyProvider = throwable.provider ?: "social",
                                    emailError = null,
                                    passwordError = null,
                                    generalError = null,
                                )
                            }

                        is DomainException.Server ->
                            _uiState.update {
                                it.copy(
                                    emailError = LoginContract.LoginError(throwable.message ?: "Try again later"),
                                )
                            }

                        else ->
                            _uiState.update {
                                it.copy(
                                    generalError = LoginContract.LoginError(throwable.message ?: "Try again later"),
                                )
                            }
                    }
                },
            )
    }

    private fun navigateToRegister() {
        _navEffect.trySend(
            NavigationEffect.Navigate(
                route = Screen.Register(redirectAfterRegister = redirectAfterLogin),
            ),
        )
    }

    private fun navigateToForgotPassword() {
        _navEffect.trySend(
            NavigationEffect.Navigate(
                route = Screen.ForgotPassword,
            ),
        )
    }

    private fun handleLoginClick() {
        val currentState = uiState.value
        val emailError = ValidationManager.validateEmail(currentState.email).toLocalizedError()
        val passwordError = ValidationManager.validatePassword(currentState.password).toLocalizedError()

        if (emailError == null && passwordError == null) {
            login()
        } else {
            _uiState.update { current ->
                current.copy(
                    emailError = emailError?.let { LoginContract.LoginError(it) },
                    passwordError = passwordError?.let { LoginContract.LoginError(it) },
                    isLoading = false,
                    hasSubmittedOnce = true,
                )
            }
        }
    }

    private fun handleSuccessfulLogin(loginResponseData: AuthResponseData) {
        viewModelScope.launch {
            // Defensive: if a different user signs in (e.g. logout was interrupted by a
            // process kill before chat was wiped), purge the previous user's DoneBot history
            // so it never leaks into this session. Read before setUser overwrites the cache.
            val previousUserId = dataStoreHelper.observeUser().first()?.id
            if (previousUserId != null && previousUserId != loginResponseData.user.id) {
                chatRepository.clear()
            }
            sessionPreferences.setAccessToken(loginResponseData.accessToken)
            sessionPreferences.setRefreshToken(loginResponseData.refreshToken)
            sessionPreferences.setExpiresAt(loginResponseData.expiresIn)
            dataStoreHelper.setUser(userData = loginResponseData.user)
            dataStoreHelper.setFirstLoginPermissionPromptPending(true)

            taskSyncRepository.fetchTasks(force = true)

            kotlinx.coroutines.yield()

            val destination = resolveRedirectDestination()
            if (redirectAfterLogin != null) {
                _navEffect.send(
                    NavigationEffect.Navigate(
                        route = destination,
                        popUpTo = Screen.Home,
                        isInclusive = false,
                    ),
                )
            } else {
                _navEffect.send(
                    NavigationEffect.Navigate(
                        route = destination,
                        popUpTo = Screen.Onboarding,
                        isInclusive = true,
                    ),
                )
            }
        }
    }

    private fun resolveRedirectDestination(): Screen {
        val redirect = redirectAfterLogin ?: return Screen.Home
        return when {
            redirect.contains("CreateNewGroup") -> Screen.CreateNewGroup
            redirect.contains("Groups") -> Screen.Groups()
            redirect.contains("Chat") -> Screen.Chat
            else -> Screen.Home
        }
    }

    private fun onEmailChange(email: String) {
        _uiState.update { current ->
            current.copy(
                email = email,
                socialOnlyProvider = null,
                emailError =
                if (current.hasSubmittedOnce) {
                    ValidationManager
                        .validateEmail(email)
                        .toLocalizedError()
                        ?.let { LoginContract.LoginError(it) }
                } else {
                    null
                },
            )
        }
    }

    private fun onPasswordChange(password: String) {
        _uiState.update { current ->
            current.copy(
                password = password,
                passwordError =
                if (current.hasSubmittedOnce) {
                    ValidationManager
                        .validatePassword(password)
                        .toLocalizedError()
                        ?.let { LoginContract.LoginError(it) }
                } else {
                    null
                },
            )
        }
    }

    private fun String.toLocalizedError(): String? {
        if (this.isEmpty()) return null

        return when (this) {
            ValidationManager.ValidationErrors.EMAIL_BLANK ->
                context.getString(R.string.error_email_blank)

            ValidationManager.ValidationErrors.EMAIL_INVALID ->
                context.getString(R.string.error_email_invalid)

            ValidationManager.ValidationErrors.PASSWORD_BLANK ->
                context.getString(R.string.error_password_blank)

            ValidationManager.ValidationErrors.PASSWORD_MIN_LENGTH ->
                context.getString(
                    R.string.error_password_min_length,
                    ValidationManager.PasswordRules.MIN_LENGTH,
                )

            else -> this
        }
    }

    private fun togglePasswordVisibility() {
        val state = uiState.value
        _uiState.update { it.copy(isPasswordVisible = !state.isPasswordVisible) }
    }
}
