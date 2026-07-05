package com.todoapp.mobile.ui.forgotpassword

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.auth.AuthScaffold
import com.todoapp.mobile.ui.common.SecureScreenEffect
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
    SecureScreenEffect()
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
    AuthScaffold(brandTitle = stringResource(R.string.forgot_password)) {
        ForgotPasswordFormPanel(uiState = uiState, onAction = onAction)
    }
}

@Composable
private fun ForgotPasswordFormPanel(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
) {
    Spacer(Modifier.height(8.dp))
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
        modifier = Modifier.fillMaxWidth(),
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
