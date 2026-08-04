package com.todoapp.mobile.ui.resetpassword

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.common.SecureScreenEffect
import com.todoapp.mobile.ui.resetpassword.ResetPasswordContract.UiAction
import com.todoapp.mobile.ui.resetpassword.ResetPasswordContract.UiEffect
import com.todoapp.mobile.ui.resetpassword.ResetPasswordContract.UiState
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDCompactOutlinedTextField
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.extensions.collectWithLifecycle
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Composable
fun ResetPasswordScreen(
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        TDText(
            text = stringResource(R.string.reset_password_hint),
            color = TDTheme.colors.gray,
        )
        Spacer(Modifier.height(16.dp))

        PasswordField(
            value = uiState.newPassword,
            label = stringResource(R.string.new_password),
            visible = uiState.isNewVisible,
            errorRes = uiState.newError,
            enabled = !uiState.isSubmitting && !uiState.isDone,
            onValueChange = { onAction(UiAction.OnNewChange(it)) },
            onToggleVisibility = { onAction(UiAction.OnToggleNewVisibility) },
        )

        PasswordField(
            value = uiState.confirmPassword,
            label = stringResource(R.string.confirm_new_password),
            visible = uiState.isConfirmVisible,
            errorRes = uiState.confirmError,
            enabled = !uiState.isSubmitting && !uiState.isDone,
            onValueChange = { onAction(UiAction.OnConfirmChange(it)) },
            onToggleVisibility = { onAction(UiAction.OnToggleConfirmVisibility) },
        )

        Spacer(Modifier.height(24.dp))

        if (uiState.isDone) {
            TDText(
                text = stringResource(R.string.reset_password_success),
                color = TDTheme.colors.darkGreen,
            )
            Spacer(Modifier.height(16.dp))
            TDButton(
                text = stringResource(R.string.back_to_login),
                fullWidth = true,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onAction(UiAction.OnBackToLogin) },
            )
        } else {
            TDButton(
                text = stringResource(R.string.reset_password),
                fullWidth = true,
                isEnable = uiState.canSubmit,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onAction(UiAction.OnSubmit) },
            )
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    label: String,
    visible: Boolean,
    errorRes: Int?,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
) {
    TDCompactOutlinedTextField(
        value = value,
        label = label,
        onValueChange = onValueChange,
        enabled = enabled,
        isError = errorRes != null,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            Icon(
                painter = tdPainter(
                    if (visible) {
                        com.example.uikit.R.drawable.ic_visibility
                    } else {
                        com.example.uikit.R.drawable.ic_visibility_off
                    },
                ),
                contentDescription = stringResource(R.string.toggle_password_visibility),
                tint = TDTheme.colors.gray,
                modifier = Modifier.clickable(onClick = onToggleVisibility),
            )
        },
        modifier = Modifier.fillMaxWidth(),
    )
    errorRes?.let { res ->
        val text = if (res == R.string.error_password_min_length) {
            stringResource(res, ResetPasswordContract.MIN_PASSWORD_LENGTH)
        } else {
            stringResource(res)
        }
        TDText(text = text, color = TDTheme.colors.red)
    }
}

@TDPreview
@Composable
private fun ResetPasswordScreenPreview(
    @PreviewParameter(ResetPasswordPreviewProvider::class) state: UiState,
) {
    TDTheme {
        ResetPasswordScreen(
            uiState = state,
            uiEffect = emptyFlow(),
            onAction = {},
        )
    }
}
