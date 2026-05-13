package com.todoapp.mobile.ui.pomodoro

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.theme.ToDoAppTheme

@Composable
fun PomodoroTimerRing(
    progress: Float,
    progressColor: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 280.dp,
    strokeWidth: Dp = 16.dp,
) {
    val sweepAngle = 360f * progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(inset, inset)

            // Background track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )

            // Progress arc — drains from full to empty as time elapses
            if (sweepAngle > 0f) {
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
            }
        }
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun PomodoroTimerRingPreview() {
    ToDoAppTheme {
        val palette = PomodoroModeTheme.resolve(ModeColorKey.Focus, isSystemInDarkTheme())
        Box(modifier = Modifier.padding(16.dp)) {
            PomodoroTimerRing(
                progress = 0.75f,
                progressColor = palette.content,
                trackColor = palette.track,
            )
        }
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun PomodoroTimerRingEmptyPreview() {
    ToDoAppTheme {
        val palette = PomodoroModeTheme.resolve(ModeColorKey.Focus, isSystemInDarkTheme())
        Box(modifier = Modifier.padding(16.dp)) {
            PomodoroTimerRing(
                progress = 0f,
                progressColor = palette.content,
                trackColor = palette.track,
            )
        }
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun PomodoroTimerRingFullPreview() {
    ToDoAppTheme {
        val palette = PomodoroModeTheme.resolve(ModeColorKey.Focus, isSystemInDarkTheme())
        Box(modifier = Modifier.padding(16.dp)) {
            PomodoroTimerRing(
                progress = 1f,
                progressColor = palette.content,
                trackColor = palette.track,
            )
        }
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun PomodoroTimerRingShortBreakPreview() {
    ToDoAppTheme {
        val palette = PomodoroModeTheme.resolve(ModeColorKey.ShortBreak, isSystemInDarkTheme())
        Box(modifier = Modifier.padding(16.dp)) {
            PomodoroTimerRing(
                progress = 0.4f,
                progressColor = palette.content,
                trackColor = palette.track,
            )
        }
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun PomodoroTimerRingOverTimePreview() {
    ToDoAppTheme {
        val palette = PomodoroModeTheme.resolve(ModeColorKey.OverTime, isSystemInDarkTheme())
        Box(modifier = Modifier.padding(16.dp)) {
            PomodoroTimerRing(
                progress = 0.5f,
                progressColor = palette.content,
                trackColor = palette.track,
            )
        }
    }
}
