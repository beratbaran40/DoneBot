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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

/** Grid rendering style for [gridBackground]. */
enum class GridStyle { Lines, Dots }

/**
 * Draws a Notion-style graph-paper grid behind content: a base fill, then hairline lines
 * (or dots) tiled at a fixed [spacing]. Colors are passed explicitly — this is a plain (not
 * `@Composable`) `Modifier`, so callers supply `TDTheme.colors.background` +
 * `TDTheme.colors.gridLine`, mirroring [neumorphicShadow].
 *
 * `drawBehind` re-runs only on size change (never per frame unless animated) and uses draw
 * primitives with value-class [Offset] — no heap allocation.
 */
fun Modifier.gridBackground(
    baseColor: Color,
    lineColor: Color,
    spacing: Dp = 24.dp,
    lineWidth: Dp = 1.dp,
    style: GridStyle = GridStyle.Lines,
): Modifier = this.drawBehind {
    drawRect(color = baseColor)
    // Transparent line color (e.g. the ORIGINAL palette sets gridLine = Transparent) => solid fill,
    // no grid: skip the tiling loop entirely.
    if (lineColor.alpha == 0f) return@drawBehind
    val step = spacing.toPx()
    if (step <= 0f) return@drawBehind
    val stroke = lineWidth.toPx()
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
