package com.todoapp.mobile.ui.forgotpassword

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.forgotpassword.ForgotPasswordContract.UiAction
import com.todoapp.mobile.ui.forgotpassword.ForgotPasswordContract.UiEffect
import com.todoapp.mobile.ui.forgotpassword.ForgotPasswordContract.UiState
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDCompactOutlinedTextField
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.extensions.collectWithLifecycle
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme
import kotlinx.coroutines.flow.Flow

@Composable
fun ForgotPasswordScreen(
    uiState: UiState,
    uiEffect: Flow<UiEffect>,
    onAction: (UiAction) -> Unit,
) {
    val context = LocalContext.current
    uiEffect.collectWithLifecycle {
        when (it) {
            is UiEffect.ShowToast ->
                Toast.makeText(context, context.getString(it.messageRes), Toast.LENGTH_SHORT).show()
        }
    }
    ForgotPasswordContent(uiState, onAction)
}

@Composable
private fun ForgotPasswordContent(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
) {
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    if (isPortrait) {
        ForgotPasswordPortraitContent(uiState = uiState, onAction = onAction)
    } else {
        ForgotPasswordLandscapeContent(uiState = uiState, onAction = onAction)
    }
}

@Composable
private fun ForgotPasswordBrandingPanel(modifier: Modifier = Modifier) {
    Column(
        modifier =
        modifier
            .background(TDTheme.colors.pendingGray)
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
            Modifier
                .size(70.dp)
                .background(color = TDTheme.colors.white.copy(alpha = 0.25f), shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_lock_reset),
                contentDescription = stringResource(R.string.logo),
                modifier = Modifier.size(40.dp),
                tint = TDTheme.colors.white,
            )
        }
        Spacer(Modifier.height(8.dp))
        TDText(
            text = stringResource(R.string.forgot_password),
            style = TDTheme.typography.heading1,
            color = TDTheme.colors.white,
        )
    }
}

@Composable
private fun ForgotPasswordFormPanel(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
) {
    Spacer(Modifier.height(32.dp))
    TDText(text = stringResource(R.string.recover_access), style = TDTheme.typography.heading1)
    Spacer(Modifier.height(16.dp))
    TDText(
        text = stringResource(R.string.enter_your_email_address_and_we_will_send_you_a_link_to_reset_your_password),
        color = TDTheme.colors.gray.copy(0.7f),
    )
    Spacer(Modifier.height(16.dp))
    TDCompactOutlinedTextField(
        value = uiState.email,
        label = stringResource(R.string.email),
        onValueChange = { onAction(UiAction.OnEmailChange(it)) },
        placeholder = stringResource(R.string.email),
        isError = uiState.error != null,
        leadingIcon = {
            Icon(
                painterResource(R.drawable.ic_mail_white),
                contentDescription = stringResource(R.string.email),
                tint = TDTheme.colors.gray.copy(0.5f),
            )
        },
        roundedCornerShape = RoundedCornerShape(12.dp),
        height = 50.dp,
    )
    uiState.error?.let {
        TDText(text = it, color = TDTheme.colors.red)
    }
    if (uiState.isSent) {
        Spacer(Modifier.height(12.dp))
        TDText(
            text = stringResource(R.string.reset_link_sent),
            color = TDTheme.colors.darkGreen,
        )
    }
    Spacer(Modifier.height(24.dp))
    TDButton(
        text = stringResource(R.string.send_reset_link),
        fullWidth = true,
        isEnable = !uiState.isSubmitting,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)),
    ) { onAction(UiAction.OnForgotPasswordTap) }
    Spacer(Modifier.height(24.dp))
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .imePadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        TDText(text = stringResource(R.string.remember_your_password), color = TDTheme.colors.gray.copy(0.6f))
        TDText(
            text = stringResource(R.string.back_to_login),
            color = TDTheme.colors.pendingGray,
            style = TDTheme.typography.heading6,
            modifier = Modifier.clickable { onAction(UiAction.OnBackToLoginTap) },
        )
    }
    Spacer(Modifier.height(32.dp))
}

@Composable
private fun ForgotPasswordPortraitContent(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .background(color = TDTheme.colors.pendingGray)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier =
                Modifier
                    .size(70.dp)
                    .background(color = TDTheme.colors.white.copy(alpha = 0.25f), shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_lock_reset),
                    contentDescription = stringResource(R.string.logo),
                    modifier = Modifier.size(40.dp),
                    tint = TDTheme.colors.white,
                )
            }
            Spacer(Modifier.height(8.dp))
            TDText(
                text = stringResource(R.string.forgot_password),
                style = TDTheme.typography.heading1,
                color = TDTheme.colors.white,
            )
        }

        Column(
            Modifier
                .weight(2f)
                .clip(RoundedCornerShape(topStart = 60.dp, topEnd = 60.dp))
                .background(color = TDTheme.colors.background)
                .padding(start = 32.dp, end = 32.dp, top = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            ForgotPasswordFormPanel(uiState = uiState, onAction = onAction)
        }
    }
}

@Composable
private fun ForgotPasswordLandscapeContent(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
) {
    Row(Modifier.fillMaxSize()) {
        ForgotPasswordBrandingPanel(
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
                .padding(horizontal = 32.dp),
        ) {
            ForgotPasswordFormPanel(uiState = uiState, onAction = onAction)
        }
    }
}

@TDPreview
@Composable
private fun ForgotPasswordEmptyPreview() {
    TDTheme {
        ForgotPasswordContent(
            uiState = UiState(email = ""),
            onAction = {},
        )
    }
}

@TDPreview
@Composable
private fun ForgotPasswordFilledPreview() {
    TDTheme {
        ForgotPasswordContent(
            uiState = UiState(email = "berat@example.com"),
            onAction = {},
        )
    }
}

@TDPreview
@Composable
private fun ForgotPasswordSubmittingPreview() {
    TDTheme {
        ForgotPasswordContent(
            uiState =
            UiState(
                email = "berat@example.com",
                isSubmitting = true,
            ),
            onAction = {},
        )
    }
}

@TDPreview
@Composable
private fun ForgotPasswordSentPreview() {
    TDTheme {
        ForgotPasswordContent(
            uiState =
            UiState(
                email = "berat@example.com",
                isSent = true,
            ),
            onAction = {},
        )
    }
}

@TDPreview
@Composable
private fun ForgotPasswordErrorPreview() {
    TDTheme {
        ForgotPasswordContent(
            uiState =
            UiState(
                email = "not-an-email",
                error = "Please enter a valid email",
            ),
            onAction = {},
        )
    }
}
