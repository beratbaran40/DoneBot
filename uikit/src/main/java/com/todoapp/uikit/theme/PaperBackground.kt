package com.todoapp.uikit.theme

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Layers a "lined notebook page" background on the receiver: paper-coloured fill,
 * faint horizontal rules, and a thin coloured margin on the left. Use to give
 * journal-style surfaces a tactile feel.
 *
 * Colours come from [TDTheme] by default so the modifier follows light/dark mode.
 *
 * @param lineSpacing distance between successive horizontal rules. Tune per
 *   surface — list cards want denser spacing than long-form editors.
 */
@Composable
fun Modifier.paperBackground(
    paperColor: Color = TDTheme.colors.white,
    lineColor: Color = TDTheme.colors.lightGray.copy(alpha = LINE_ALPHA),
    marginColor: Color = TDTheme.colors.crossRed,
    lineSpacing: Dp = 28.dp,
    marginX: Dp = 28.dp,
    marginWidth: Dp = 2.dp,
    headerLineWidth: Dp = 1.dp,
    headerLineColor: Color = lineColor,
): Modifier = this
    .background(paperColor)
    .drawBehind {
        val spacingPx = lineSpacing.toPx()
        val marginPx = marginX.toPx()
        val strokePx = 1.dp.toPx()
        val headerStrokePx = headerLineWidth.toPx()

        // First rule (just below the would-be title line) is drawable as a thicker accent
        // — callers can use this to visually divide the title from the body content.
        var y = spacingPx
        var first = true
        while (y < size.height) {
            drawLine(
                color = if (first) headerLineColor else lineColor,
                start = Offset(marginPx, y),
                end = Offset(size.width, y),
                strokeWidth = if (first) headerStrokePx else strokePx,
            )
            y += spacingPx
            first = false
        }

        drawLine(
            color = marginColor,
            start = Offset(marginPx, 0f),
            end = Offset(marginPx, size.height),
            strokeWidth = marginWidth.toPx(),
        )
    }

/**
 * Overload that derives [lineSpacing] from a [TextStyle]'s `lineHeight` so each typed line
 * lands on a paper rule. Useful when the receiver hosts a BasicTextField — pass the same
 * text style to both the field and this modifier.
 */
@Composable
fun Modifier.paperBackground(
    textStyle: TextStyle,
    paperColor: Color = TDTheme.colors.white,
    lineColor: Color = TDTheme.colors.lightGray.copy(alpha = LINE_ALPHA),
    marginColor: Color = TDTheme.colors.crossRed,
    marginX: Dp = 28.dp,
    marginWidth: Dp = 2.dp,
    headerLineWidth: Dp = 1.dp,
    headerLineColor: Color = lineColor,
): Modifier {
    val density = LocalDensity.current
    val spacing = with(density) { textStyle.lineHeight.toDp() }
    return paperBackground(
        paperColor = paperColor,
        lineColor = lineColor,
        marginColor = marginColor,
        lineSpacing = spacing,
        marginX = marginX,
        marginWidth = marginWidth,
        headerLineWidth = headerLineWidth,
        headerLineColor = headerLineColor,
    )
}

private const val LINE_ALPHA = 0.45f
