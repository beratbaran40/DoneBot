package com.todoapp.mobile.ui.pomodoro.ambience

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.round
import kotlin.math.sin
import kotlin.random.Random

/**
 * Shared plumbing for the Pomodoro ambience scenes.
 *
 * Two rules hold all of these together and both exist to keep a screen-filling loop off the
 * jank budget:
 *
 *  - **One clock, read in the draw phase.** [rememberSceneClock] publishes elapsed seconds into a
 *    [State] that scenes read *inside* their draw lambda. Compose then invalidates drawing only —
 *    no recomposition, no relayout, once per frame for the whole screen.
 *  - **Everything is a pure function of time.** Particle positions are computed from `t` and a
 *    per-particle constant rather than stepped and stored, so there is no mutable state to
 *    allocate or advance, and `t = 0` is a valid still frame for previews and reduce-motion.
 */

/**
 * Elapsed seconds since the scene appeared, or a constant 0 when [enabled] is false.
 *
 * Read the returned state from inside a `DrawScope` block, never from a composable body — that
 * is the whole point of returning a [State] instead of a plain `Float`.
 */
@Composable
fun rememberSceneClock(enabled: Boolean): State<Float> {
    val seconds = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(enabled) {
        if (!enabled) {
            seconds.floatValue = 0f
            return@LaunchedEffect
        }
        val startMillis = withFrameMillis { it }
        while (true) {
            withFrameMillis { frameMillis ->
                seconds.floatValue = (frameMillis - startMillis) / MILLIS_PER_SECOND
            }
        }
    }
    return seconds
}

/**
 * A fixed generator per scene, so a given device always lays its particles out the same way —
 * across recompositions, rotations and previews.
 */
internal fun sceneRandom(seed: Int): Random = Random(seed)

/** Fractional part, always positive. Used to wrap looping motion without a branch. */
internal fun fract(value: Float): Float = value - kotlin.math.floor(value)

/** A slow sine in 0f..1f — the shape most of the "breathing" in these scenes is built from. */
internal fun wave(
    time: Float,
    speed: Float,
    phase: Float,
): Float = (sin(time * speed + phase) + 1f) * HALF

internal const val TAU: Float = (2.0 * PI).toFloat()
private const val MILLIS_PER_SECOND: Float = 1000f
private const val HALF: Float = 0.5f

/**
 * Quantises a scene onto whole cells, so the 8-Bit kit's ambience is drawn by the hardware it is
 * imitating rather than merely coloured like it.
 *
 * The scenes are already pure functions of time, which is what makes this cheap: snapping happens in
 * the arithmetic, not in an offscreen buffer, so a blocky fire costs the same as a smooth one. A cell
 * of 1px or less is the identity and every kit but 8-Bit passes that.
 *
 * Positions are snapped, never rounded from a moving value — a block's cell index is a function of
 * time the same way its position was, so the fire steps between cells instead of shimmering along
 * their edges.
 */
@JvmInline
value class SceneGrid(val cell: Float) {
    val quantised: Boolean get() = cell > 1f

    /** Down to the cell boundary at or before [v]. */
    fun snap(v: Float): Float = if (quantised) floor(v / cell) * cell else v

    /** At least one whole cell, rounded to a whole number of them. */
    fun span(v: Float): Float = if (quantised) max(cell, round(v / cell) * cell) else v
}

/** A dot in the scene: a circle normally, one square cell when the kit is drawing on a grid. */
fun DrawScope.sceneDot(
    grid: SceneGrid,
    color: Color,
    center: Offset,
    radius: Float,
) {
    if (!grid.quantised) {
        drawCircle(color = color, radius = radius, center = center)
        return
    }
    val side = grid.span(radius * 2f)
    drawRect(
        color = color,
        topLeft = Offset(grid.snap(center.x - side / 2f), grid.snap(center.y - side / 2f)),
        size = Size(side, side),
    )
}

/** A streak in the scene: a round-capped line normally, a run of cells on a grid. */
fun DrawScope.sceneStreak(
    grid: SceneGrid,
    color: Color,
    start: Offset,
    end: Offset,
    width: Float,
) {
    if (!grid.quantised) {
        drawLine(color = color, start = start, end = end, strokeWidth = width, cap = StrokeCap.Round)
        return
    }
    val thickness = grid.span(width)
    val steps = max(1, ceil(hypot(end.x - start.x, end.y - start.y) / grid.cell).toInt())
    for (i in 0..steps) {
        val f = i.toFloat() / steps
        drawRect(
            color = color,
            topLeft = Offset(
                grid.snap(start.x + (end.x - start.x) * f),
                grid.snap(start.y + (end.y - start.y) * f),
            ),
            size = Size(thickness, grid.cell),
        )
    }
}

/** Steps a 0..1 ramp into [steps] bands, so a gradient becomes a palette. */
fun banded(
    from: Color,
    to: Color,
    fraction: Float,
    steps: Int,
): Color = lerp(from, to, floor(fraction * steps).coerceIn(0f, steps - 1f) / (steps - 1f))
