package com.todoapp.mobile.ui.pomodoro

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.pomodoro.PomodoroContract.UiAction
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.previews.TDPreviewDialog
import com.todoapp.uikit.theme.TDTheme

/**
 * Confirms ending a session before the queue runs out. Uses the app's chrome colours rather than
 * the mode palette — a dialog is a decision surface, not part of the ambience.
 */
@Composable
fun PomodoroFinishEarlyDialog(onAction: (UiAction) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAction(UiAction.DismissEndSessionDialog) },
        title = {
            TDText(
                text = stringResource(R.string.pomodoro_end_session_title),
                style = TDTheme.typography.heading5,
                color = TDTheme.colors.onBackground,
            )
        },
        text = {
            TDText(
                text = stringResource(R.string.pomodoro_end_session_message),
                style = TDTheme.typography.subheading1,
                color = TDTheme.colors.onBackground.copy(alpha = BODY_ALPHA),
            )
        },
        confirmButton = {
            TextButton(onClick = { onAction(UiAction.ConfirmEndSession) }) {
                TDText(
                    text = stringResource(R.string.pomodoro_end_session),
                    style = TDTheme.typography.heading6,
                    color = TDTheme.colors.crossRed,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction(UiAction.DismissEndSessionDialog) }) {
                TDText(
                    text = stringResource(R.string.pomodoro_keep_going),
                    style = TDTheme.typography.heading6,
                    color = TDTheme.colors.pendingGray,
                )
            }
        },
        containerColor = TDTheme.colors.surface,
    )
}

private const val BODY_ALPHA = 0.7f

@TDPreviewDialog
@Composable
private fun PomodoroFinishEarlyDialogPreview() {
    TDTheme {
        PomodoroFinishEarlyDialog(onAction = {})
    }
}
