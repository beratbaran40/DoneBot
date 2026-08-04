package com.todoapp.uikit.image

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.todoapp.uikit.theme.TDCornerStyle
import com.todoapp.uikit.theme.TDTheme

/**
 * Source-icon -> pixel-variant lookup used by [tdIconRes].
 *
 * Defaults to `:uikit`'s own icons so uikit components and previews resolve standalone. `:app` owns
 * drawables under a different `R` class, so it provides the MERGED map (uikit + app) once at the
 * theme root — see `ToDoApp`. A resource id that is not in the map resolves to itself.
 */
val LocalPixelIconMap = staticCompositionLocalOf { UikitPixelIcons }

/**
 * Resolves a drawable to its 8-Bit variant when a pixel kit is active, and to itself otherwise.
 *
 * Returning the input unchanged for unmapped ids is what makes the call-site sweep safe: wrapping a
 * raster illustration, a launcher asset, or an icon that has no pixel variant is a no-op rather than
 * a crash or a blank.
 */
@Composable
@ReadOnlyComposable
@DrawableRes
fun tdIconRes(
    @DrawableRes id: Int,
): Int = if (TDTheme.shapes.cornerStyle == TDCornerStyle.PIXEL) {
    LocalPixelIconMap.current[id] ?: id
} else {
    id
}

/**
 * Drop-in for `painterResource(id)` that follows the active kit. Prefer this everywhere an `ic_*`
 * vector is drawn; for raster illustrations use `rememberPixelPainter` instead, which downsamples
 * rather than swapping the asset.
 */
@Composable
fun tdPainter(
    @DrawableRes id: Int,
): Painter = painterResource(tdIconRes(id))
