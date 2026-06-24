package com.todoapp.mobile.ui.login

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import com.todoapp.mobile.R
import com.todoapp.mobile.data.auth.GoogleSignInManager
import com.todoapp.mobile.ui.login.LoginContract.UiAction
import com.todoapp.mobile.ui.login.LoginContract.UiEffect
import com.todoapp.mobile.ui.login.LoginContract.UiState
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.extensions.collectWithLifecycle
import com.todoapp.uikit.theme.TDTheme
import kotlinx.coroutines.flow.Flow

@Composable
fun LoginScreen(
    uiState: UiState,
    uiEffect: Flow<UiEffect>,
    onAction: (UiAction) -> Unit,
) {
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
            is UiEffect.ShowToast -> {}
        }
    }

    LoginContent(uiState = uiState, onAction = onAction)
}

@Composable
private fun LoginContent(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
) {
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    if (isPortrait) {
        LoginPortraitContent(uiState = uiState, onAction = onAction)
    } else {
        LoginLandscapeContent(uiState = uiState, onAction = onAction)
    }
}

@Composable
private fun LoginPortraitContent(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(color = TDTheme.colors.pendingGray)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Spacer(Modifier.height(32.dp))
        Box(
            modifier =
            Modifier
                .size(70.dp)
                .background(
                    color = TDTheme.colors.background.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(16.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_logo),
                contentDescription = stringResource(R.string.logo),
                modifier = Modifier.size(40.dp),
                tint = TDTheme.colors.white,
            )
        }
        Spacer(Modifier.height(12.dp))
        TDText(
            text = stringResource(R.string.login_header),
            style = TDTheme.typography.heading1,
            color = TDTheme.colors.white,
        )
        TDText(
            text = stringResource(R.string.elevate_your_productivity),
            style = TDTheme.typography.heading4,
            color = TDTheme.colors.white.copy(0.8f),
        )
        Spacer(Modifier.weight(1f))

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 60.dp, topEnd = 60.dp))
                .background(color = TDTheme.colors.background)
                .padding(start = 32.dp, end = 32.dp, top = 32.dp),
        ) {
            LoginFormPanel(uiState = uiState, onAction = onAction)
        }
    }
}

@Composable
private fun LoginLandscapeContent(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
) {
    Row(Modifier.fillMaxSize()) {
        LoginBrandingPanel(
            modifier =
            Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
        Column(
            modifier =
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(TDTheme.colors.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 24.dp),
        ) {
            LoginFormPanel(uiState = uiState, onAction = onAction)
        }
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
