package com.todoapp.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

/** Gap between segments, in degrees. Small enough to read as a ring, wide enough to count. */
private const val SEGMENT_GAP_DEGREES = 3f
private const val FULL_CIRCLE_DEGREES = 360f
private const val START_ANGLE_TOP = -90f

/**
 * A segmented progress ring sized to frame [content] — used on the profile avatar to surface the
 * same health-points streak the Activity screen shows as hearts.
 *
 * Deliberately **segmented rather than a continuous arc**: a smooth sweep is right for the rounded
 * kits but wrong for a pixel kit, whereas discrete segments read correctly in both. The only thing
 * that varies per kit is the cap ([StrokeCap.Round] vs [StrokeCap.Butt]) and the stroke weight,
 * which follows `TDTheme.style.borderWidth`.
 *
 * [filled] segments use `heartFull`, the rest `heartEmpty` — the same tokens as [TDHealthBar], so
 * the two surfaces cannot drift apart.
 */
@Composable
fun TDHealthRing(
    filled: Int,
    total: Int,
    size: Dp,
    modifier: Modifier = Modifier,
    thickness: Dp = 4.dp,
    content: @Composable () -> Unit,
) {
    val segments = total.coerceAtLeast(1)
    val on = filled.coerceIn(0, segments)
    val fullColor = TDTheme.colors.heartFull
    val emptyColor = TDTheme.colors.heartEmpty
    // A stepped kit has no round caps anywhere else; matching that here keeps the ring in-language.
    val cap = if (TDTheme.motion.stepped) StrokeCap.Butt else StrokeCap.Round
    val strokeDp = thickness + TDTheme.style.borderWidth

    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                val stroke = strokeDp.toPx()
                val inset = stroke / 2f
                val arc = FULL_CIRCLE_DEGREES / segments
                val sweep = arc - SEGMENT_GAP_DEGREES
                repeat(segments) { i ->
                    drawArc(
                        color = if (i < on) fullColor else emptyColor,
                        startAngle = START_ANGLE_TOP + i * arc + SEGMENT_GAP_DEGREES / 2f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(this.size.width - stroke, this.size.height - stroke),
                        style = Stroke(width = stroke, cap = cap),
                    )
                }
            }
            // Inset the content by the ring so the avatar never sits under a segment.
            .padding(strokeDp + thickness),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@TDPreview
@Composable
private fun TdHealthRingStatesPreview() {
    TDTheme {
        Row(
            modifier = Modifier
                .background(TDTheme.colors.background)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(20, 13, 4, 0).forEach { hearts ->
                TDHealthRing(filled = hearts, total = 20, size = 96.dp) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(TDTheme.shapes.circle)
                            .background(TDTheme.colors.lightPending),
                        contentAlignment = Alignment.Center,
                    ) {
                        TDText(
                            text = "BB",
                            style = TDTheme.typography.heading4,
                            color = TDTheme.colors.pendingGray,
                        )
                    }
                }
            }
        }
    }
}
