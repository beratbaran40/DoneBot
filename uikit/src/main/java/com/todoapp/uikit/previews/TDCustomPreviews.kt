package com.todoapp.uikit.previews

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

@Preview(
    name = "Light",
    showBackground = true,
    widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Preview(
    name = "Dark",
    showBackground = true,
    widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
annotation class TDPreview

@Preview(
    name = "Light (NoBg)",
    showBackground = false,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Preview(
    name = "Dark (NoBg)",
    showBackground = false,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
annotation class TDPreviewNoBg

@Preview(
    name = "Light – Wide",
    showBackground = true,
    widthDp = 411,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Preview(
    name = "Dark – Wide",
    showBackground = true,
    widthDp = 411,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
annotation class TDPreviewWide

@Preview(
    name = "Light – Dialog",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Preview(
    name = "Dark – Dialog",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
annotation class TDPreviewDialog

@Preview(
    name = "Light – Form",
    showBackground = true,
    widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Preview(
    name = "Dark – Form",
    showBackground = true,
    widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
annotation class TDPreviewForm

/**
 * Device-width matrix (single theme) to catch horizontal overflow across small→large phones.
 * Narrow ≈ split-screen and 320dp phones, Small ≈ Pixel 4a (~344dp), Medium ≈ Pixel 7 (360dp),
 * Large ≈ Pixel 7 Pro (411dp).
 */
@Preview(name = "Narrow · 320", showBackground = true, widthDp = 320)
@Preview(name = "Small · 344", showBackground = true, widthDp = 344)
@Preview(name = "Medium · 360", showBackground = true, widthDp = 360)
@Preview(name = "Large · 411", showBackground = true, widthDp = 411)
annotation class TDPreviewDevices

/**
 * The squeeze matrix, for anything that lays text out inside a bounded box.
 *
 * Three separate things shrink the room a label has, and only one of them is the screen: a 320dp
 * width, a larger system font, and — in the PIXEL kit — a wider typeface. This annotation covers the
 * first two. The face has to be set in the preview body (`TDTheme(palette = PaletteKit.PIXEL)`),
 * because a palette cannot be chosen from an annotation.
 *
 * Single theme on purpose, like [TDPreviewDevices]: overflow is a layout failure, not a colour one,
 * and doubling every cell for dark mode doubles the render cost for nothing. 1.3 rather than the 2.0
 * extreme — a layout that survives 1.3 degrades gracefully past it, and one that fails at 1.3 is
 * already broken for a large share of real users.
 */
@Preview(name = "Narrow · 320", showBackground = true, widthDp = 320)
@Preview(name = "Narrow · 320 · font 1.3", showBackground = true, widthDp = 320, fontScale = 1.3f)
@Preview(name = "Medium · 360 · font 1.3", showBackground = true, widthDp = 360, fontScale = 1.3f)
annotation class TDPreviewNarrow
