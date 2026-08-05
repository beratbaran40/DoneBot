package com.todoapp.mobile.ui.pomodoro

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.ui.pomodoro.PomodoroContract.UiAction
import com.todoapp.mobile.ui.pomodoro.PomodoroContract.UiState
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.theme.TDTheme

/**
 * Phone- and tablet-portrait layout: session dots up top, the ring centred, controls at the bottom.
 * The width cap keeps the panel readable on a tablet while the ambience behind it stays full-bleed.
 */
@Composable
fun PomodoroPortraitContent(
    uiState: UiState,
    visuals: PomodoroVisuals,
    onAction: (UiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
        modifier
            .fillMaxHeight()
            .widthIn(max = PortraitMaxWidth)
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(TopInset))

        PomodoroSessionDots(
            totalSessions = uiState.totalSessions,
            currentIndex = uiState.currentSessionIndex,
            contentColor = visuals.content,
            dimColor = visuals.surface,
        )

        Spacer(Modifier.weight(1f))

        PomodoroProgressRing(
            min = uiState.min,
            second = uiState.second,
            progress = visuals.progress,
            progressColor = visuals.content,
            trackColor = visuals.track,
            textColor = visuals.content,
        )

        Spacer(Modifier.height(28.dp))

        PomodoroModeCard(
            mode = uiState.mode,
            surfaceColor = visuals.surface,
            contentColor = visuals.content,
            lightShadow = visuals.lightShadow,
            darkShadow = visuals.darkShadow,
        )

        if (!uiState.infoMessage.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            TDText(
                text = uiState.infoMessage,
                style = TDTheme.typography.subheading1,
                color = visuals.content.copy(alpha = INFO_ALPHA),
            )
        }

        Spacer(Modifier.height(12.dp))

        PomodoroEndSessionChip(contentColor = visuals.content, onAction = onAction)

        Spacer(Modifier.weight(1f))

        PomodoroControls(
            isRunning = uiState.isRunning,
            isOvertime = uiState.isOvertime,
            surfaceColor = visuals.surface,
            contentColor = visuals.content,
            lightShadow = visuals.lightShadow,
            darkShadow = visuals.darkShadow,
            onAction = onAction,
        )

        Spacer(Modifier.height(BottomInset))
    }
}

// Large-screen cap: keep the immersive full-bleed background but centre the timer panel on tablets.
private val PortraitMaxWidth = 480.dp

// Clears the ambience button, which floats at the top-right of the same band. A long session wraps
// its dots across the full width, and at the old 20dp they ran underneath it.
private val TopInset = 64.dp
private val BottomInset = 40.dp
private const val INFO_ALPHA = 0.6f
