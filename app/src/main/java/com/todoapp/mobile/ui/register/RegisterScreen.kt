package com.todoapp.mobile.ui.register

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import com.todoapp.mobile.R
import com.todoapp.mobile.data.auth.GoogleSignInManager
import com.todoapp.mobile.ui.auth.AuthScaffold
import com.todoapp.mobile.ui.common.SecureScreenEffect
import com.todoapp.mobile.ui.register.RegisterContract.UiAction
import com.todoapp.mobile.ui.register.RegisterContract.UiEffect
import com.todoapp.mobile.ui.register.RegisterContract.UiState
import com.todoapp.uikit.extensions.collectWithLifecycle
import com.todoapp.uikit.theme.TDTheme
import kotlinx.coroutines.flow.Flow

@Composable
fun RegisterScreen(
    uiState: UiState,
    uiEffect: Flow<UiEffect>,
    onAction: (UiAction) -> Unit,
) {
    SecureScreenEffect()
    val context = LocalContext.current
    uiEffect.collectWithLifecycle(minActiveState = Lifecycle.State.CREATED) {
        when (it) {
            UiEffect.LaunchGoogleSignIn -> {
                GoogleSignInManager
                    .getGoogleIdToken(context)
                    .onSuccess { token -> onAction(UiAction.OnGoogleSignInResult(token)) }
                    .onFailure { error ->
                        onAction(UiAction.OnGoogleSignInFailed(error.message ?: "Sign-in cancelled"))
                    }
            }
            is UiEffect.ShowToast -> Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        RegisterContent(uiState = uiState, onAction = onAction)

        if (uiState.isRedirecting) {
            Box(
                modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { },
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun RegisterContent(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
) {
    AuthScaffold(
        brandTitle = stringResource(R.string.create_account),
        brandSubtitle = stringResource(R.string.join_us_and_start_organizing_your_tasks_efficiently),
    ) {
        RegisterFormPanel(uiState = uiState, onAction = onAction)
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun RegisterContentPreview() {
    TDTheme {
        RegisterContent(
            uiState =
            UiState(
                fullName = "",
                email = "natalia@example.com",
                password = "password",
                confirmPassword = "password",
                isPasswordVisible = true,
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun RegisterContentDarkPreview() {
    TDTheme {
        RegisterContent(
            uiState =
            UiState(
                fullName = "Natalia Smith",
                email = "natalia@example.com",
                password = "password123",
                confirmPassword = "password123",
                isPasswordVisible = false,
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun RegisterContentEmptyPreview() {
    TDTheme {
        RegisterContent(uiState = UiState(), onAction = {})
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun RegisterContentWeakPasswordPreview() {
    TDTheme {
        RegisterContent(
            uiState =
            UiState(
                fullName = "Natalia",
                email = "natalia@example.com",
                password = "abc",
                confirmPassword = "abc",
                passwordStrength = com.todoapp.mobile.common.passwordValidation.PasswordStrength.WEAK,
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun RegisterContentMediumPasswordPreview() {
    TDTheme {
        RegisterContent(
            uiState =
            UiState(
                fullName = "Natalia",
                email = "natalia@example.com",
                password = "Pass1234",
                confirmPassword = "Pass1234",
                passwordStrength = com.todoapp.mobile.common.passwordValidation.PasswordStrength.MEDIUM,
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun RegisterContentStrongPasswordPreview() {
    TDTheme {
        RegisterContent(
            uiState =
            UiState(
                fullName = "Natalia Smith",
                email = "natalia@example.com",
                password = "Str0ng!Pass#9876",
                confirmPassword = "Str0ng!Pass#9876",
                passwordStrength = com.todoapp.mobile.common.passwordValidation.PasswordStrength.STRONG,
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun RegisterContentEmailErrorPreview() {
    TDTheme {
        RegisterContent(
            uiState =
            UiState(
                fullName = "Natalia",
                email = "not-email",
                emailError = RegisterContract.RegisterError("Please enter a valid email"),
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun RegisterContentMismatchPasswordPreview() {
    TDTheme {
        RegisterContent(
            uiState =
            UiState(
                fullName = "Natalia",
                email = "natalia@example.com",
                password = "password123",
                confirmPassword = "different",
                confirmPasswordError = RegisterContract.RegisterError("Passwords do not match"),
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun RegisterContentRedirectingPreview() {
    TDTheme {
        RegisterContent(
            uiState =
            UiState(
                fullName = "Natalia Smith",
                email = "natalia@example.com",
                password = "Str0ng!Pass#9876",
                confirmPassword = "Str0ng!Pass#9876",
                isRedirecting = true,
            ),
            onAction = {},
        )
    }
}
