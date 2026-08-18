package com.todoapp.mobile.ui.pomodoro.ambience

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Resonance made visible: rings expanding from the centre of the screen on a steady cadence, with
 * slow motes drifting through them.
 *
 * The handpan has no literal scene the way fire and rain do, so this one answers the sound rather
 * than the object — rings emanate from behind the timer, which is exactly where the eye already is.
 */
@Composable
fun HandpanScene(
    clock: State<Float>,
    tint: Color,
    ink: AmbienceInk,
    isDark: Boolean,
    cell: Dp,
    modifier: Modifier = Modifier,
) {
    val motes = remember { motes(seed = 5_501) }
    val rippleInk = ink.ripple

    Spacer(
        modifier.drawBehind {
            val t = clock.value
            drawRect(tint)
            val grid = SceneGrid(if (cell > 0.dp) cell.toPx() else 1f)
            drawRipples(grid, t, rippleInk)
            drawMotes(grid, motes, t, rippleInk)
        },
    )
}

/**
 * [RING_COUNT] rings share one cycle, evenly offset, so a new one always leaves the centre as the
 * outermost fades — a continuous swell rather than a repeating pulse.
 */
private fun DrawScope.drawRipples(
    grid: SceneGrid,
    time: Float,
    ink: Color,
) {
    val center = Offset(size.width * CENTER_X, size.height * CENTER_Y)
    val maxRadius = size.maxDimension * MAX_RADIUS
    repeat(RING_COUNT) { index ->
        val progress = fract(time / RING_PERIOD + index.toFloat() / RING_COUNT)
        // Ease out: rings sprint away from the centre, then linger as they thin out.
        val eased = 1f - (1f - progress) * (1f - progress)
        val radius = maxRadius * eased
        val color = ink.copy(alpha = ink.alpha * (1f - progress) * RING_ALPHA)
        val width = RING_WIDTH_MAX - (RING_WIDTH_MAX - RING_WIDTH_MIN) * progress
        if (grid.quantised) {
            // A square swell rather than a stepped circle: an expanding frame is what this effect
            // looked like on the hardware, and it costs one rect instead of a ring of cells.
            val side = grid.span(radius * 2f)
            drawRect(
                color = color,
                topLeft = Offset(grid.snap(center.x - side / 2f), grid.snap(center.y - side / 2f)),
                size = Size(side, side),
                style = Stroke(width = grid.span(width)),
            )
        } else {
            drawCircle(color = color, radius = radius, center = center, style = Stroke(width = width))
        }
    }
}

private fun DrawScope.drawMotes(
    grid: SceneGrid,
    motes: List<Mote>,
    time: Float,
    ink: Color,
) {
    motes.forEach { mote ->
        val rise = fract(mote.phase + time * mote.speed)
        val sway = kotlin.math.sin(time * mote.swaySpeed + mote.phase * TAU) * size.width * mote.swayReach
        // Fade in and out at the edges of the travel so motes never blink into existence.
        val presence = kotlin.math.sin(rise * kotlin.math.PI.toFloat())
        drawCircle(
            color = ink.copy(alpha = ink.alpha * presence * MOTE_ALPHA),
            radius = size.minDimension * mote.radius,
            center = Offset(size.width * mote.x + sway, size.height * (1f - rise)),
        )
    }
}

private fun motes(seed: Int): List<Mote> {
    val random = sceneRandom(seed)
    return List(MOTE_COUNT) {
        Mote(
            x = random.nextFloat(),
            speed = MOTE_MIN_SPEED + random.nextFloat() * (MOTE_MAX_SPEED - MOTE_MIN_SPEED),
            phase = random.nextFloat(),
            radius = MOTE_MIN_RADIUS + random.nextFloat() * (MOTE_MAX_RADIUS - MOTE_MIN_RADIUS),
            swaySpeed = MOTE_SWAY_MIN + random.nextFloat() * (MOTE_SWAY_MAX - MOTE_SWAY_MIN),
            swayReach = random.nextFloat() * MOTE_SWAY_REACH,
        )
    }
}

private class Mote(
    val x: Float,
    val speed: Float,
    val phase: Float,
    val radius: Float,
    val swaySpeed: Float,
    val swayReach: Float,
)

private const val CENTER_X = 0.5f
private const val CENTER_Y = 0.45f
private const val RING_COUNT = 5
private const val RING_PERIOD = 7.5f
private const val MAX_RADIUS = 0.75f
private const val RING_ALPHA = 0.30f
private const val RING_WIDTH_MIN = 0.8f
private const val RING_WIDTH_MAX = 3.2f

private const val MOTE_COUNT = 14
private const val MOTE_ALPHA = 0.35f
private const val MOTE_MIN_SPEED = 0.018f
private const val MOTE_MAX_SPEED = 0.045f
private const val MOTE_MIN_RADIUS = 0.003f
private const val MOTE_MAX_RADIUS = 0.009f
private const val MOTE_SWAY_MIN = 0.25f
private const val MOTE_SWAY_MAX = 0.65f
private const val MOTE_SWAY_REACH = 0.05f
