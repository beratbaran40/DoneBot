package com.todoapp.mobile.ui.pomodoro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.todoapp.uikit.theme.TDTheme

/**
 * Session progress dots — one dot per phase (each focus period and each short/long break).
 *
 * The current dot is slightly larger. Past and current dots use [contentColor]; future dots
 * use [dimColor]. When the phases don't fit on a single line they wrap onto the next line, so
 * the dot count always matches the real number of phases in the session — nothing is hidden.
 */
@Composable
fun PomodoroSessionDots(
    totalSessions: Int,
    currentIndex: Int,
    contentColor: Color,
    dimColor: Color,
    modifier: Modifier = Modifier,
) {
    if (totalSessions <= 0) return

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DOT_GAP, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(DOT_GAP),
    ) {
        for (i in 0 until totalSessions) {
            val isCurrent = i == currentIndex
            val isPast = i < currentIndex
            val dotSize = if (isCurrent) CURRENT_DOT_SIZE else DOT_SIZE
            val dotColor = if (isPast || isCurrent) contentColor else dimColor

            // Fixed-size cell keeps the 8dp and 12dp dots vertically centered within a line.
            Box(
                modifier = Modifier.size(CURRENT_DOT_SIZE),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                    Modifier
                        .size(dotSize)
                        .clip(TDTheme.shapes.circle)
                        .background(dotColor),
                )
            }
        }
    }
}

private val DOT_SIZE = 8.dp
private val CURRENT_DOT_SIZE = 12.dp
private val DOT_GAP = 6.dp
