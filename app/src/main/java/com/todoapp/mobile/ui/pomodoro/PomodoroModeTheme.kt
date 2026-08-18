package com.todoapp.mobile.ui.pomodoro

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.todoapp.uikit.theme.PaletteKit
import com.todoapp.uikit.theme.TDColor

data class PomodoroModePalette(
    val background: Color,
    val surface: Color,
    val content: Color,
    val track: Color,
    val lightShadow: Color,
    val darkShadow: Color,
)

object PomodoroModeTheme {
    private val focusLight =
        PomodoroModePalette(
            background = Color(0xFFF0FFF4),
            surface = Color(0xFFD4EDDA),
            content = Color(0xFF1A4731),
            track = Color(0xFFB7DFCA),
            lightShadow = Color(0xFFFFFFFF).copy(alpha = 0.85f),
            darkShadow = Color(0xFF1A4731).copy(alpha = 0.18f),
        )
    private val focusDark =
        PomodoroModePalette(
            background = Color(0xFF1A2E23),
            surface = Color(0xFF2D5A3D),
            content = Color(0xFF48BB78),
            track = Color(0xFF234835),
            lightShadow = Color(0xFF3A7A52).copy(alpha = 0.5f),
            darkShadow = Color(0xFF000000).copy(alpha = 0.45f),
        )

    private val shortBreakLight =
        PomodoroModePalette(
            background = Color(0xFFFFF5F0),
            surface = Color(0xFFFFDDD0),
            content = Color(0xFF7B2D20),
            track = Color(0xFFFAC4B0),
            lightShadow = Color(0xFFFFFFFF).copy(alpha = 0.85f),
            darkShadow = Color(0xFF7B2D20).copy(alpha = 0.18f),
        )
    private val shortBreakDark =
        PomodoroModePalette(
            background = Color(0xFF2D1A15),
            surface = Color(0xFF5C2A20),
            content = Color(0xFFFC8181),
            track = Color(0xFF3D201A),
            lightShadow = Color(0xFF7A3A30).copy(alpha = 0.5f),
            darkShadow = Color(0xFF000000).copy(alpha = 0.45f),
        )

    private val longBreakLight =
        PomodoroModePalette(
            background = Color(0xFFEBF8FF),
            surface = Color(0xFFBEE3F8),
            content = Color(0xFF1A365D),
            track = Color(0xFFA0CDE8),
            lightShadow = Color(0xFFFFFFFF).copy(alpha = 0.85f),
            darkShadow = Color(0xFF1A365D).copy(alpha = 0.18f),
        )
    private val longBreakDark =
        PomodoroModePalette(
            background = Color(0xFF0D1B2A),
            surface = Color(0xFF1A3A5C),
            content = Color(0xFF63B3ED),
            track = Color(0xFF152840),
            lightShadow = Color(0xFF1F4A72).copy(alpha = 0.5f),
            darkShadow = Color(0xFF000000).copy(alpha = 0.45f),
        )

    private val overtimeLight =
        PomodoroModePalette(
            background = Color(0xFFFFF5F5),
            surface = Color(0xFFFED7D7),
            content = Color(0xFF742A2A),
            track = Color(0xFFF9B8B8),
            lightShadow = Color(0xFFFFFFFF).copy(alpha = 0.85f),
            darkShadow = Color(0xFF742A2A).copy(alpha = 0.18f),
        )
    private val overtimeDark =
        PomodoroModePalette(
            background = Color(0xFF2D1515),
            surface = Color(0xFF5C2020),
            content = Color(0xFFF56565),
            track = Color(0xFF3D1C1C),
            lightShadow = Color(0xFF7A2828).copy(alpha = 0.5f),
            darkShadow = Color(0xFF000000).copy(alpha = 0.45f),
        )

    /**
     * The Terminal kit does not tint a mode, it changes the tube. Each mode becomes a single-phosphor
     * screen in its own colour — green for focus, amber for a short break, blue for a long one, red
     * for overtime — which is what a monochrome CRT actually was: one phosphor, chosen at the
     * factory. P1 came in green and P3 in amber, so this is period-correct rather than invented.
     *
     * Every field is derived from that one hue instead of being restated, so a kit token change
     * carries straight through and there is no second copy of these accents to drift from.
     */
    private fun terminal(
        colorKey: ModeColorKey,
        isDark: Boolean,
        colors: TDColor,
    ): PomodoroModePalette {
        val phosphor = when (colorKey) {
            ModeColorKey.Focus -> colors.primary
            ModeColorKey.ShortBreak -> colors.orange
            ModeColorKey.LongBreak -> colors.mediumGreen
            ModeColorKey.OverTime -> colors.crossRed
        }
        // Unlit glass in the dark, tinted paper in the light — the ground carries a trace of the hue
        // either way, so the mode reads before a single glyph is drawn.
        val ground = lerp(colors.background, phosphor, if (isDark) GROUND_MIX_DARK else GROUND_MIX_LIGHT)
        return PomodoroModePalette(
            background = ground,
            surface = lerp(ground, phosphor, PANEL_MIX),
            content = phosphor,
            track = phosphor.copy(alpha = TRACK_ALPHA),
            // The kit defines its surfaces with a hairline, not a shadow, so keep both ends faint
            // enough that the ring reads as drawn rather than lit.
            lightShadow = phosphor.copy(alpha = if (isDark) 0.20f else 0.35f),
            darkShadow = colors.onBackground.copy(alpha = if (isDark) 0.45f else 0.15f),
        )
    }

    fun resolve(
        colorKey: ModeColorKey,
        isDark: Boolean,
        palette: PaletteKit,
        colors: TDColor,
    ): PomodoroModePalette = when (palette) {
        PaletteKit.TERMINAL -> terminal(colorKey, isDark, colors)
        PaletteKit.ORIGINAL, PaletteKit.MONOCHROME, PaletteKit.PIXEL -> when (colorKey) {
            ModeColorKey.Focus -> if (isDark) focusDark else focusLight
            ModeColorKey.ShortBreak -> if (isDark) shortBreakDark else shortBreakLight
            ModeColorKey.LongBreak -> if (isDark) longBreakDark else longBreakLight
            ModeColorKey.OverTime -> if (isDark) overtimeDark else overtimeLight
        }
    }
}

private const val GROUND_MIX_DARK = 0.04f
private const val GROUND_MIX_LIGHT = 0.06f
private const val PANEL_MIX = 0.10f
private const val TRACK_ALPHA = 0.22f
