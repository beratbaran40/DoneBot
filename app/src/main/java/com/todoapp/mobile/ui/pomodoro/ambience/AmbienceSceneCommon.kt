package com.todoapp.mobile.ui.pomodoro.ambience

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import kotlin.math.PI
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
