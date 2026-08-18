package com.todoapp.mobile.ui.pomodoro.ambience

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.pow

/**
 * A fire at the bottom of the screen: three overlapping tongues whose silhouettes wobble out of
 * phase, embers drifting up from them, and a warm glow that breathes over the whole surface.
 *
 * The [Path] is allocated once and rewritten in place each frame — the one piece of mutable state
 * in the ambience scenes, and the alternative (a fresh Path per tongue per frame) would be three
 * allocations at 60 Hz.
 */
@Composable
fun FireplaceScene(
    clock: State<Float>,
    tint: Color,
    ink: AmbienceInk,
    isDark: Boolean,
    cell: Dp,
    modifier: Modifier = Modifier,
) {
    val embers = remember { embers(seed = 4_409) }
    val flamePath = remember { Path() }
    val glowAlpha = if (isDark) DARK_GLOW_ALPHA else LIGHT_GLOW_ALPHA
    val flameAlpha = if (isDark) DARK_FLAME_ALPHA else LIGHT_FLAME_ALPHA

    Spacer(
        modifier.drawBehind {
            val t = clock.value
            val grid = SceneGrid(if (cell > 0.dp) cell.toPx() else 1f)
            drawRect(tint)
            drawGlow(grid, t, glowAlpha, ink.glow)
            TONGUES.forEachIndexed { i, tongue ->
                if (grid.quantised) {
                    drawBlockTongue(grid, t, tongue, flameAlpha, ink.flames[i])
                } else {
                    drawTongue(flamePath, t, tongue, flameAlpha, ink.flames[i])
                }
            }
            drawEmbers(grid, embers, t, flameAlpha, ink.ember)
        },
    )
}

/** The hearth's light on the room, pulsing gently — the cue that reads first at a glance. */
private fun DrawScope.drawGlow(
    grid: SceneGrid,
    time: Float,
    alpha: Float,
    glow: Color,
) {
    val breath = wave(time, GLOW_SPEED, 0f)
    val flicker = wave(time, FLICKER_SPEED, 1.7f)
    // Hardware with a handful of colours lit a small area brightly rather than washing the screen,
    // and a banded falloff over a huge radius is just a stack of flat panels. Pulling the reach in
    // lets the bands read as light instead of as backdrop.
    val reach = if (grid.quantised) BLOCK_GLOW_REACH else 1f
    val radius = size.maxDimension * (GLOW_RADIUS_BASE + breath * GLOW_RADIUS_SWING) * reach
    val core = glow.copy(alpha = alpha * (GLOW_CORE + flicker * GLOW_FLICKER) * if (grid.quantised) BLOCK_GLOW_ALPHA else 1f)
    val center = Offset(size.width * HEARTH_X, size.height * HEARTH_Y)
    // On a grid the light falls off in steps: hardware with a handful of colours could not fade, so
    // it banded. Doubling each stop makes the gradient hold its value and then jump.
    val stops =
        if (grid.quantised) {
            // Each band is entered and left at the same alpha, so the gradient holds its value and
            // then jumps. A single stop per band would just be a piecewise-linear fade.
            Array(GLOW_BANDS * 2) { i ->
                val band = i / 2
                val edge = (if (i % 2 == 0) band else band + 1).toFloat() / GLOW_BANDS
                edge to core.copy(alpha = core.alpha * (1f - band.toFloat() / GLOW_BANDS))
            }
        } else {
            arrayOf(0f to core, 1f to Color.Transparent)
        }
    drawCircle(
        brush = Brush.radialGradient(colorStops = stops, center = center, radius = radius),
        radius = radius,
        center = center,
    )
}

/**
 * One tongue of flame: a closed shape whose two sides are cubics with control points that sway
 * out of phase, so the silhouette never repeats exactly within a session.
 */
