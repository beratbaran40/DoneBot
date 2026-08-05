package com.todoapp.mobile.ui.pomodoro.ambience

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope

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
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val embers = remember { embers(seed = 4_409) }
    val flamePath = remember { Path() }
    val glowAlpha = if (isDark) DARK_GLOW_ALPHA else LIGHT_GLOW_ALPHA
    val flameAlpha = if (isDark) DARK_FLAME_ALPHA else LIGHT_FLAME_ALPHA

    Spacer(
        modifier.drawBehind {
            val t = clock.value
            drawRect(tint)
            drawGlow(t, glowAlpha)
            TONGUES.forEach { tongue -> drawTongue(flamePath, t, tongue, flameAlpha) }
            drawEmbers(embers, t, flameAlpha)
        },
    )
}

/** The hearth's light on the room, pulsing gently — the cue that reads first at a glance. */
private fun DrawScope.drawGlow(
    time: Float,
    alpha: Float,
) {
    val breath = wave(time, GLOW_SPEED, 0f)
    val flicker = wave(time, FLICKER_SPEED, 1.7f)
    val radius = size.maxDimension * (GLOW_RADIUS_BASE + breath * GLOW_RADIUS_SWING)
    drawCircle(
        brush =
        Brush.radialGradient(
            colors = listOf(GlowInner.copy(alpha = alpha * (GLOW_CORE + flicker * GLOW_FLICKER)), Color.Transparent),
            center = Offset(size.width * HEARTH_X, size.height * HEARTH_Y),
            radius = radius,
        ),
        radius = radius,
        center = Offset(size.width * HEARTH_X, size.height * HEARTH_Y),
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
            colors = listOf(tongue.tip.copy(alpha = alpha * TIP_ALPHA), tongue.root.copy(alpha = alpha)),
            startY = tipY,
            endY = baseY,
        ),
    )
}

private fun DrawScope.drawEmbers(
    embers: List<Ember>,
    time: Float,
    alpha: Float,
) {
    embers.forEach { ember ->
        val rise = fract(ember.phase + time * ember.speed)
        // Embers cool and shrink as they climb, and vanish before the top edge.
        val presence = (1f - rise) * (1f - rise)
        val y = size.height * (1f - rise * ember.reach)
        val drift = kotlin.math.sin(time * ember.driftSpeed + ember.phase * TAU) * size.width * ember.driftReach
        drawCircle(
            color = EmberColor.copy(alpha = alpha * presence * EMBER_ALPHA),
            radius = size.minDimension * ember.radius * presence,
            center = Offset(size.width * ember.x + drift, y),
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
    val root: Color,
    val tip: Color,
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
private val GlowInner = Color(0xFFFF9D3D)
private val EmberColor = Color(0xFFFFC163)

private val TONGUES =
    listOf(
        Tongue(0.5f, 0.30f, 0.34f, 1.10f, 0.0f, root = Color(0xFFE2521B), tip = Color(0xFFFFC163)),
        Tongue(0.38f, 0.19f, 0.24f, 1.55f, 2.1f, root = Color(0xFFF4761F), tip = Color(0xFFFFD98A)),
        Tongue(0.62f, 0.16f, 0.20f, 1.90f, 4.3f, root = Color(0xFFFF8C2B), tip = Color(0xFFFFE7B0)),
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
