package com.todoapp.mobile.ui.pomodoro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.todoapp.mobile.common.RingtoneHolder
import com.todoapp.mobile.ui.pomodoro.PomodoroContract.UiAction
import com.todoapp.mobile.ui.pomodoro.PomodoroContract.UiEffect
import com.todoapp.mobile.ui.pomodoro.PomodoroContract.UiState
import com.todoapp.uikit.components.TDScreenWithSheet
import kotlinx.coroutines.flow.Flow

/**
 * Entry point for the Pomodoro timer: one-time effects and the ambience sheet host. The layout
 * itself lives in [PomodoroContent].
 */
@Composable
fun PomodoroScreen(
    uiState: UiState,
    uiEffect: Flow<UiEffect>,
    onAction: (UiAction) -> Unit,
) {
    val ringtoneHolder = remember { RingtoneHolder() }
    val context = LocalContext.current

    DisposableEffect(Unit) {
        // The banner is the timer's presence on every *other* screen; here it would just repeat
        // what fills the display, so it steps aside for as long as this screen is mounted.
        onAction(UiAction.ToggleBannerVisibility(false))
        onDispose {
            onAction(UiAction.ToggleBannerVisibility(true))
            // Leaving mid-chime otherwise leaves the ringtone playing with no owner to stop it.
            ringtoneHolder.stop()
        }
    }

    LaunchedEffect(Unit) {
        uiEffect.collect { effect ->
            when (effect) {
                is UiEffect.SessionFinished -> ringtoneHolder.play(context)
            }
        }
    }

    TDScreenWithSheet(
        isSheetOpen = uiState.showAmbienceSheet,
        onDismissSheet = { onAction(UiAction.DismissAmbienceSheet) },
        sheetContent = {
            PomodoroAmbienceSheet(
                selected = uiState.ambience,
                volume = uiState.ambienceVolume,
                backgroundEnabled = uiState.ambienceBackgroundEnabled,
                onAction = onAction,
            )
        },
    ) {
        PomodoroContent(uiState = uiState, onAction = onAction)
    }
}
