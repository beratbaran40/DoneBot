package com.todoapp.mobile.ui.pomodorolaunch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.pomodorolaunch.PomodoroLaunchContract.UiAction
import com.todoapp.mobile.ui.pomodorolaunch.PomodoroLaunchContract.UiState
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDButtonType
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.theme.TDTheme

@Composable
fun PomodoroLaunchScreen(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
) {
    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        Box(
            modifier =
            Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(TDTheme.colors.pendingGray),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = tdPainter(R.drawable.ic_focus),
                contentDescription = null,
                tint = TDTheme.colors.white,
                modifier = Modifier.size(40.dp),
            )
        }

        Spacer(Modifier.height(16.dp))

        TDText(
            text = stringResource(R.string.pomodoro_launch_title),
            style = TDTheme.typography.heading2,
            color = TDTheme.colors.onBackground,
        )

        Spacer(Modifier.height(8.dp))

        TDText(
            text = stringResource(R.string.pomodoro_launch_subtitle),
            style = TDTheme.typography.subheading1,
            color = TDTheme.colors.onBackground.copy(alpha = 0.55f),
        )

        Spacer(Modifier.height(32.dp))

        if (!uiState.isLoading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LaunchStatCard(
                    label = stringResource(R.string.pomodoro_sessions_label),
                    value = uiState.sessionCount.toString(),
                    modifier = Modifier.weight(1f),
                )
                LaunchStatCard(
                    label = stringResource(R.string.pomodoro_focus_label),
                    value = stringResource(R.string.pomodoro_value_min, uiState.focusTime),
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LaunchStatCard(
                    label = stringResource(R.string.pomodoro_short_break_label),
                    value = stringResource(R.string.pomodoro_value_min, uiState.shortBreak),
                    modifier = Modifier.weight(1f),
                )
                LaunchStatCard(
                    label = stringResource(R.string.pomodoro_long_break_label),
                    value = stringResource(R.string.pomodoro_value_min, uiState.longBreak),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.weight(1f))

        TDButton(
            text = stringResource(R.string.start),
            type = TDButtonType.PRIMARY,
            fullWidth = true,
            onClick = { onAction(UiAction.OnStartTap) },
        )

        Spacer(Modifier.height(12.dp))

        TDButton(
            text = stringResource(R.string.pomodoro_configure_timer),
            type = TDButtonType.OUTLINE,
            fullWidth = true,
            onClick = { onAction(UiAction.OnSettingsTap) },
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun LaunchStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
        modifier
            .clip(TDTheme.shapes.large)
            .background(TDTheme.colors.pendingGray)
            .padding(vertical = 16.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TDText(
            text = value,
            style = TDTheme.typography.heading3,
            color = TDTheme.colors.white,
        )
        Spacer(Modifier.height(4.dp))
        TDText(
            text = label,
            style = TDTheme.typography.subheading1,
            color = TDTheme.colors.onBackground,
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun PomodoroLaunchScreenPreview() {
    TDTheme {
        PomodoroLaunchScreen(
            uiState =
            UiState(
                sessionCount = 8,
                focusTime = 25,
                shortBreak = 5,
                longBreak = 20,
                isLoading = false,
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun PomodoroLaunchScreenLoadingPreview() {
    TDTheme {
        PomodoroLaunchScreen(
            uiState =
            UiState(
                sessionCount = 0,
                focusTime = 25,
                shortBreak = 5,
                longBreak = 20,
                isLoading = true,
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun PomodoroLaunchScreenLongPreview() {
    TDTheme {
        PomodoroLaunchScreen(
            uiState =
            UiState(
                sessionCount = 24,
                focusTime = 50,
                shortBreak = 10,
                longBreak = 30,
                isLoading = false,
            ),
            onAction = {},
        )
    }
}
