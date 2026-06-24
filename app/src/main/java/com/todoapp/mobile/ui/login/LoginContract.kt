package com.todoapp.mobile.ui.login

import androidx.compose.runtime.Immutable

object LoginContract {
    @Immutable
    data class UiState(
        val email: String = "",
        val password: String = "",
        val isPasswordVisible: Boolean = false,
        val emailError: LoginError? = null,
        val passwordError: LoginError? = null,
        val generalError: LoginError? = null,
        val socialOnlyProvider: String? = null,
        val isLoading: Boolean = false,
        val hasSubmittedOnce: Boolean = false,
    )

    sealed interface UiAction {
        data class OnEmailChange(
            val value: String,
        ) : UiAction

        data class OnPasswordChange(
            val value: String,
        ) : UiAction

        data object OnPasswordVisibilityTap : UiAction

        data object OnLoginTap : UiAction

        data object OnForgotPasswordTap : UiAction

        data object OnGoogleSignInTap : UiAction

        data class OnSuccessfulGoogleLogin(
            val token: String,
        ) : UiAction

        data class OnGoogleSignInFailed(
            val message: String,
        ) : UiAction

        data object OnRegisterTap : UiAction
    }

    sealed interface UiEffect {
        data object GoogleLogin : UiEffect

        data class ShowToast(
            val message: String,
        ) : UiEffect
    }

    data class LoginError(
        val message: String,
    )
}
