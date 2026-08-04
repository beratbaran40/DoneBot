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
 * use a gradient. It is drawn through a repeating shader rather than per cell: at a 2dp cell a
 * 411x900dp screen holds ~92,000 cells, which no per-cell loop can afford.
 */
enum class GridStyle { Lines, Dots, Dither }

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

    // One 2x2-cell tile, repeated by the shader. Built here so it survives every frame that does not
    // change the size or density.
    val ditherBrush = if (textured && style == GridStyle.Dither) {
        val cell = step.roundToInt().coerceAtLeast(1)
        val tilePx = cell * 2
        val tile = ImageBitmap(tilePx, tilePx, ImageBitmapConfig.Argb8888)
        val tileSize = Size(tilePx.toFloat(), tilePx.toFloat())
        CanvasDrawScope().draw(this, layoutDirection, Canvas(tile), tileSize) {
            drawRect(baseColor)
            val c = cell.toFloat()
            drawRect(lineColor, topLeft = Offset(0f, 0f), size = Size(c, c))
            drawRect(lineColor, topLeft = Offset(c, c), size = Size(c, c))
        }
        ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
    } else {
        null
    }

    onDrawBehind {
        if (ditherBrush != null) {
            drawRect(ditherBrush)
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
            // Handled above through the cached shader.
            GridStyle.Dither -> Unit
        }
    }
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