private fun DrawScope.drawTongue(
    path: Path,
    time: Float,
    tongue: Tongue,
    alpha: Float,
    flame: FlameInk,
) {
    val baseY = size.height
    val centerX = size.width * tongue.x
    val height = size.height * tongue.height * (TONGUE_BASE + wave(time, tongue.speed, tongue.phase) * TONGUE_SWING)
    val halfWidth = size.width * tongue.width
    val sway = (wave(time, tongue.speed * SWAY_RATIO, tongue.phase + 1f) - HALF_UNIT) * halfWidth * SWAY_REACH
    val tipY = baseY - height

    path.reset()
    path.moveTo(centerX - halfWidth, baseY)
    path.cubicTo(
        centerX - halfWidth, baseY - height * WAIST,
        centerX - halfWidth * SHOULDER + sway, baseY - height * SHOULDER_Y,
        centerX + sway, tipY,
    )
    path.cubicTo(
        centerX + halfWidth * SHOULDER + sway, baseY - height * SHOULDER_Y,
        centerX + halfWidth, baseY - height * WAIST,
        centerX + halfWidth, baseY,
    )
    path.close()

    drawPath(
        path = path,
        brush =
        Brush.verticalGradient(
            colors = listOf(flame.tip.copy(alpha = alpha * TIP_ALPHA), flame.root.copy(alpha = alpha)),
            startY = tipY,
            endY = baseY,
        ),
    )
}

/**
 * The same tongue, built from whole cells instead of two cubics.
 *
 * A curve cannot be quantised after the fact without shimmering along the cell edges, so the pixel
 * silhouette is derived rather than sampled: each column's height comes from a bump around the
 * swayed axis, and the colour steps through a four-entry ramp on the way up. That is how a sprite
 * fire was actually drawn, and it holds still between frames because the column index is a function
 * of time exactly as the smooth path's control points were.
 */
private fun DrawScope.drawBlockTongue(
    grid: SceneGrid,
    time: Float,
    tongue: Tongue,
    alpha: Float,
    flame: FlameInk,
) {
    val baseY = size.height
    val centerX = size.width * tongue.x
    val height = size.height * tongue.height * (TONGUE_BASE + wave(time, tongue.speed, tongue.phase) * TONGUE_SWING)
    val halfWidth = size.width * tongue.width
    val sway = (wave(time, tongue.speed * SWAY_RATIO, tongue.phase + 1f) - HALF_UNIT) * halfWidth * SWAY_REACH
    val axis = centerX + sway
    val columns = ceil(halfWidth * 2f / grid.cell).toInt().coerceAtLeast(1)

    for (i in 0 until columns) {
        val x = grid.snap(centerX - halfWidth + i * grid.cell)
        val u = ((x + grid.cell * HALF_UNIT) - axis) / halfWidth
        val bump = (1f - u * u).coerceAtLeast(0f)
        val columnHeight = height * bump * bump.pow(BLOCK_TAPER)
        if (columnHeight < grid.cell) continue
        val rows = (columnHeight / grid.cell).toInt()
        // One rect per colour band rather than per cell. Adjacent cells of the same colour are
        // indistinguishable once drawn, so this is the same picture for a sixth of the draw calls —
        // and the tallest tongue is 60-odd cells high at 60 Hz.
        for (band in 0 until BLOCK_RAMP_STEPS) {
            val fromRow = rows * band / BLOCK_RAMP_STEPS
            val toRow = rows * (band + 1) / BLOCK_RAMP_STEPS
            if (toRow <= fromRow) continue
            val rise = band.toFloat() / (BLOCK_RAMP_STEPS - 1)
            drawRect(
                color = banded(flame.root, flame.tip, rise, BLOCK_RAMP_STEPS)
                    .copy(alpha = alpha * (1f - rise * (1f - TIP_ALPHA))),
                topLeft = Offset(x, grid.snap(baseY - toRow * grid.cell)),
                size = Size(grid.cell, (toRow - fromRow) * grid.cell),
            )
        }
    }
}

private fun DrawScope.drawEmbers(
    grid: SceneGrid,
    embers: List<Ember>,
    time: Float,
    alpha: Float,
    emberInk: Color,
) {
    embers.forEach { ember ->
        val rise = fract(ember.phase + time * ember.speed)
        // Embers cool and shrink as they climb, and vanish before the top edge.
        val presence = (1f - rise) * (1f - rise)
        val y = size.height * (1f - rise * ember.reach)
        val drift = kotlin.math.sin(time * ember.driftSpeed + ember.phase * TAU) * size.width * ember.driftReach
        sceneDot(
            grid = grid,
            color = emberInk.copy(alpha = alpha * presence * EMBER_ALPHA),
            center = Offset(size.width * ember.x + drift, y),
            radius = size.minDimension * ember.radius * presence,
        )
    }
}

