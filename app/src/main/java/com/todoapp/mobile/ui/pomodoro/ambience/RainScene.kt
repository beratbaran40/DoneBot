package com.todoapp.mobile.ui.pomodoro.ambience

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Rain seen through a window: two parallax sheets of falling streaks, plus a handful of droplets
 * crawling down the glass in front of them.
 *
 * Every streak's position is `fract(phase + t * speed)` — a pure function of the clock, so there
 * is nothing to step and nothing to allocate once the layers are built.
 */
@Composable
fun RainScene(
    clock: State<Float>,
    tint: Color,
    ink: AmbienceInk,
    isDark: Boolean,
    cell: Dp,
    modifier: Modifier = Modifier,
) {
    // Fractional coordinates, so the layers survive rotation and never depend on pixel size.
    val far = remember { rainLayer(seed = 1_101, count = FAR_COUNT, minSpeed = 0.22f, maxSpeed = 0.38f) }
    val near = remember { rainLayer(seed = 2_203, count = NEAR_COUNT, minSpeed = 0.55f, maxSpeed = 0.85f) }
    val glass = remember { glassDroplets(seed = 3_307) }

    val streak = ink.streak

    Spacer(
        modifier.drawBehind {
            val t = clock.value
            val grid = SceneGrid(if (cell > 0.dp) cell.toPx() else 1f)
            drawRect(tint)
            drawStreaks(grid, far, t, streak.copy(alpha = FAR_ALPHA), FAR_WIDTH, FAR_LENGTH)
            drawStreaks(grid, near, t, streak.copy(alpha = NEAR_ALPHA), NEAR_WIDTH, NEAR_LENGTH)
            drawGlass(grid, glass, t, streak.copy(alpha = GLASS_ALPHA))
        },
    )
}

private fun DrawScope.drawStreaks(
    grid: SceneGrid,
    layer: List<Streak>,
    time: Float,
    color: Color,
    strokeWidth: Float,
    lengthFraction: Float,
) {
    val length = size.height * lengthFraction
    val travel = size.height + length
    layer.forEach { streak ->
        val y = fract(streak.phase + time * streak.speed) * travel - length
        val x = streak.x * size.width + streak.drift * size.width
        drawLine(
            color = color,
            start = Offset(x, y),
            end = Offset(x + streak.slant * size.width, y + length),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

/** Droplets that cling, swell and slide — much slower than the rain behind them. */
private fun DrawScope.drawGlass(
    grid: SceneGrid,
    droplets: List<Streak>,
    time: Float,
    color: Color,
) {
    droplets.forEach { droplet ->
        val progress = fract(droplet.phase + time * droplet.speed)
        val y = progress * size.height
        val x = droplet.x * size.width
        // Fade in at the top and out at the bottom so nothing pops into or out of existence.
        val presence = kotlin.math.sin(progress * kotlin.math.PI.toFloat())
        val radius = size.minDimension * droplet.slant
        drawCircle(color.copy(alpha = color.alpha * presence), radius, Offset(x, y))
        drawLine(
            color = color.copy(alpha = color.alpha * presence * TRAIL_FADE),
            start = Offset(x, y - radius * TRAIL_LENGTH),
            end = Offset(x, y),
            strokeWidth = radius,
            cap = StrokeCap.Round,
        )
    }
}

private fun rainLayer(
    seed: Int,
    count: Int,
    minSpeed: Float,
    maxSpeed: Float,
): List<Streak> {
    val random = sceneRandom(seed)
    return List(count) {
        Streak(
            x = random.nextFloat(),
            speed = minSpeed + random.nextFloat() * (maxSpeed - minSpeed),
            phase = random.nextFloat(),
            drift = 0f,
            slant = SLANT_MIN + random.nextFloat() * (SLANT_MAX - SLANT_MIN),
        )
    }
}

private fun glassDroplets(seed: Int): List<Streak> {
    val random = sceneRandom(seed)
    return List(GLASS_COUNT) {
        Streak(
            x = random.nextFloat(),
            speed = GLASS_MIN_SPEED + random.nextFloat() * (GLASS_MAX_SPEED - GLASS_MIN_SPEED),
            phase = random.nextFloat(),
            drift = 0f,
            // Doubles as the droplet radius, as a fraction of the smaller screen dimension.
            slant = GLASS_MIN_RADIUS + random.nextFloat() * (GLASS_MAX_RADIUS - GLASS_MIN_RADIUS),
        )
    }
}

/** One falling element. All values are fractions of the drawing surface, never pixels. */
private class Streak(
    val x: Float,
    val speed: Float,
    val phase: Float,
    val drift: Float,
    val slant: Float,
)

private const val FAR_COUNT = 60
private const val NEAR_COUNT = 30
private const val GLASS_COUNT = 10

private const val FAR_ALPHA = 0.16f
private const val NEAR_ALPHA = 0.32f
private const val GLASS_ALPHA = 0.22f

private const val FAR_WIDTH = 1.4f
private const val NEAR_WIDTH = 2.6f
private const val FAR_LENGTH = 0.05f
private const val NEAR_LENGTH = 0.09f

private const val SLANT_MIN = 0.008f
private const val SLANT_MAX = 0.028f

private const val GLASS_MIN_SPEED = 0.012f
private const val GLASS_MAX_SPEED = 0.035f
private const val GLASS_MIN_RADIUS = 0.004f
private const val GLASS_MAX_RADIUS = 0.011f
private const val TRAIL_LENGTH = 6f
private const val TRAIL_FADE = 0.45f
