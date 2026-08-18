package com.todoapp.mobile.ui.pomodoro.ambience

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.domain.ambience.PomodoroAmbience
import com.todoapp.uikit.previews.TDPreviewWide
import com.todoapp.uikit.theme.PaletteKit
import com.todoapp.uikit.theme.TDTheme

/**
 * The full-bleed backdrop of the Pomodoro timer.
 *
 * [tint] is the mode colour the screen already animates between (Focus green, break blue, overtime
 * red) and it stays the base layer here, so the mode a session is in still reads at a glance — the
 * scene paints its character *over* that rather than replacing it. With
 * [PomodoroAmbience.None] the result is a flat [tint] fill: byte-for-byte the screen's previous
 * behaviour, which is what keeps "ambience off" a zero-risk default.
 *
 * @param animate false collapses every scene to its `t = 0` still frame — see
 *   [com.todoapp.mobile.ui.common.rememberAnimationsEnabled] for what turns it off.
 */
@Composable
fun PomodoroAmbienceScene(
    ambience: PomodoroAmbience,
    tint: Color,
    accent: Color,
    isDark: Boolean,
    animate: Boolean,
    modifier: Modifier = Modifier,
) {
    val clock = rememberSceneClock(enabled = animate && ambience != PomodoroAmbience.None)
    // The scene used to see only the ground it burns against. It needs the mode's own colour too:
    // one kit paints the whole scene in it, another drains it away entirely.
    val palette = TDTheme.palette
    val colors = TDTheme.colors
    // 8-Bit draws the scene on a grid; every other kit passes 0.dp, which is the identity.
    val cell = if (palette == PaletteKit.PIXEL) SceneCell else 0.dp
    val ink = remember(palette, isDark, colors, accent, tint) {
        ambienceInk(palette = palette, isDark = isDark, colors = colors, accent = accent, ground = tint)
    }

    Box(modifier) {
        when (ambience) {
            PomodoroAmbience.None -> Spacer(Modifier.fillMaxSize().background(tint))
            PomodoroAmbience.Fireplace -> FireplaceScene(clock, tint, ink, isDark, cell, Modifier.fillMaxSize())
            PomodoroAmbience.Rain -> RainScene(clock, tint, ink, isDark, cell, Modifier.fillMaxSize())
            PomodoroAmbience.Handpan -> HandpanScene(clock, tint, ink, isDark, cell, Modifier.fillMaxSize())
        }

        if (ambience != PomodoroAmbience.None) {
            // The timer is 96sp of thin type sitting right where the busiest part of every scene
            // is. A soft pool of the base colour under it buys back the contrast the scene spends,
            // without flattening the edges of the screen where the motion actually reads.
            Spacer(
                Modifier
                    .fillMaxSize()
                    // No explicit radius: the default derives it from the surface, so the pool
                    // stays proportional on a tablet instead of shrinking to a spot in the middle.
                    .background(
                        Brush.radialGradient(
                            colors = listOf(tint.copy(alpha = SCRIM_ALPHA), Color.Transparent),
                        ),
                    ),
            )
        }
    }
}

/** Coarse enough that a flame reads as a stack of blocks, fine enough that it still reads as a flame. */
private val SceneCell = 5.dp

private const val SCRIM_ALPHA = 0.55f

// ── Previews ──────────────────────────────────────────────────────────────────
// Scenes render their t = 0 still frame here: LocalInspectionMode forces animate off, and the
// clock is a constant anyway inside a preview.

@TDPreviewWide
@Composable
private fun PomodoroAmbienceSceneFireplacePreview() {
    TDTheme {
        PomodoroAmbienceScene(
            ambience = PomodoroAmbience.Fireplace,
            tint = Color(0xFF1A2E23),
            accent = Color(0xFF48BB78),
            isDark = true,
            animate = false,
            modifier = Modifier.size(360.dp, 720.dp),
        )
    }
}

@TDPreviewWide
@Composable
private fun PomodoroAmbienceSceneRainPreview() {
    TDTheme {
        PomodoroAmbienceScene(
            ambience = PomodoroAmbience.Rain,
            tint = Color(0xFFEBF8FF),
            accent = Color(0xFF48BB78),
            isDark = false,
            animate = false,
            modifier = Modifier.size(360.dp, 720.dp),
        )
    }
}

@TDPreviewWide
@Composable
private fun PomodoroAmbienceSceneHandpanPreview() {
    TDTheme {
        PomodoroAmbienceScene(
            ambience = PomodoroAmbience.Handpan,
            tint = Color(0xFF0D1B2A),
            accent = Color(0xFF48BB78),
            isDark = true,
            animate = false,
            modifier = Modifier.size(360.dp, 720.dp),
        )
    }
}

@TDPreviewWide
@Composable
private fun PomodoroAmbienceSceneNonePreview() {
    TDTheme {
        PomodoroAmbienceScene(
            ambience = PomodoroAmbience.None,
            tint = Color(0xFFF0FFF4),
            accent = Color(0xFF48BB78),
            isDark = false,
            animate = false,
            modifier = Modifier.size(360.dp, 720.dp),
        )
    }
}
