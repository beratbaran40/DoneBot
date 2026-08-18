// The file's primary export is the gridBackground() Modifier; the GridStyle enum is a supporting
// type, so the filename intentionally follows the function rather than the enum.
@file:Suppress("MatchingDeclarationName")

package com.todoapp.uikit.modifier

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme
import kotlin.math.roundToInt

/**
 * Background texture style for [gridBackground].
 *
 * [Dither] is the ordered 2-cell checkerboard 8-bit hardware used wherever a modern renderer would
 * use a gradient. [Scanlines] is the horizontal-only CRT band pattern — one rule every `spacing`,
 * nothing vertical. Both are drawn through a repeating shader rather than per cell: at a 2dp cell a
 * 411x900dp screen holds ~92,000 cells and at a 4dp scanline pitch a 900dp column holds ~640 bands,
 * which no per-cell loop should pay for on every invalidation.
 */
enum class GridStyle { Lines, Dots, Dither, Scanlines }

/**
 * Draws a texture behind content: a base fill, then hairline lines, dots, or a dither checkerboard
 * tiled at a fixed [spacing]. Colors are passed explicitly — this is a plain (not `@Composable`)
 * `Modifier`, so callers supply `TDTheme.colors.background` + `TDTheme.colors.gridLine`, mirroring
 * [neumorphicShadow].
 *
 * `drawWithCache` builds the dither tile once per size/density change; the per-frame cost is a
 * single `drawRect` with a repeating shader. The line and dot paths draw straight into
 * `onDrawBehind` and allocate nothing.
 */
fun Modifier.gridBackground(
    baseColor: Color,
    lineColor: Color,
    spacing: Dp = 24.dp,
    lineWidth: Dp = 1.dp,
    style: GridStyle = GridStyle.Lines,
): Modifier = this.drawWithCache {
    val step = spacing.toPx()
    // Transparent line color (e.g. the ORIGINAL palette sets gridLine = Transparent) => solid fill.
    val textured = lineColor.alpha > 0f && step > 0f
    val stroke = lineWidth.toPx()

    // Tiled textures build their bitmap here so it survives every frame that does not change the size
    // or density; the per-frame cost is then one drawRect with a repeating shader.
    val tileBrush = when {
        !textured -> null
        style == GridStyle.Dither -> ditherBrush(baseColor, lineColor, step.roundToInt().coerceAtLeast(1))
        style == GridStyle.Scanlines -> {
            // At least two rows per period, and a band that always leaves a gap — otherwise the tile
            // degenerates into a solid fill and the texture silently disappears.
            val cell = step.roundToInt().coerceAtLeast(2)
            scanlineBrush(baseColor, lineColor, cell, stroke.roundToInt().coerceIn(1, cell - 1))
        }
        else -> null
    }

    onDrawBehind {
        if (tileBrush != null) {
            drawRect(tileBrush)
            return@onDrawBehind
        }
        drawRect(color = baseColor)
        if (!textured) return@onDrawBehind
        when (style) {
            GridStyle.Lines -> {
                var x = 0f
                while (x <= size.width) {
                    drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = stroke)
                    x += step
                }
                var y = 0f
                while (y <= size.height) {
                    drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = stroke)
                    y += step
                }
            }
            GridStyle.Dots -> {
                var y = 0f
                while (y <= size.height) {
                    var x = 0f
                    while (x <= size.width) {
                        drawCircle(lineColor, radius = stroke, center = Offset(x, y))
                        x += step
                    }
                    y += step
                }
            }
            // Both handled above through the cached shader.
            GridStyle.Dither, GridStyle.Scanlines -> Unit
        }
    }
}

/** One 2x2-cell ordered-dither tile: the checkerboard 8-bit hardware used in place of a gradient. */
private fun CacheDrawScope.ditherBrush(baseColor: Color, lineColor: Color, cellPx: Int): ShaderBrush {
    val tilePx = cellPx * 2
    val tile = ImageBitmap(tilePx, tilePx, ImageBitmapConfig.Argb8888)
    val tileSize = Size(tilePx.toFloat(), tilePx.toFloat())
    CanvasDrawScope().draw(this, layoutDirection, Canvas(tile), tileSize) {
        drawRect(baseColor)
        val c = cellPx.toFloat()
        drawRect(lineColor, topLeft = Offset(0f, 0f), size = Size(c, c))
        drawRect(lineColor, topLeft = Offset(c, c), size = Size(c, c))
    }
    return ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
}

/**
 * One CRT scanline period: a single-pixel-wide column holding the band and the gap below it. The
 * tile is 1px wide because the pattern has no horizontal structure, so repeating in X is free and
 * the bitmap stays a few dozen bytes. Sized in device pixels so the shader samples 1:1 and the band
 * edge stays hard, exactly as the dither tile does.
 *
 * Internal rather than private because [com.todoapp.uikit.modifier.crtScreen] paints the same bands
 * over artwork, with a transparent base so the image shows through between them.
 */
internal fun CacheDrawScope.scanlineBrush(
    baseColor: Color,
    lineColor: Color,
    cellPx: Int,
    bandPx: Int,
): ShaderBrush {
    val tile = ImageBitmap(1, cellPx, ImageBitmapConfig.Argb8888)
    val tileSize = Size(1f, cellPx.toFloat())
    CanvasDrawScope().draw(this, layoutDirection, Canvas(tile), tileSize) {
        drawRect(baseColor)
        drawRect(lineColor, topLeft = Offset.Zero, size = Size(1f, bandPx.toFloat()))
    }
    return ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
}

@TDPreview
@Composable
private fun TdGridBackgroundPreview() {
    TDTheme {
        Column(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .gridBackground(TDTheme.colors.background, TDTheme.colors.gridLine),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .gridBackground(
                        baseColor = TDTheme.colors.background,
                        lineColor = TDTheme.colors.gridLine,
                        style = GridStyle.Dots,
                    ),
            )
        }
    }
}

/**
 * Colours are literal rather than tokens because no shipping kit selects [GridStyle.Scanlines] yet —
 * a preview reading `TDTheme.colors.gridLine` would resolve ORIGINAL's transparent line and render
 * an empty box, hiding the very draw path this preview exists to show.
 */
@TDPreview
@Composable
private fun TdScanlineBackgroundPreview() {
    TDTheme {
        Box(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
                .gridBackground(
                    baseColor = Color(0xFF050705),
                    lineColor = Color(0xFF0C130C),
                    spacing = 4.dp,
                    lineWidth = 1.dp,
                    style = GridStyle.Scanlines,
                ),
        )
    }
}
