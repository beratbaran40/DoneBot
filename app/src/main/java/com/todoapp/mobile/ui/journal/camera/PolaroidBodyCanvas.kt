package com.todoapp.mobile.ui.journal.camera

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.todoapp.uikit.theme.PolaroidColors
import com.todoapp.uikit.theme.TDTheme

/**
 * Renders the full skeuomorphic Polaroid OneStep camera body on a single Canvas via
 * [drawWithCache]: geometry + paints are built once per size change, the draw phase only
 * references cached objects and branches on [isShutterPressed]. Colors are read once from
 * [TDTheme] and captured by the cache lambda (which is not `@Composable`).
 */
@Composable
internal fun SkeuomorphicPolaroidCanvas(
    isShutterPressed: Boolean,
    onShutterStateChange: (Boolean) -> Unit,
    onShutterClick: () -> Unit,
) {
    val textMeasurer = rememberTextMeasurer()
    val currentOnShutterClick by rememberUpdatedState(onShutterClick)
    val colors = TDTheme.colors.polaroid
    val brandIcon = ImageBitmap.imageResource(com.todoapp.mobile.R.drawable.img_donebot_new_logo)

    Spacer(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val paddingX = 16.dp.toPx()
                        val paddingTop = 16.dp.toPx()
                        val paddingBottom = 32.dp.toPx()
                        val w = size.width - (paddingX * 2)
                        val h = size.height - (paddingTop + paddingBottom)

                        val trayH = h * 0.28f
                        val lipH = h * 0.048f
                        val ledgeH = h * 0.065f
                        val topBodyH = h - trayH - ledgeH - lipH

                        val shutterCenter = Offset(
                            paddingX + w * 0.20f,
                            paddingTop + topBodyH * 0.68f,
                        )
                        val shutterRadius = w * 0.065f * 1.35f

                        if ((offset - shutterCenter).getDistance() <= shutterRadius) {
                            onShutterStateChange(true)
                            tryAwaitRelease()
                            onShutterStateChange(false)
                            currentOnShutterClick()
                        }
                    },
                )
            }
            .drawWithCache {
                // ── CACHE PHASE: runs once per size change ──
                val paddingX = 16.dp.toPx()
                val paddingTop = 16.dp.toPx()
                val paddingBottom = 32.dp.toPx()

                val w = size.width - (paddingX * 2)
                val h = size.height - (paddingTop + paddingBottom)

                val trayW = w
                val topBodyBottomW = w * 0.94f
                val topBodyTopW = w * 0.88f

                val trayH = h * 0.28f
                val lipH = h * 0.048f
                val ledgeH = h * 0.065f
                val topBodyH = h - trayH - ledgeH - lipH

                val trayLeft = 0f
                val trayRight = w

                val topBodyBottomLeft = (w - topBodyBottomW) / 2f
                val topBodyBottomRight = topBodyBottomLeft + topBodyBottomW
                val topBodyTopLeft = (w - topBodyTopW) / 2f
                val topBodyTopRight = topBodyTopLeft + topBodyTopW

                val topY = 0f
                val ledgeTopY = topBodyH
                val lipTopY = ledgeTopY + ledgeH
                val trayTopY = lipTopY + lipH
                val bottomY = h

                val topRadius = 24.dp.toPx()
                val seamRadius = 3.dp.toPx()
                val bottomRadius = 24.dp.toPx()

                // Cached paths
                val topBumpW = w * 0.48f
                val topBumpH = 14.dp.toPx()
                val topBumpX = (w - topBumpW) / 2f
                val topBumpY = topY - topBumpH + 4f

                val topBumpPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = topBumpX, top = topBumpY,
                            right = topBumpX + topBumpW, bottom = topY + 10f,
                            topLeftCornerRadius = CornerRadius(10.dp.toPx()),
                            topRightCornerRadius = CornerRadius(10.dp.toPx()),
                            bottomLeftCornerRadius = CornerRadius.Zero,
                            bottomRightCornerRadius = CornerRadius.Zero,
                        ),
                    )
                }

                val topBodyPath = Path().apply {
                    moveTo(topBodyBottomLeft, ledgeTopY)
                    lineTo(topBodyTopLeft, topY + topRadius)
                    quadraticTo(topBodyTopLeft, topY, topBodyTopLeft + topRadius, topY)
                    lineTo(topBodyTopRight - topRadius, topY)
                    quadraticTo(topBodyTopRight, topY, topBodyTopRight, topY + topRadius)
                    lineTo(topBodyBottomRight, ledgeTopY)
                    close()
                }

                val lipCornerRadius = 6.dp.toPx()
                val slantRatio = (ledgeH - lipCornerRadius) / ledgeH
                val leftSlantX = topBodyBottomLeft + (trayLeft - topBodyBottomLeft) * slantRatio
                val rightSlantX = topBodyBottomRight + (trayRight - topBodyBottomRight) * slantRatio

                val ledgeAndLipPath = Path().apply {
                    moveTo(topBodyBottomLeft, ledgeTopY)
                    lineTo(topBodyBottomRight, ledgeTopY)
                    lineTo(rightSlantX, lipTopY - lipCornerRadius)
                    quadraticTo(trayRight, lipTopY, trayRight, lipTopY + lipCornerRadius)
                    lineTo(trayRight, trayTopY)
                    lineTo(trayLeft, trayTopY)
                    lineTo(trayLeft, lipTopY + lipCornerRadius)
                    quadraticTo(trayLeft, lipTopY, leftSlantX, lipTopY - lipCornerRadius)
                    close()
                }

                val trayPath = Path().apply {
                    moveTo(trayLeft + seamRadius, trayTopY)
                    lineTo(trayRight - seamRadius, trayTopY)
                    quadraticTo(trayRight, trayTopY, trayRight, trayTopY + seamRadius)
                    lineTo(trayRight, bottomY - bottomRadius)
                    quadraticTo(trayRight, bottomY, trayRight - bottomRadius, bottomY)
                    lineTo(trayLeft + bottomRadius, bottomY)
                    quadraticTo(trayLeft, bottomY, trayLeft, bottomY - bottomRadius)
                    lineTo(trayLeft, trayTopY + seamRadius)
                    quadraticTo(trayLeft, trayTopY, trayLeft + seamRadius, trayTopY)
                    close()
                }

                val silhouettePath = Path().apply {
                    addPath(topBumpPath)
                    addPath(topBodyPath)
                    addPath(ledgeAndLipPath)
                    addPath(trayPath)
                }

                // Cached brushes
                val topBumpGradient = Brush.verticalGradient(
                    listOf(colors.chassisLight, colors.nearBlack),
                    startY = topBumpY, endY = topY,
                )
                val topBumpEdgeBrush = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.25f), Color.Transparent),
                    start = Offset(topBumpX + topBumpW, topBumpY),
                    end = Offset(topBumpX, topBumpY + topBumpH),
                )
                val trayGradient = Brush.verticalGradient(
                    listOf(colors.chassisLight, colors.chassisDark),
                    startY = trayTopY, endY = bottomY,
                )
                val ledgeGradient = Brush.verticalGradient(
                    listOf(colors.bodyCreamEdge, colors.bodyCreamShadow),
                    startY = ledgeTopY, endY = lipTopY,
                )
                val lipGradient = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.8f), colors.bodyCreamLip),
                    startY = lipTopY, endY = trayTopY,
                )
                val bodyGradient = Brush.verticalGradient(
                    listOf(Color.White, colors.bodyCream),
                    startY = topY, endY = ledgeTopY,
                )
                val leftFlareBrush = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.6f), Color.Transparent),
                    start = Offset(topBodyBottomLeft, ledgeTopY),
                    end = Offset(leftSlantX, lipTopY - lipCornerRadius),
                )
                val rightFlareBrush = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.6f), Color.Transparent),
                    start = Offset(topBodyBottomRight, ledgeTopY),
                    end = Offset(rightSlantX, lipTopY - lipCornerRadius),
                )

                // Cached shadow paints (allocated once, not per frame)
                val bodyShadowPaint = blurPaint(Color.Black.copy(alpha = 0.5f), 35f)
                val shadowPaint12 = blurPaint(Color.Black.copy(alpha = 0.3f), 12f)
                val shadowPaint25 = blurPaint(Color.Black.copy(alpha = 0.4f), 25f)
                val shadowPaint15 = blurPaint(Color.Black.copy(alpha = 0.4f), 15f)
                val shadowPaint10 = blurPaint(Color.Black.copy(alpha = 0.4f), 10f)
                val shadowPaint8 = blurPaint(Color.Black.copy(alpha = 0.2f), 8f)
                val specularPaint = blurPaint(Color.White.copy(alpha = 0.85f), 1f)
                val dialHighlightPaint = blurPaint(Color.White.copy(alpha = 0.15f), 4f)

                // Component positions
                val housingCenter = Offset(w / 2f, topBodyH * 0.48f)
                val housingSize = topBodyBottomW * 0.35f
                val stripeStartY = housingCenter.y + (housingSize * 0.35f)
                val shutterCenter = Offset(w * 0.20f, topBodyH * 0.68f)
                val shutterRadius = w * 0.065f
                val flashCenter = Offset(w * 0.79f, topBodyH * 0.22f)
                val flashSize = w * 0.14f
                val dialCenter = Offset(w * 0.77f, topBodyH * 0.65f)
                val dialRadius = w * 0.05f

                // ── DRAW PHASE: runs per frame, uses cached objects ──
                onDrawBehind {
                    withTransform({ translate(left = paddingX, top = paddingTop) }) {
                        // Body shadow
                        drawIntoCanvas { canvas ->
                            canvas.translate(-8f, 30f)
                            canvas.drawPath(silhouettePath, bodyShadowPaint)
                            canvas.translate(8f, -30f)
                        }

                        drawPath(topBumpPath, topBumpGradient)
                        drawPath(topBumpPath, topBumpEdgeBrush, style = Stroke(2f))

                        drawRect(
                            colors.panelSeam,
                            Offset(trayLeft, trayTopY - seamRadius),
                            Size(trayW, seamRadius * 2),
                        )

                        drawPath(trayPath, trayGradient)
                        drawPath(ledgeAndLipPath, ledgeGradient)
                        clipPath(ledgeAndLipPath) {
                            drawRect(lipGradient, Offset(0f, lipTopY), Size(w, trayTopY - lipTopY))
                            drawLine(
                                Color.White.copy(alpha = 0.9f),
                                Offset(0f, lipTopY), Offset(w, lipTopY),
                                strokeWidth = 4f,
                            )
                        }
                        drawPath(topBodyPath, bodyGradient)

                        // Edge highlights and seam lines
                        drawLine(
                            Color.White.copy(alpha = 0.6f),
                            Offset(topBodyTopLeft, topY + topRadius),
                            Offset(topBodyBottomLeft, ledgeTopY),
                            strokeWidth = 3f,
                        )
                        drawLine(
                            Color.Black.copy(alpha = 0.05f),
                            Offset(topBodyTopRight, topY + topRadius),
                            Offset(topBodyBottomRight, ledgeTopY),
                            strokeWidth = 3f,
                        )
                        drawLine(
                            Color.Black.copy(alpha = 0.15f),
                            Offset(topBodyBottomLeft, ledgeTopY),
                            Offset(topBodyBottomRight, ledgeTopY),
                            strokeWidth = 3f,
                        )
                        drawLine(
                            Color.Black.copy(alpha = 0.3f),
                            Offset(trayLeft + seamRadius, trayTopY),
                            Offset(trayRight - seamRadius, trayTopY),
                            strokeWidth = 3f,
                        )
                        drawLine(
                            leftFlareBrush,
                            Offset(topBodyBottomLeft, ledgeTopY),
                            Offset(leftSlantX, lipTopY - lipCornerRadius),
                            strokeWidth = 4f,
                        )
                        drawLine(
                            rightFlareBrush,
                            Offset(topBodyBottomRight, ledgeTopY),
                            Offset(rightSlantX, lipTopY - lipCornerRadius),
                            strokeWidth = 4f,
                        )

                        // Components
                        drawPerspectiveStripe(w, topBodyBottomW, stripeStartY, ledgeTopY, lipTopY, trayTopY, colors)
                        drawLensAssembly(housingCenter, housingSize, shadowPaint25, specularPaint, colors)
                        drawBranding(w, topY, topBodyH, shadowPaint8, colors, brandIcon)
                        drawFlash(flashCenter, flashSize, shadowPaint15, colors)
                        drawShutterButton(shutterCenter, shutterRadius, isShutterPressed, shadowPaint12, colors)
                        drawExposureDial(dialCenter, dialRadius, shadowPaint10, dialHighlightPaint, colors)
                        drawBottomTrayDetails(textMeasurer, trayLeft, trayTopY, trayW, trayH, colors)
                    }
                }
            },
    )
}

