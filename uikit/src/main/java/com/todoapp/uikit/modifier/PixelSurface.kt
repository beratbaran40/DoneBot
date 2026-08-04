package com.todoapp.uikit.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp

private const val BEVEL_LIGHT_ALPHA = 0.30f
private const val BEVEL_SHADE_ALPHA = 0.22f

/** Bevel bands are two border-widths thick — one visible "pixel" on each side of the outline. */
private const val BEVEL_THICKNESS_FACTOR = 2f

/**
 * 8-bit surface: a **zero-blur** offset drop shadow, a flat fill, a one-block inner bevel (light on
 * the top/left edges, shade on the bottom/right) and a hard outline — all in a single `drawBehind`.
 * No `BlurMaskFilter`, no `graphicsLayer`, no per-frame allocation beyond the silhouette path.
 *
 * Colours are passed explicitly because this is a plain, non-`@Composable` modifier, mirroring
 * [neumorphicShadow] and [gridBackground]. Callers that only need the shadow (because they already
 * paint their own background) should use `Modifier.tdShadow`, which dispatches on the active kit.
 */
fun Modifier.pixelSurface(
    fill: Color,
    outline: Color,
    shape: Shape,
    borderWidth: Dp,
    shadowOffset: Dp,
): Modifier = this.drawBehind {
    val silhouette = toPath(shape)
    val border = borderWidth.toPx()
    val drop = shadowOffset.toPx()

    if (drop > 0f) {
        translate(left = drop, top = drop) { drawPath(silhouette, color = outline) }
    }
    drawPath(silhouette, color = fill)

    if (border > 0f) {
        clipPath(silhouette) {
            drawBevel(thickness = border * BEVEL_THICKNESS_FACTOR)
            // Stroking at 2x and clipping to the silhouette leaves an inset edge of exactly
            // `border` px, which a centred stroke on a generic path would not.
            drawPath(silhouette, color = outline, style = Stroke(width = border * 2f))
        }
    }
}

/**
 * The hard drop shadow on its own — an offset copy of [shape] in [color], no blur. Stacks under a
 * caller-supplied background, so it is the [pixelSurface] counterpart for components that already
 * paint themselves.
 */
internal fun Modifier.hardShadow(
    color: Color,
    shape: Shape,
    offset: Dp,
): Modifier = this.drawBehind {
    val drop = offset.toPx()
    if (drop <= 0f) return@drawBehind
    val silhouette = toPath(shape)
    translate(left = drop, top = drop) { drawPath(silhouette, color = color) }
}

/**
 * Resolves a [Shape] to a [Path] at the current draw size. Going through a path (rather than the
 * per-variant draw calls) means rounded, rectangular and generic pixel outlines all render, fill and
 * clip through one code path.
 */
private fun DrawScope.toPath(shape: Shape): Path = Path().apply {
    addOutline(shape.createOutline(size, layoutDirection, this@toPath))
}

/** Light band along the top and left edges, shade along the bottom and right — a raised block. */
private fun DrawScope.drawBevel(thickness: Float) {
    val light = Color.White.copy(alpha = BEVEL_LIGHT_ALPHA)
    val shade = Color.Black.copy(alpha = BEVEL_SHADE_ALPHA)
    drawRect(light, size = Size(size.width, thickness))
    drawRect(light, size = Size(thickness, size.height))
    drawRect(
        shade,
        topLeft = Offset(0f, size.height - thickness),
        size = Size(size.width, thickness),
    )
    drawRect(
        shade,
        topLeft = Offset(size.width - thickness, 0f),
        size = Size(thickness, size.height),
    )
}
