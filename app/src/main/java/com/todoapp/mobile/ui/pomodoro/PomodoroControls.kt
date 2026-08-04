package com.todoapp.mobile.ui.pomodoro

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.pomodoro.PomodoroContract.UiAction
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.modifier.tdShadow
import com.todoapp.uikit.theme.TDTheme
import com.example.uikit.R as UiKitR

@Composable
fun PomodoroControls(
    isRunning: Boolean,
    isOvertime: Boolean,
    surfaceColor: Color,
    contentColor: Color,
    lightShadow: Color,
    darkShadow: Color,
    onAction: (UiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Center play/pause — large neumorphic circle
        val playIconRes = if (isRunning && !isOvertime) UiKitR.drawable.ic_pause else UiKitR.drawable.ic_resume
        val playDescRes =
            when {
                isOvertime -> R.string.next
                isRunning -> R.string.pause
                else -> R.string.start
            }
        Box(
            modifier =
            Modifier
                .padding(horizontal = 32.dp)
                .size(88.dp)
                .tdShadow(
                    lightShadow = lightShadow,
                    darkShadow = darkShadow,
                    cornerRadius = 44.dp,
                    elevation = 10.dp,
                ).clip(CircleShape)
                .background(surfaceColor)
                .clickable {
                    if (isRunning && !isOvertime) {
                        onAction(UiAction.StopCountDown)
                    } else {
                        onAction(UiAction.StartCountDown)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = tdPainter(playIconRes),
                contentDescription = stringResource(playDescRes),
                tint = contentColor,
                modifier = Modifier.size(42.dp),
            )
        }

        // → Skip / SkipNext
        Box(
            modifier =
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable { onAction(UiAction.SkipSession) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = tdPainter(UiKitR.drawable.ic_skip_next),
                contentDescription = stringResource(R.string.skip),
                tint = contentColor.copy(alpha = 0.65f),
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun PomodoroControlsPreview() {
    val palette = PomodoroModeTheme.resolve(ModeColorKey.Focus, isDark = false)
    TDTheme {
        PomodoroControls(
            isRunning = false,
            isOvertime = false,
            surfaceColor = palette.surface,
            contentColor = palette.content,
            lightShadow = palette.lightShadow,
            darkShadow = palette.darkShadow,
            onAction = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun PomodoroControlsRunningPreview() {
    val palette = PomodoroModeTheme.resolve(ModeColorKey.Focus, isDark = false)
    TDTheme {
        PomodoroControls(
            isRunning = true,
            isOvertime = false,
            surfaceColor = palette.surface,
            contentColor = palette.content,
            lightShadow = palette.lightShadow,
            darkShadow = palette.darkShadow,
            onAction = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun PomodoroControlsOvertimePreview() {
    val palette = PomodoroModeTheme.resolve(ModeColorKey.OverTime, isDark = false)
    TDTheme {
        PomodoroControls(
            isRunning = true,
            isOvertime = true,
            surfaceColor = palette.surface,
            contentColor = palette.content,
            lightShadow = palette.lightShadow,
            darkShadow = palette.darkShadow,
            onAction = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun PomodoroControlsShortBreakPreview() {
    val palette = PomodoroModeTheme.resolve(ModeColorKey.ShortBreak, isDark = false)
    TDTheme {
        PomodoroControls(
            isRunning = true,
            isOvertime = false,
            surfaceColor = palette.surface,
            contentColor = palette.content,
            lightShadow = palette.lightShadow,
            darkShadow = palette.darkShadow,
            onAction = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
