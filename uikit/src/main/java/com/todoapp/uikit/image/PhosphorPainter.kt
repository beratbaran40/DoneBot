package com.todoapp.uikit.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import com.todoapp.uikit.theme.PaletteKit
import com.todoapp.uikit.theme.TDTheme

/** Rec. 709 luminance weights — the same ones WCAG contrast uses. */
private const val LUMA_R = 0.2126f
private const val LUMA_G = 0.7152f
private const val LUMA_B = 0.0722f

/** A colour matrix's last column is added in 0..255 space, while [Color] channels are 0..1. */
private const val OFFSET_SCALE = 255f

/**
 * Collapses artwork onto a single phosphor ramp: every pixel's luminance picks a point between
 * [dark] and [bright], which is what a one-colour CRT does to whatever signal it is given.
 *
 * Expressed as a plain colour matrix rather than a shader so it runs on every API level this app
 * supports and costs one filter per draw. The alpha row is the identity, so transparency, circular
 * clips and drop shadows all survive untouched.
 */
private fun phosphorFilter(dark: Color, bright: Color): ColorFilter {
    fun row(from: Float, to: Float) = floatArrayOf(LUMA_R * (to - from), LUMA_G * (to - from), LUMA_B * (to - from), 0f, from * OFFSET_SCALE)
    return ColorFilter.colorMatrix(
        ColorMatrix(
            row(dark.red, bright.red) +
                row(dark.green, bright.green) +
                row(dark.blue, bright.blue) +
                floatArrayOf(0f, 0f, 0f, 1f, 0f),
        ),
    )
}

/** Draws [delegate] through [filter]; everything else about the painter is unchanged. */
private class PhosphorPainter(
    private val delegate: Painter,
    private val filter: ColorFilter,
) : Painter() {
    override val intrinsicSize: Size get() = delegate.intrinsicSize

    override fun DrawScope.onDraw() {
        with(delegate) { draw(size, colorFilter = filter) }
    }
}

/**
 * Artwork rendered the way the active kit renders artwork. Returns [painter] untouched for the kits
 * that have no opinion, so call sites can wrap unconditionally — the same contract
 * [rememberPixelPainter] already established.
 *
 * The two opinionated kits cannot both apply: PIXEL downsamples to hard-edged blocks, TERMINAL
 * collapses the image onto its phosphor ramp. [size] is the dp the artwork is drawn at; PIXEL needs
 * it to pick a block count, TERMINAL ignores it.
 */
@Composable
fun rememberKitArtPainter(
    painter: Painter,
    size: Dp,
    blockSize: Int = DEFAULT_BLOCK_SIZE,
): Painter {
    val pixelated = rememberPixelPainter(painter, size, blockSize)
    val filter = tdPhosphorFilter() ?: return pixelated
    return remember(pixelated, filter) { PhosphorPainter(pixelated, filter) }
}

/**
 * The same ramp as a bare [ColorFilter], for the two draw paths that cannot take a [Painter]: Coil's
 * `AsyncImage`, and a raw `drawImage` in a `DrawScope`. `null` for every kit but Terminal, which is
 * exactly what both of those parameters want when nothing should be applied.
 */
@Composable
fun tdPhosphorFilter(): ColorFilter? {
    if (TDTheme.palette != PaletteKit.TERMINAL) {
        return null
    }
    // The ramp inverts between modes because the physics do. A dark screen EMITS light, so the
    // brightest part of the image is the phosphor and the darkest is the unlit glass. Paper ABSORBS
    // it, so the darkest part is the ink and the brightest is the blank sheet.
    val colors = TDTheme.colors
    val ramp =
        if (TDTheme.isDark) {
            colors.background to colors.primary
        } else {
            colors.primary to colors.background
        }
    return remember(ramp) { phosphorFilter(ramp.first, ramp.second) }
}
