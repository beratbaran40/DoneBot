package com.todoapp.mobile.ui.pomodoro

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.todoapp.mobile.ui.pomodoro.PomodoroContract.UiState
import com.todoapp.uikit.theme.TDTheme

/**
 * Everything the timer layouts need to paint themselves, resolved once so portrait and landscape
 * can't drift apart. Both used to derive this block independently — six `animateColorAsState`
 * calls each, copy-pasted.
 */
@Immutable
data class PomodoroVisuals(
    val background: Color,
    val surface: Color,
    val content: Color,
    val track: Color,
    val lightShadow: Color,
    val darkShadow: Color,
    /** Ring fill, 1f at the start of a session draining to 0f — already smoothed between ticks. */
    val progress: Float,
)

/**
 * Crossfades the whole palette whenever the session changes mode, and eases the ring between the
 * engine's one-second ticks so it sweeps instead of stepping.
 */
@Composable
fun rememberPomodoroVisuals(uiState: UiState): PomodoroVisuals {
    val isDark = TDTheme.isDark
    val palette = TDTheme.palette
    val colors = TDTheme.colors
    val target =
        remember(uiState.mode.colorKey, isDark, palette, colors) {
            PomodoroModeTheme.resolve(uiState.mode.colorKey, isDark, palette, colors)
        }

    // A stepped kit ticks its animations in whole frames instead of gliding. Both here and on the
    // ring below the rounded kits keep exactly the curve they had: emphasizedEasing is FastOutSlowIn
    // for them, which is what tween() already defaulted to, and the ring's sweep stays linear.
    val motion = TDTheme.motion
    val colorSpec = tween<Color>(COLOR_ANIM_MS, easing = motion.emphasizedEasing)
    val background by animateColorAsState(target.background, colorSpec, "pomoBg")
    val surface by animateColorAsState(target.surface, colorSpec, "pomoSurface")
    val content by animateColorAsState(target.content, colorSpec, "pomoContent")
    val track by animateColorAsState(target.track, colorSpec, "pomoTrack")
    val lightShadow by animateColorAsState(target.lightShadow, colorSpec, "pomoLightShadow")
    val darkShadow by animateColorAsState(target.darkShadow, colorSpec, "pomoDarkShadow")

    // derivedStateOf so the ring is recomputed only when the clock actually moves, not when an
    // unrelated field of UiState changes.
    val progressFraction by remember(uiState.min, uiState.second, uiState.totalSessionSeconds) {
        derivedStateOf {
            val remaining = uiState.min * SECONDS_PER_MINUTE + uiState.second
            val total = uiState.totalSessionSeconds
            if (total > 0L) remaining.toFloat() / total.toFloat() else 1f
        }
    }
    val progress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec =
        tween(
            durationMillis = PROGRESS_ANIM_MS,
            easing = if (motion.stepped) motion.standardEasing else LinearEasing,
        ),
        label = "pomoProgress",
    )

    return PomodoroVisuals(
        background = background,
        surface = surface,
        content = content,
        track = track,
        lightShadow = lightShadow,
        darkShadow = darkShadow,
        progress = progress,
    )
}

private const val COLOR_ANIM_MS = 400
private const val PROGRESS_ANIM_MS = 900
private const val SECONDS_PER_MINUTE = 60L