/** A blurred fill [Paint] used for the camera's soft drop shadows and specular smears. */
private fun blurPaint(paintColor: Color, blurRadius: Float): Paint = Paint().apply {
    color = paintColor
    asFrameworkPaint().maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
}

/**
 * Draws the iconic Polaroid rainbow stripe across the body, ledge, and tray. Each band is drawn in
 * three segments (upper-body rect, a cubic Bezier patch across the angled ledge, tray rect) so it
 * follows the body's perspective warp.
 */
internal fun DrawScope.drawPerspectiveStripe(
    w: Float,
    topBodyW: Float,
    startY: Float,
    ledgeTopY: Float,
    lipTopY: Float,
    trayTopY: Float,
    colors: PolaroidColors,
) {
    val topStripeW = topBodyW * 0.075f
    val bottomStripeW = topStripeW * 1.4f
    val topX = (w - topStripeW) / 2f
    val bottomX = (w - bottomStripeW) / 2f
    val topSegmentW = topStripeW / colors.rainbow.size
    val bottomSegmentW = bottomStripeW / colors.rainbow.size
    val controlPointOffset = (lipTopY - ledgeTopY) * 0.4f

    colors.rainbow.forEachIndexed { index, color ->
        val segTopX = topX + (index * topSegmentW)
        val segBottomX = bottomX + (index * bottomSegmentW)

        drawRect(color, Offset(segTopX, startY), Size(topSegmentW, ledgeTopY - startY))

        val flarePath = Path().apply {
            moveTo(segTopX, ledgeTopY)
            lineTo(segTopX + topSegmentW, ledgeTopY)
            cubicTo(
                segTopX + topSegmentW, ledgeTopY + controlPointOffset,
                segBottomX + bottomSegmentW, lipTopY - controlPointOffset,
                segBottomX + bottomSegmentW, lipTopY,
            )
            lineTo(segBottomX, lipTopY)
            cubicTo(
                segBottomX, lipTopY - controlPointOffset,
                segTopX, ledgeTopY + controlPointOffset,
                segTopX, ledgeTopY,
            )
            close()
        }
        drawPath(flarePath, color)
        drawRect(color, Offset(segBottomX, lipTopY), Size(bottomSegmentW, trayTopY - lipTopY))
    }

    val slantedShadowPath = Path().apply {
        moveTo(topX, ledgeTopY)
        lineTo(topX + topStripeW, ledgeTopY)
        cubicTo(
            topX + topStripeW, ledgeTopY + controlPointOffset,
            bottomX + bottomStripeW, lipTopY - controlPointOffset,
            bottomX + bottomStripeW, lipTopY,
        )
        lineTo(bottomX, lipTopY)
        cubicTo(
            bottomX, lipTopY - controlPointOffset,
            topX, ledgeTopY + controlPointOffset,
            topX, ledgeTopY,
        )
        close()
    }
    drawPath(
        slantedShadowPath,
        Brush.verticalGradient(
            listOf(Color.Black.copy(alpha = 0f), Color.Black.copy(alpha = 0.35f)),
            startY = ledgeTopY, endY = lipTopY,
        ),
    )
    drawRect(
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.4f), Color.Transparent),
            startY = lipTopY, endY = trayTopY,
        ),
        Offset(bottomX, lipTopY), Size(bottomStripeW, trayTopY - lipTopY),
    )
}