private fun embers(seed: Int): List<Ember> {
    val random = sceneRandom(seed)
    return List(EMBER_COUNT) {
        Ember(
            x = HEARTH_X + (random.nextFloat() - HALF_UNIT) * EMBER_SPREAD,
            speed = EMBER_MIN_SPEED + random.nextFloat() * (EMBER_MAX_SPEED - EMBER_MIN_SPEED),
            phase = random.nextFloat(),
            radius = EMBER_MIN_RADIUS + random.nextFloat() * (EMBER_MAX_RADIUS - EMBER_MIN_RADIUS),
            reach = EMBER_MIN_REACH + random.nextFloat() * (EMBER_MAX_REACH - EMBER_MIN_REACH),
            driftSpeed = EMBER_DRIFT_MIN + random.nextFloat() * (EMBER_DRIFT_MAX - EMBER_DRIFT_MIN),
            driftReach = random.nextFloat() * EMBER_DRIFT_REACH,
        )
    }
}

private class Tongue(
    val x: Float,
    val width: Float,
    val height: Float,
    val speed: Float,
    val phase: Float,
)

private class Ember(
    val x: Float,
    val speed: Float,
    val phase: Float,
    val radius: Float,
    val reach: Float,
    val driftSpeed: Float,
    val driftReach: Float,
)

// Fire keeps its own colours in every palette kit, like the rest of the Pomodoro mode surface.

private val TONGUES =
    listOf(
        Tongue(0.5f, 0.30f, 0.34f, 1.10f, 0.0f),
        Tongue(0.38f, 0.19f, 0.24f, 1.55f, 2.1f),
        Tongue(0.62f, 0.16f, 0.20f, 1.90f, 4.3f),
    )

private const val HEARTH_X = 0.5f
private const val HEARTH_Y = 0.96f
private const val HALF_UNIT = 0.5f

private const val DARK_GLOW_ALPHA = 0.55f
private const val LIGHT_GLOW_ALPHA = 0.34f
private const val DARK_FLAME_ALPHA = 0.62f
private const val LIGHT_FLAME_ALPHA = 0.42f

private const val GLOW_SPEED = 0.7f
private const val FLICKER_SPEED = 4.3f
private const val GLOW_RADIUS_BASE = 0.55f
private const val GLOW_RADIUS_SWING = 0.10f
private const val GLOW_CORE = 0.72f
private const val GLOW_FLICKER = 0.28f

private const val TONGUE_BASE = 0.80f
private const val TONGUE_SWING = 0.30f
private const val SWAY_RATIO = 0.63f
private const val SWAY_REACH = 0.55f
private const val WAIST = 0.30f
private const val SHOULDER = 0.72f
private const val SHOULDER_Y = 0.74f
private const val TIP_ALPHA = 0.15f

private const val BLOCK_RAMP_STEPS = 4
private const val GLOW_BANDS = 5
private const val BLOCK_GLOW_REACH = 0.45f
private const val BLOCK_GLOW_ALPHA = 0.55f
private const val BLOCK_TAPER = 0.35f

private const val EMBER_COUNT = 22
private const val EMBER_ALPHA = 0.85f
private const val EMBER_SPREAD = 0.42f
private const val EMBER_MIN_SPEED = 0.07f
private const val EMBER_MAX_SPEED = 0.17f
private const val EMBER_MIN_RADIUS = 0.0035f
private const val EMBER_MAX_RADIUS = 0.008f
private const val EMBER_MIN_REACH = 0.45f
private const val EMBER_MAX_REACH = 0.85f
private const val EMBER_DRIFT_MIN = 0.5f
private const val EMBER_DRIFT_MAX = 1.4f
private const val EMBER_DRIFT_REACH = 0.06f
