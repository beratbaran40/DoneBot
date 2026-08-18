package com.todoapp.mobile.ui.pomodoro

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.todoapp.uikit.theme.TDElevationStyle
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
    // A hard-elevation kit draws the play button as a solid key with a shadow block behind it. Next
    // to that, a bare glyph reads as a rendering fault rather than as a quieter action, so skip gets
    // a surface of its own there — the same reasoning that put the outline on the calendar tab.
    val skipOnSurface = TDTheme.style.elevationStyle == TDElevationStyle.HARD
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ControlGap, Alignment.CenterHorizontally),
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
                .size(PlayDiameter)
                .tdShadow(
                    lightShadow = lightShadow,
                    darkShadow = darkShadow,
                    cornerRadius = 44.dp,
                    elevation = 10.dp,
                ).clip(TDTheme.shapes.circle)
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
                .size(if (skipOnSurface) SkipDiameterOnSurface else SkipDiameter)
                .then(
                    if (skipOnSurface) {
                        Modifier.tdShadow(
                            lightShadow = lightShadow,
                            darkShadow = darkShadow,
                            cornerRadius = SkipDiameterOnSurface / 2,
                            elevation = SkipElevation,
                        )
                    } else {
                        Modifier
                    },
                ).clip(TDTheme.shapes.circle)
                .then(if (skipOnSurface) Modifier.background(surfaceColor) else Modifier)
                .clickable { onAction(UiAction.SkipSession) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = tdPainter(UiKitR.drawable.ic_skip_next),
                contentDescription = stringResource(R.string.skip),
                tint = contentColor.copy(alpha = SKIP_INK_ALPHA),
                modifier = Modifier.size(SkipIcon),
            )
        }
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun PomodoroControlsPreview() {
    val palette = PomodoroModeTheme.resolve(
        colorKey = ModeColorKey.Focus,
        isDark = false,
        palette = TDTheme.palette,
        colors = TDTheme.colors,
    )
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
    val palette = PomodoroModeTheme.resolve(
        colorKey = ModeColorKey.Focus,
        isDark = false,
        palette = TDTheme.palette,
        colors = TDTheme.colors,
    )
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
    val palette = PomodoroModeTheme.resolve(
        colorKey = ModeColorKey.OverTime,
        isDark = false,
        palette = TDTheme.palette,
        colors = TDTheme.colors,
    )
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
    val palette = PomodoroModeTheme.resolve(
        colorKey = ModeColorKey.ShortBreak,
        isDark = false,
        palette = TDTheme.palette,
        colors = TDTheme.colors,
    )
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

/** The play key, and the gap that keeps skip clear of its shadow block in a hard-elevation kit. */
private val PlayDiameter = 88.dp
private val ControlGap = 24.dp
private val SkipDiameter = 48.dp
private val SkipDiameterOnSurface = 56.dp
private val SkipElevation = 6.dp
private val SkipIcon = 28.dp
private const val SKIP_INK_ALPHA = 0.65f
