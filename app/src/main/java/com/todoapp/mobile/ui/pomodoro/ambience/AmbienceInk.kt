package com.todoapp.mobile.ui.pomodoro.ambience

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.todoapp.uikit.theme.PaletteKit
import com.todoapp.uikit.theme.TDColor

/** Root-to-tip pair for one flame tongue. */
@Immutable
data class FlameInk(val root: Color, val tip: Color)

/**
 * Every colour the three ambience scenes draw with, resolved by the kit before a frame is rendered.
 *
 * The scenes used to keep these as file-private constants, which put them out of reach of the palette
 * and made the Pomodoro screen the one surface in the app that ignored the kit entirely. Passing them
 * as data rather than filtering the finished frame keeps each scene what its own header claims it is —
 * a pure function of time — and avoids an offscreen layer at 60 Hz for a screen that has none today.
 */
@Immutable
data class AmbienceInk(
    val glow: Color,
    val ember: Color,
    val flames: List<FlameInk>,
    val streak: Color,
    val ripple: Color,
)

/** The scenes as authored: a real fire, rain on cold glass, and a violet resonance. */
private fun originalInk(isDark: Boolean) = AmbienceInk(
    glow = Color(0xFFFF9D3D),
    ember = Color(0xFFFFC163),
    flames = listOf(
        FlameInk(root = Color(0xFFE2521B), tip = Color(0xFFFFC163)),
        FlameInk(root = Color(0xFFF4761F), tip = Color(0xFFFFD98A)),
        FlameInk(root = Color(0xFFFF8C2B), tip = Color(0xFFFFE7B0)),
    ),
    streak = if (isDark) Color(0xFFBFD8F0) else Color(0xFF3D5A80),
    ripple = if (isDark) Color(0xFFCBB8F5) else Color(0xFF4C3D80),
)

/**
 * Monochrome spends no colour on the scene so the ring and the numerals are the only chromatic thing
 * on the screen. The fire still reads as fire: its three tongues already differ in brightness and
 * silhouette, and that is what carried it — not the hue.
 */
private fun monochromeInk(colors: TDColor) = AmbienceInk(
    glow = colors.white,
    ember = colors.lightGray,
    flames = listOf(
        FlameInk(root = colors.darkPending, tip = colors.lightGray),
        FlameInk(root = colors.pendingGray, tip = colors.mediumPending),
        FlameInk(root = colors.mediumPending, tip = colors.lightGray),
    ),
    streak = colors.gray,
    ripple = colors.gray,
)

/**
 * Terminal has one phosphor lit at a time, so the scene is a ramp of the mode's own colour: a green
 * fire while focusing, a blue one on a long break, a red one in overtime. A single-colour tube cannot
 * show anything else, which is the point.
 */
private fun terminalInk(phosphor: Color, ground: Color) = AmbienceInk(
    glow = lerp(ground, phosphor, GLOW_MIX),
    ember = phosphor,
    flames = listOf(
        FlameInk(root = lerp(ground, phosphor, ROOT_MIX_LOW), tip = phosphor),
        FlameInk(root = lerp(ground, phosphor, ROOT_MIX_MID), tip = phosphor),
        FlameInk(root = lerp(ground, phosphor, ROOT_MIX_HIGH), tip = phosphor),
    ),
    streak = phosphor,
    ripple = phosphor,
)

/**
 * [accent] is the mode's own colour — the same one the ring and numerals are drawn in — and [ground]
 * the surface it burns against.
 */
fun ambienceInk(
    palette: PaletteKit,
    isDark: Boolean,
    colors: TDColor,
    accent: Color,
    ground: Color,
): AmbienceInk = when (palette) {
    PaletteKit.MONOCHROME -> monochromeInk(colors)
    PaletteKit.TERMINAL -> terminalInk(accent, ground)
    PaletteKit.ORIGINAL, PaletteKit.PIXEL -> originalInk(isDark)
}

private const val GLOW_MIX = 0.60f
private const val ROOT_MIX_LOW = 0.35f
private const val ROOT_MIX_MID = 0.55f
private const val ROOT_MIX_HIGH = 0.75f
