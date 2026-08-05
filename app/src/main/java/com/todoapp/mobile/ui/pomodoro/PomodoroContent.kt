package com.todoapp.mobile.ui.pomodoro

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.domain.ambience.PomodoroAmbience
import com.todoapp.mobile.ui.common.rememberAnimationsEnabled
import com.todoapp.mobile.ui.pomodoro.PomodoroContract.UiAction
import com.todoapp.mobile.ui.pomodoro.PomodoroContract.UiState
import com.todoapp.mobile.ui.pomodoro.ambience.PomodoroAmbienceScene
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

/**
 * Composes the timer over its ambience.
 *
 * The scene is the screen's only background — it paints [PomodoroVisuals.background] itself, so
 * with [PomodoroAmbience.None] this is exactly the flat mode-coloured surface the screen has
 * always had. The route carries no top bar, bottom bar or navigation rail (Pomodoro has no
 * `AppDestination` entry at all) and the root Scaffold contributes zero insets, so this really
 * does get the whole window to draw into.
 */
@Composable
fun PomodoroContent(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    val visuals = rememberPomodoroVisuals(uiState)

    Box(modifier.fillMaxSize()) {
        PomodoroAmbienceScene(
            ambience = uiState.ambience,
            tint = visuals.background,
            isDark = TDTheme.isDark,
            animate = rememberAnimationsEnabled(),
            modifier = Modifier.matchParentSize(),
        )

        if (isPortrait) {
            PomodoroPortraitContent(
                uiState = uiState,
                visuals = visuals,
                onAction = onAction,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        } else {
            PomodoroLandscapeContent(
                uiState = uiState,
                visuals = visuals,
                onAction = onAction,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        PomodoroAmbienceButton(
            ambience = uiState.ambience,
            contentColor = visuals.content,
            onClick = { onAction(UiAction.OnAmbienceButtonTap) },
            modifier =
            Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 12.dp, end = 16.dp),
        )

        if (uiState.showFinishEarlyDialog) {
            PomodoroFinishEarlyDialog(onAction = onAction)
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@TDPreview
@Composable
private fun PomodoroFocusPreview() {
    TDTheme {
        PomodoroContent(
            uiState = PomodoroPreviewData.focus,
            onAction = {},
        )
    }
}

@TDPreview
@Composable
private fun PomodoroShortBreakPreview() {
    TDTheme {
        PomodoroContent(uiState = PomodoroPreviewData.shortBreak, onAction = {})
    }
}

@TDPreview
@Composable
private fun PomodoroLongBreakPreview() {
    TDTheme {
        PomodoroContent(uiState = PomodoroPreviewData.longBreak, onAction = {})
    }
}

@TDPreview
@Composable
private fun PomodoroOverTimePreview() {
    TDTheme {
        PomodoroContent(uiState = PomodoroPreviewData.overtime, onAction = {})
    }
}

@TDPreview
@Composable
private fun PomodoroPausedPreview() {
    TDTheme {
        PomodoroContent(uiState = PomodoroPreviewData.paused, onAction = {})
    }
}

@TDPreview
@Composable
private fun PomodoroFinishEarlyPreview() {
    TDTheme {
        PomodoroContent(uiState = PomodoroPreviewData.finishEarlyDialog, onAction = {})
    }
}

@TDPreview
@Composable
private fun PomodoroManySessionsPreview() {
    TDTheme {
        PomodoroContent(uiState = PomodoroPreviewData.manySessions, onAction = {})
    }
}

@TDPreview
@Composable
private fun PomodoroWithFireplacePreview() {
    TDTheme {
        PomodoroContent(uiState = PomodoroPreviewData.withFireplace, onAction = {})
    }
}

@TDPreview
@Composable
private fun PomodoroWithRainPreview() {
    TDTheme {
        PomodoroContent(uiState = PomodoroPreviewData.withRain, onAction = {})
    }
}

@TDPreview
@Composable
private fun PomodoroWithHandpanPreview() {
    TDTheme {
        PomodoroContent(uiState = PomodoroPreviewData.withHandpan, onAction = {})
    }
}
