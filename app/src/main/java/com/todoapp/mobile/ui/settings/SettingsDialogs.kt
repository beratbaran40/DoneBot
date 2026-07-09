package com.todoapp.mobile.ui.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.common.LocalReduceMotion
import com.todoapp.uikit.components.TDGoodbyeDialog
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.extensions.ObscuredTouchGuard
import com.todoapp.uikit.previews.TDPreviewDialog
import com.todoapp.uikit.theme.TDTheme

@Composable
internal fun SettingsLogoutDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            TDText(
                text = stringResource(R.string.logout_dialog_title),
                style = TDTheme.typography.heading5,
                color = TDTheme.colors.onBackground,
            )
        },
        text = {
            ObscuredTouchGuard()
            TDText(
                text = stringResource(R.string.logout_dialog_message),
                style = TDTheme.typography.subheading1,
                color = TDTheme.colors.onBackground,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                TDText(
                    text = stringResource(R.string.logout),
                    style = TDTheme.typography.subheading1,
                    color = TDTheme.colors.crossRed,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                TDText(
                    text = stringResource(R.string.cancel),
                    style = TDTheme.typography.subheading1,
                    color = TDTheme.colors.onBackground,
                )
            }
        },
        containerColor = TDTheme.colors.surface,
    )
}

@Composable
internal fun SettingsDeleteAccountDialog(
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val reduceMotion = LocalReduceMotion.current
    TDGoodbyeDialog(
        speechBubbleText = stringResource(R.string.delete_account_speech_bubble),
        legalDetailText = stringResource(R.string.delete_account_dialog_message),
        typedConfirmLabel = stringResource(R.string.delete_account_typed_confirm_label),
        typedConfirmWord = stringResource(R.string.delete_account_typed_confirm_word),
        confirmButtonText = stringResource(R.string.delete_account_button),
        dismissButtonText = stringResource(R.string.cancel),
        inProgressText = stringResource(R.string.delete_account_in_progress),
        isProcessing = isDeleting,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        reduceMotion = reduceMotion,
    )
}

@TDPreviewDialog
@Composable
private fun SettingsLogoutDialogPreview() {
    TDTheme {
        SettingsLogoutDialog(
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@TDPreviewDialog
@Composable
private fun SettingsDeleteAccountDialogPreview() {
    TDTheme {
        SettingsDeleteAccountDialog(
            isDeleting = false,
            onDismiss = {},
            onConfirm = {},
        )
    }
}

@TDPreviewDialog
@Composable
private fun SettingsDeleteAccountDialogDeletingPreview() {
    TDTheme {
        SettingsDeleteAccountDialog(
            isDeleting = true,
            onDismiss = {},
            onConfirm = {},
        )
    }
}
