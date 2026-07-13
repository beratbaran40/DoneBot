package com.todoapp.uikit.theme

import androidx.compose.ui.graphics.Color

/**
 * A selectable app color "kit". [ORIGINAL] is the app's classic purple/blue palette (the default);
 * [MONOCHROME] is the Notion-style neutral chrome + graph-paper grid, keeping the app's characteristic
 * semantic accents (blue = one-time, orange = pomodoro/reminders, green = done, red = error).
 * The chosen kit is provided by [TDTheme]; every screen consumes it transparently via `TDTheme.colors`.
 */
enum class PaletteKit {
    ORIGINAL,
    MONOCHROME,
}

private fun PaletteKit.resolveColors(dark: Boolean): TDColor = when (this) {
    PaletteKit.ORIGINAL -> if (dark) defaultDarkColors() else defaultLightColors()
    PaletteKit.MONOCHROME -> if (dark) monochromeDarkColors() else monochromeLightColors()
}

/**
 * `(background, gridLine)` for rendering a preview of this kit's card body. Feeding these to
 * `Modifier.gridBackground(...)` yields the graph-paper grid for MONOCHROME and a plain fill for
 * ORIGINAL (whose `gridLine` is `Color.Transparent`, so the modifier's tiling short-circuits).
 * Resolves the kit's own factory regardless of the currently active theme.
 */
fun PaletteKit.gridColors(dark: Boolean): Pair<Color, Color> {
    val colors = resolveColors(dark)
    return colors.background to colors.gridLine
}

/**
 * An edge-to-edge palette strip that visually distinguishes the kits: ORIGINAL a chromatic
 * purple/blue ramp, MONOCHROME a neutral ink→gray ladder punctuated by its characteristic accents
 * (blue / orange / green / red). Resolves the kit's own factory regardless of the active theme.
 */
fun PaletteKit.stripColors(dark: Boolean): List<Color> {
    val c = resolveColors(dark)
    return when (this) {
        PaletteKit.ORIGINAL ->
            listOf(c.darkPurple, c.purple, c.lightPurple, c.orange, c.mediumGreen, c.crossRed)
        PaletteKit.MONOCHROME ->
            listOf(c.onBackground, c.gray, c.lightGray, c.purple, c.orange, c.mediumGreen, c.crossRed)
    }
}
