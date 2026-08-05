package com.todoapp.mobile.ui.pomodoro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.LocalWindowSizeClass
import com.todoapp.mobile.ui.pomodoro.PomodoroContract.UiAction
import com.todoapp.mobile.ui.pomodoro.PomodoroContract.UiState
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.theme.TDTheme

/**
 * Two columns side by side: timer on the left, mode and controls on the right.
 *
 * Landscape rings are small to fit a phone's short height; tablets (Expanded width) have the
 * vertical room for the full-size ring, so the timer text isn't clipped.
 */
@Composable
fun PomodoroLandscapeContent(
    uiState: UiState,
    visuals: PomodoroVisuals,
    onAction: (UiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ringSize =
        if (LocalWindowSizeClass.current.widthSizeClass == WindowWidthSizeClass.Expanded) {
            ProgressRingReferenceSize
        } else {
            CompactLandscapeRingSize
        }

    Row(
        modifier =
        modifier
            .fillMaxHeight()
            .widthIn(max = LandscapeMaxWidth)
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            PomodoroSessionDots(
                totalSessions = uiState.totalSessions,
                currentIndex = uiState.currentSessionIndex,
                contentColor = visuals.content,
                dimColor = visuals.surface,
            )
            Spacer(Modifier.height(12.dp))
            PomodoroProgressRing(
                min = uiState.min,
                second = uiState.second,
                progress = visuals.progress,
                progressColor = visuals.content,
                trackColor = visuals.track,
                textColor = visuals.content,
                size = ringSize,
            )
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            PomodoroModeCard(
                mode = uiState.mode,
                surfaceColor = visuals.surface,
                contentColor = visuals.content,
                lightShadow = visuals.lightShadow,
                darkShadow = visuals.darkShadow,
            )

            if (!uiState.infoMessage.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                TDText(
                    text = uiState.infoMessage,
                    style = TDTheme.typography.subheading1,
                    color = visuals.content.copy(alpha = INFO_ALPHA),
                )
            }

            Spacer(Modifier.height(10.dp))

            PomodoroEndSessionChip(contentColor = visuals.content, onAction = onAction)

            Spacer(Modifier.height(16.dp))

            PomodoroControls(
                isRunning = uiState.isRunning,
                isOvertime = uiState.isOvertime,
                surfaceColor = visuals.surface,
                contentColor = visuals.content,
                lightShadow = visuals.lightShadow,
                darkShadow = visuals.darkShadow,
                onAction = onAction,
            )
        }
    }
}

private val LandscapeMaxWidth = 840.dp
private val CompactLandscapeRingSize = 220.dp
private const val INFO_ALPHA = 0.6f
