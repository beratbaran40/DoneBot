package com.todoapp.mobile.ui.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.auth.AuthConsentFooter
import com.todoapp.mobile.ui.login.LoginContract.UiAction
import com.todoapp.mobile.ui.login.LoginContract.UiState
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDButtonType
import com.todoapp.uikit.components.TDCompactOutlinedTextField
import com.todoapp.uikit.components.TDInfoCard
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.theme.TDTheme

@Composable
internal fun LoginFormPanel(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
) {
    TDText(
        text = stringResource(R.string.welcome_back),
        style = TDTheme.typography.heading2,
        color = TDTheme.colors.onBackground,
    )
    Spacer(Modifier.height(4.dp))
    TDText(
        text = stringResource(R.string.please_sign_in_to_your_account),
        style = TDTheme.typography.heading5,
        color = TDTheme.colors.gray,
    )
    uiState.socialOnlyProvider?.let { provider ->
        Spacer(Modifier.height(16.dp))
        TDInfoCard(
            text =
            when (provider) {
                "google" -> stringResource(R.string.login_social_only_google)
                else -> stringResource(R.string.login_social_only_generic)
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Spacer(Modifier.height(24.dp))

    TDCompactOutlinedTextField(
        value = uiState.email,
        label = stringResource(R.string.email_address),
        onValueChange = { onAction(UiAction.OnEmailChange(it)) },
        placeholder = stringResource(R.string.email),
        isError = uiState.emailError != null,
        leadingIcon = {
            Icon(
                tdPainter(R.drawable.ic_mail_white),
                contentDescription = stringResource(R.string.email),
                tint = TDTheme.colors.onBackground.copy(0.5f),
            )
        },
        height = 50.dp,
    )
    uiState.emailError?.let {
        TDText(text = it.message, color = TDTheme.colors.red)
    }

    Spacer(Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TDText(
            text = stringResource(R.string.password),
            style = TDTheme.typography.heading6,
            color = TDTheme.colors.onBackground,
        )
        TDText(
            text = stringResource(R.string.forgot_password),
            style = TDTheme.typography.subheading4,
            color = TDTheme.colors.pendingGray,
            modifier = Modifier.clickable { onAction(UiAction.OnForgotPasswordTap) },
        )
    }
    Spacer(Modifier.height(4.dp))

    TDCompactOutlinedTextField(
        value = uiState.password,
        label = null,
        visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        onValueChange = { onAction(UiAction.OnPasswordChange(it)) },
        placeholder = stringResource(R.string.password),
        isError = uiState.passwordError != null,
        leadingIcon = {
            Icon(
                tdPainter(R.drawable.ic_lock),
                contentDescription = stringResource(R.string.password),
                tint = TDTheme.colors.onBackground.copy(0.5f),
            )
        },
        trailingIcon = {
            IconButton(onClick = { onAction(UiAction.OnPasswordVisibilityTap) }) {
                Icon(
                    painter =
                    tdPainter(
                        if (uiState.isPasswordVisible) {
                            R.drawable.ic_visibility_on
                        } else {
                            R.drawable.ic_visibility_close
                        },
                    ),
                    contentDescription = stringResource(R.string.toggle_password_visibility),
                    tint = TDTheme.colors.onBackground.copy(0.5f),
                )
            }
        },
        height = 50.dp,
    )
    uiState.passwordError?.let {
        TDText(text = it.message, color = TDTheme.colors.red)
    }

    Spacer(Modifier.height(24.dp))

    TDButton(
        text = stringResource(R.string.login),
        fullWidth = true,
        modifier = Modifier.clip(TDTheme.shapes.medium),
    ) { onAction(UiAction.OnLoginTap) }

    Spacer(Modifier.height(16.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        TDButton(
            text = stringResource(R.string.google),
            fullWidth = true,
            type = if (uiState.socialOnlyProvider == "google") TDButtonType.PRIMARY else TDButtonType.OUTLINE,
            icon = tdPainter(R.drawable.ic_google_logo),
            modifier = Modifier.fillMaxWidth(),
        ) { onAction(UiAction.OnGoogleSignInTap) }
    }

    Spacer(Modifier.height(16.dp))
    AuthConsentFooter()

    Spacer(Modifier.height(24.dp))

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        TDText(
            text = stringResource(R.string.dont_have_an_account),
            style = TDTheme.typography.heading6,
            color = TDTheme.colors.onBackground.copy(0.7f),
        )
        TDText(
            text = stringResource(R.string.register),
            color = TDTheme.colors.pendingGray,
            style = TDTheme.typography.heading6.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.clickable { onAction(UiAction.OnRegisterTap) },
        )
    }
}
