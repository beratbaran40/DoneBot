package com.todoapp.uikit.theme

import androidx.compose.ui.graphics.Color

/**
 * A selectable app "kit". [ORIGINAL] is the app's classic purple/blue palette (the default);
 * [MONOCHROME] is the Notion-style neutral chrome + graph-paper grid, keeping the app's characteristic
 * semantic accents (blue = one-time, orange = pomodoro/reminders, green = done, red = error);
 * [PIXEL] is the 8-bit kit — an NES palette plus a pixel face, stair-stepped corners, chunky borders
 * and hard shadows; [TERMINAL] is the CRT phosphor kit — acid green on near-black, a monospace face,
 * scanlines and stepped motion, keeping rounded corners so it needs no artwork of its own.
 *
 * Despite the name a kit now carries more than colour: [colors] resolves its palette and [style] its
 * geometry, typeface and motion. Both are provided by [TDTheme]; screens consume them transparently
 * via `TDTheme.colors` / `TDTheme.shapes` / `TDTheme.style`.
 *
 * The entry NAMES are the persisted DataStore values (see `PaletteRepositoryImpl`) — adding one is
 * safe, renaming one silently resets every user who had it selected.
 */
enum class PaletteKit {
    ORIGINAL,
    MONOCHROME,
    PIXEL,
    TERMINAL,
}

/** This kit's colours for the given mode, resolved independently of the currently active theme. */
internal fun PaletteKit.colors(dark: Boolean): TDColor = when (this) {
    PaletteKit.ORIGINAL -> if (dark) defaultDarkColors() else defaultLightColors()
    PaletteKit.MONOCHROME -> if (dark) monochromeDarkColors() else monochromeLightColors()
    PaletteKit.PIXEL -> if (dark) pixelDarkColors() else pixelLightColors()
    PaletteKit.TERMINAL -> if (dark) terminalDarkColors() else terminalLightColors()
}

/**
 * This kit's non-colour tokens, resolved outside its own theme scope — the same escape hatch as
 * [gridColors] and [stripColors]. Needed by callers such as the theme-change reveal, whose
 * `LaunchedEffect` runs outside the `TDTheme { }` it hosts and so has no composition locals.
 */
fun PaletteKit.style(): TDStyle = when (this) {
    PaletteKit.ORIGINAL -> defaultStyle()
    PaletteKit.MONOCHROME -> monochromeStyle()
    PaletteKit.PIXEL -> pixelStyle()
    PaletteKit.TERMINAL -> terminalStyle()
}

/**
 * `(background, gridLine)` for rendering a preview of this kit's card body. Feeding these to
 * `Modifier.gridBackground(...)` yields the graph-paper grid for MONOCHROME and a plain fill for
 * ORIGINAL (whose `gridLine` is `Color.Transparent`, so the modifier's tiling short-circuits).
 * Resolves the kit's own factory regardless of the currently active theme.
 */
fun PaletteKit.gridColors(dark: Boolean): Pair<Color, Color> {
    val colors = colors(dark)
    return colors.background to colors.gridLine
}

/**
 * An edge-to-edge palette strip that visually distinguishes the kits: ORIGINAL a chromatic
 * purple/blue ramp, MONOCHROME a neutral ink→gray ladder punctuated by its characteristic accents
 * (blue / orange / green / red). Resolves the kit's own factory regardless of the active theme.
 */
fun PaletteKit.stripColors(dark: Boolean): List<Color> {
    val c = colors(dark)
    return when (this) {
        PaletteKit.ORIGINAL ->
            listOf(c.darkPurple, c.purple, c.lightPurple, c.orange, c.mediumGreen, c.crossRed)
        PaletteKit.MONOCHROME ->
            listOf(c.onBackground, c.gray, c.lightGray, c.purple, c.orange, c.mediumGreen, c.crossRed)
        // Reads as an NES sprite ramp rather than a tint ladder: ink, then the four saturated hues.
        PaletteKit.PIXEL ->
            listOf(c.onBackground, c.purple, c.mediumGreen, c.lightYellow, c.orange, c.crossRed)
        // A status line, not a ladder: phosphor text, the cursor green, then done / warn / error.
        PaletteKit.TERMINAL ->
            listOf(c.onBackground, c.primary, c.mediumGreen, c.orange, c.crossRed, c.purple)
    }
}
