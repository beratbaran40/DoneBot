package com.todoapp.uikit.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * A stair-stepped ("chamfered") corner in the style of an NES dialog box: each corner is cut away by
 * a staircase of [steps] blocks, one block being [unit] on a side. `steps = 1` is a single-block
 * chamfer, `steps = 2` the classic two-stair.
 *
 * Emits an [Outline.Generic], so it composes with `Modifier.clip`, `Modifier.background(shape)` and
 * `Modifier.border(width, color, shape)` exactly like `RoundedCornerShape` — `border` strokes the
 * generic path, which is what gives the outline its stepped silhouette.
 *
 * The block size is clamped so a small badge gets a proportionally shorter stair instead of
 * collapsing into a diamond — see [blockPx].
 *
 * NOTE: a generic outline cannot drive a platform elevation shadow below API 29 (`:uikit` minSdk is
 * 24). Never hand this shape to `Modifier.shadow` — the pixel kit draws its own hard shadow, see
 * `Modifier.tdShadow`.
 */
@Immutable
data class PixelCornerShape(
    val unit: Dp,
    val steps: Int = 2,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val stepCount = steps.coerceAtLeast(1)
        val requestedPx = with(density) { unit.toPx() }
        val block = blockPx(size.width, size.height, requestedPx, stepCount)
        if (block <= 0f) return Outline.Rectangle(size.toRect())

        val width = size.width
        val height = size.height
        val inset = stepCount * block
        val path =
            Path().apply {
                moveTo(0f, inset)
                stairXFirst(stepCount, block, originX = 0f, originY = 0f, signX = 1f, signY = 1f)
                lineTo(width - inset, 0f)
                stairYFirst(stepCount, block, originX = width, originY = 0f, signX = -1f, signY = 1f)
                lineTo(width, height - inset)
                stairXFirst(stepCount, block, originX = width, originY = height, signX = -1f, signY = -1f)
                lineTo(inset, height)
                stairYFirst(stepCount, block, originX = 0f, originY = height, signX = 1f, signY = -1f)
                close()
            }
        return Outline.Generic(path)
    }

    companion object {
        /**
         * Two corners share every edge, so the two staircases together must leave a flat run in the
         * middle. Allowing them half the side each (the obvious clamp) lets them meet exactly at the
         * midpoint on a small square — the shape degenerates into a diamond with zero-length edges.
         * Capping each staircase at a third of the shorter side keeps at least a third of every edge
         * straight, which is what makes a 16dp badge still read as a rectangle.
         */
        private const val MAX_STAIR_FRACTION_OF_SIDE = 3f

        /**
         * Edge length of one stair block, in px, after clamping [requestedPx] to what actually fits
         * in a [width] x [height] box at [steps] steps. Pure geometry — pinned by `PaletteStyleTest`
         * because it is the rule that keeps small chips from collapsing.
         */
        fun blockPx(width: Float, height: Float, requestedPx: Float, steps: Int): Float {
            val stepCount = steps.coerceAtLeast(1)
            val maxPx = minOf(width, height) / (MAX_STAIR_FRACTION_OF_SIDE * stepCount)
            return minOf(requestedPx, maxPx)
        }
    }
}

/**
 * Staircase that steps along X first — the top-left and bottom-right corners when walked clockwise.
 * For `steps = 2` at the top-left this traces `(0,2u) → (u,2u) → (u,u) → (2u,u) → (2u,0)`.
 */
private fun Path.stairXFirst(
    steps: Int,
    block: Float,
    originX: Float,
    originY: Float,
    signX: Float,
    signY: Float,
) {
    for (i in 0 until steps) {
        lineTo(originX + signX * (i + 1) * block, originY + signY * (steps - i) * block)
        lineTo(originX + signX * (i + 1) * block, originY + signY * (steps - i - 1) * block)
    }
}

/**
 * Staircase that steps along Y first — the top-right and bottom-left corners when walked clockwise.
 * For `steps = 2` at the top-right this traces `(w-2u,0) → (w-2u,u) → (w-u,u) → (w-u,2u) → (w,2u)`.
 */
private fun Path.stairYFirst(
    steps: Int,
    block: Float,
    originX: Float,
    originY: Float,
    signX: Float,
    signY: Float,
) {
    for (i in 0 until steps) {
        lineTo(originX + signX * (steps - i) * block, originY + signY * (i + 1) * block)
        lineTo(originX + signX * (steps - i - 1) * block, originY + signY * (i + 1) * block)
    }
}
