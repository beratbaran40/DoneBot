package com.todoapp.mobile.ui.journal.camera

import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.todoapp.uikit.theme.TDTheme

/**
 * The live viewfinder inside its framing chrome: a hairline outline plus four corner brackets, so
 * the square that actually gets captured stays legible against the blurred backdrop behind it.
 *
 * The frame is drawn as siblings *over* the [PreviewView] rather than as modifiers on it — the
 * preview is a real Android view (SurfaceView-backed), and layering Compose drawing on top of it is
 * the reliable way to decorate it. The outline uses [TDTheme]'s shape, so the PIXEL palette gets its
 * stair-stepped corners for free; the brackets sit inset from the edges and are therefore
 * independent of the corner radius.
 */
@Composable
internal fun PolaroidViewfinder(previewView: PreviewView?, modifier: Modifier = Modifier) {
    val polaroid = TDTheme.colors.polaroid
    val shape = TDTheme.shapes.large

    Box(modifier = modifier) {
        CameraViewfinder(
            previewView = previewView,
            modifier = Modifier
                .fillMaxSize()
                .clip(shape),
        )

        // Two passes: a dark band first, then the cream hairline over its outer edge, so the frame
        // reads against both a bright and a dark scene.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(OUTLINE_SHADE, Color.Black.copy(alpha = 0.30f), shape),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(OUTLINE_WIDTH, polaroid.bodyCreamLip.copy(alpha = 0.9f), shape),
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val inset = BRACKET_INSET.toPx()
            val length = size.minDimension * BRACKET_LENGTH_FRACTION
            drawCornerBrackets(inset, length, BRACKET_STROKE.toPx() + 2f, Color.Black.copy(alpha = 0.35f))
            drawCornerBrackets(inset, length, BRACKET_STROKE.toPx(), polaroid.bodyCreamLip)
        }
    }
}

/** Draws an L at each corner, [inset] in from the edges and [length] long along both axes. */
private fun DrawScope.drawCornerBrackets(inset: Float, length: Float, strokeWidth: Float, color: Color) {
    val right = size.width - inset
    val bottom = size.height - inset
    val corners = listOf(
        Triple(Offset(inset, inset), 1f, 1f),
        Triple(Offset(right, inset), -1f, 1f),
        Triple(Offset(inset, bottom), 1f, -1f),
        Triple(Offset(right, bottom), -1f, -1f),
    )
    corners.forEach { (corner, dirX, dirY) ->
        drawLine(color, corner, Offset(corner.x + length * dirX, corner.y), strokeWidth)
        drawLine(color, corner, Offset(corner.x, corner.y + length * dirY), strokeWidth)
    }
}

private val OUTLINE_SHADE = 3.dp
private val OUTLINE_WIDTH = 1.5.dp
private val BRACKET_INSET = 10.dp
private val BRACKET_STROKE = 2.dp
private const val BRACKET_LENGTH_FRACTION = 0.13f
