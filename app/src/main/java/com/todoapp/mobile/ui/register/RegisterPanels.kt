package com.todoapp.mobile.ui.register

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.register.RegisterContract.UiAction
import com.todoapp.mobile.ui.register.RegisterContract.UiState
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDButtonType
import com.todoapp.uikit.components.TDCompactOutlinedTextField
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.theme.TDTheme

@Composable
internal fun RegisterFormPanel(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
) {
    Spacer(Modifier.height(16.dp))
    TDCompactOutlinedTextField(
        value = uiState.fullName,
        label = stringResource(R.string.full_name),
        onValueChange = { onAction(UiAction.OnFullNameChange(it)) },
        placeholder = stringResource(R.string.full_name),
        isError = false,
        leadingIcon = {
            Icon(
                painterResource(R.drawable.ic_person),
                contentDescription = stringResource(R.string.full_name),
                tint = TDTheme.colors.onBackground.copy(0.5f),
            )
        },
        roundedCornerShape = RoundedCornerShape(12.dp),
        height = 50.dp,
    )
    TDCompactOutlinedTextField(
        value = uiState.email,
        label = stringResource(R.string.email),
        onValueChange = { onAction(UiAction.OnEmailChange(it)) },
        placeholder = stringResource(R.string.email),
        isError = uiState.emailError != null,
        leadingIcon = {
            Icon(
                painterResource(R.drawable.ic_mail_white),
                contentDescription = stringResource(R.string.email),
                tint =
                when {
                    uiState.emailError != null -> TDTheme.colors.crossRed
                    else -> TDTheme.colors.onBackground.copy(alpha = 0.5f)
                },
            )
        },
        roundedCornerShape = RoundedCornerShape(12.dp),
        height = 50.dp,
    )
    uiState.emailError?.let {
        TDText(text = it.message, color = TDTheme.colors.red)
    }
    TDCompactOutlinedTextField(
        value = uiState.password,
        label = stringResource(R.string.password),
        visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        onValueChange = { onAction(UiAction.OnPasswordChange(it)) },
        placeholder = stringResource(R.string.password),
        isError = uiState.passwordError != null,
        leadingIcon = {
            Icon(
                painterResource(R.drawable.ic_lock),
                contentDescription = stringResource(R.string.password),
                tint =
                when {
                    uiState.passwordError != null -> TDTheme.colors.crossRed
                    else -> TDTheme.colors.onBackground.copy(alpha = 0.5f)
                },
            )
        },
        trailingIcon = {
            IconButton(onClick = { onAction(UiAction.OnPasswordVisibilityTap) }) {
                Icon(
                    painter =
                    painterResource(
                        if (uiState.isPasswordVisible) R.drawable.ic_visibility_on else R.drawable.ic_visibility_close,
                    ),
                    contentDescription = stringResource(R.string.toggle_password_visibility),
                    tint = TDTheme.colors.onBackground.copy(alpha = 0.5f),
                )
            }
        },
        roundedCornerShape = RoundedCornerShape(12.dp),
        height = 50.dp,
    )
    uiState.passwordError?.let {
        TDText(text = it.message, color = TDTheme.colors.red)
    }

    Spacer(modifier = Modifier.height(2.dp))

    TDPasswordStrengthIndicator(uiState = uiState)

    TDCompactOutlinedTextField(
        value = uiState.confirmPassword,
        visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        label = stringResource(R.string.confirm_password),
        onValueChange = { onAction(UiAction.OnConfirmPasswordChange(it)) },
        placeholder = stringResource(R.string.confirm_password),
        isError = uiState.confirmPasswordError != null,
        leadingIcon = {
            Icon(
                painterResource(R.drawable.ic_lock),
                contentDescription = stringResource(R.string.confirm_password),
                tint =
                when {
                    uiState.confirmPasswordError != null -> TDTheme.colors.crossRed
                    else -> TDTheme.colors.onBackground.copy(alpha = 0.5f)
                },
            )
        },
        roundedCornerShape = RoundedCornerShape(12.dp),
        height = 50.dp,
    )
    uiState.confirmPasswordError?.let {
        TDText(text = it.message, color = TDTheme.colors.red)
    }
    uiState.generalError?.let {
        TDText(text = it.message, color = TDTheme.colors.red)
    }
    Spacer(Modifier.height(16.dp))
    TDButton(
        text = stringResource(R.string.sign_up),
        fullWidth = true,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)),
    ) { onAction(UiAction.OnSignUpTap) }
    Spacer(Modifier.height(16.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        TDButton(
            text = stringResource(R.string.sign_up_with_google),
            fullWidth = true,
            type = TDButtonType.OUTLINE,
            icon = painterResource(R.drawable.ic_google_logo),
            modifier = Modifier.fillMaxWidth(),
        ) { onAction(UiAction.OnGoogleSignInTap) }
    }

    Spacer(Modifier.height(16.dp))
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        TDText(
            text = stringResource(R.string.already_have_an_account),
            style = TDTheme.typography.heading6,
            color = TDTheme.colors.onBackground.copy(0.7f),
        )
        TDText(
            text = stringResource(R.string.login),
            color = TDTheme.colors.pendingGray,
            style = TDTheme.typography.heading6.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.clickable { onAction(UiAction.OnLoginTap) },
        )
    }
}
