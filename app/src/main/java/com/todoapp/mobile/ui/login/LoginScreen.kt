package com.todoapp.mobile.ui.login

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import com.todoapp.mobile.R
import com.todoapp.mobile.data.auth.GoogleSignInManager
import com.todoapp.mobile.ui.auth.AuthScaffold
import com.todoapp.mobile.ui.common.SecureScreenEffect
import com.todoapp.mobile.ui.login.LoginContract.UiAction
import com.todoapp.mobile.ui.login.LoginContract.UiEffect
import com.todoapp.mobile.ui.login.LoginContract.UiState
import com.todoapp.uikit.extensions.collectWithLifecycle
import com.todoapp.uikit.theme.TDTheme
import kotlinx.coroutines.flow.Flow

@Composable
fun LoginScreen(
    uiState: UiState,
    uiEffect: Flow<UiEffect>,
    onAction: (UiAction) -> Unit,
) {
    SecureScreenEffect()
    val context = LocalContext.current

    uiEffect.collectWithLifecycle(minActiveState = Lifecycle.State.CREATED) {
        when (it) {
            UiEffect.GoogleLogin -> {
                GoogleSignInManager
                    .getGoogleIdToken(context)
                    .onSuccess { idToken ->
                        onAction(UiAction.OnSuccessfulGoogleLogin(idToken))
                    }.onFailure { error ->
                        onAction(UiAction.OnGoogleSignInFailed(error.message ?: "Sign-in Cancelled"))
                    }
            }
            is UiEffect.ShowToast -> Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
        }
    }

    LoginContent(uiState = uiState, onAction = onAction)
}

@Composable
private fun LoginContent(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
) {
    AuthScaffold(
        brandTitle = stringResource(R.string.login_header),
        brandSubtitle = stringResource(R.string.elevate_your_productivity),
    ) {
        LoginFormPanel(uiState = uiState, onAction = onAction)
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun LoginContentPreview() {
    TDTheme {
        LoginContent(
            uiState =
            UiState(
                email = "name@example.com",
                password = "ExamplePassword123",
                isPasswordVisible = true,
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun LoginContentDarkPreview() {
    TDTheme {
        LoginContent(
            uiState =
            UiState(
                email = "name@example.com",
                password = "ExamplePassword123",
                isPasswordVisible = false,
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun LoginContentEmptyPreview() {
    TDTheme {
        LoginContent(
            uiState = UiState(),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun LoginContentEmailErrorPreview() {
    TDTheme {
        LoginContent(
            uiState =
            UiState(
                email = "not-an-email",
                emailError = LoginContract.LoginError("Please enter a valid email"),
                hasSubmittedOnce = true,
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun LoginContentPasswordErrorPreview() {
    TDTheme {
        LoginContent(
            uiState =
            UiState(
                email = "name@example.com",
                password = "abc",
                passwordError = LoginContract.LoginError("Password must be at least 8 characters"),
                hasSubmittedOnce = true,
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun LoginContentGeneralErrorPreview() {
    TDTheme {
        LoginContent(
            uiState =
            UiState(
                email = "name@example.com",
                password = "ExamplePassword123",
                generalError = LoginContract.LoginError("Invalid email or password"),
                hasSubmittedOnce = true,
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun LoginContentSocialOnlyPreview() {
    TDTheme {
        LoginContent(
            uiState =
            UiState(
                email = "name@example.com",
                socialOnlyProvider = "google",
                hasSubmittedOnce = true,
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun LoginContentLoadingPreview() {
    TDTheme {
        LoginContent(
            uiState =
            UiState(
                email = "name@example.com",
                password = "ExamplePassword123",
                isLoading = true,
            ),
            onAction = {},
        )
    }
}
