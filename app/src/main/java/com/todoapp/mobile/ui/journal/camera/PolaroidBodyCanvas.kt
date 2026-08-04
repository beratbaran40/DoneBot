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
import androidx.compose.ui.unit.toSize
import com.todoapp.uikit.theme.PolaroidColors
import com.todoapp.uikit.theme.TDTheme

/**
 * Renders the full skeuomorphic Polaroid OneStep camera body on a single Canvas via
 * [drawWithCache]: geometry + paints are built once per size change, the draw phase only
 * references cached objects and branches on [isShutterPressed]. Colors are read once from
 * [TDTheme] and captured by the cache lambda (which is not `@Composable`).
 *
 * All positions come from [PolaroidBodyMetrics] so the drawing, the shutter hit test, and the
 * screen layout that anchors the ejecting print to the film slot cannot drift apart.
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
                        val metrics = PolaroidBodyMetrics(size.toSize(), this)
                        val distance = (offset - metrics.shutterTouchCenterInBox).getDistance()
                        if (distance <= metrics.shutterTouchRadius) {
                            onShutterStateChange(true)
                            // Dragging off the button before lifting must cancel, not capture.
                            val released = tryAwaitRelease()
                            onShutterStateChange(false)
                            if (released) currentOnShutterClick()
                        }
                    },
                )
            }
            .drawWithCache {
                // ── CACHE PHASE: runs once per size change ──
                val m = PolaroidBodyMetrics(size, this)
                val w = m.w
                val h = m.h

                val trayW = w
                val trayLeft = 0f
                val trayRight = w

                val topBodyBottomLeft = (w - m.topBodyBottomW) / 2f
                val topBodyBottomRight = topBodyBottomLeft + m.topBodyBottomW
                val topBodyTopLeft = (w - m.topBodyTopW) / 2f
                val topBodyTopRight = topBodyTopLeft + m.topBodyTopW

                val topY = 0f
                val ledgeTopY = m.ledgeTopY
                val lipTopY = m.lipTopY
                val trayTopY = m.trayTopY
                val bottomY = m.bottomY

                val topRadius = 24.dp.toPx()
                val seamRadius = 3.dp.toPx()
                val bottomRadius = 24.dp.toPx()

                // Cached paths — the hood is the dark housing the film slot is cut under.
                val hoodTop = topY - m.hoodH + 4f
                val hoodPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = m.hoodLeft, top = hoodTop,
                            right = m.hoodLeft + m.hoodW, bottom = topY + 10f,
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
                val slantRatio = (lipTopY - ledgeTopY - lipCornerRadius) / (lipTopY - ledgeTopY)
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
                    addPath(hoodPath)
                    addPath(topBodyPath)
                    addPath(ledgeAndLipPath)
                    addPath(trayPath)
                }

                // Cached brushes
                val hoodGradient = Brush.verticalGradient(
                    listOf(colors.chassisLight, colors.nearBlack),
                    startY = hoodTop, endY = topY,
                )
                val hoodEdgeBrush = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.25f), Color.Transparent),
                    start = Offset(m.hoodLeft + m.hoodW, hoodTop),
                    end = Offset(m.hoodLeft, hoodTop + m.hoodH),
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

                // ── DRAW PHASE: runs per frame, uses cached objects ──
                onDrawBehind {
                    withTransform({ translate(left = m.originX, top = m.originY) }) {
                        // Body shadow
                        drawIntoCanvas { canvas ->
                            canvas.translate(-8f, 30f)
                            canvas.drawPath(silhouettePath, bodyShadowPaint)
                            canvas.translate(8f, -30f)
                        }

                        drawPath(hoodPath, hoodGradient)
                        drawPath(hoodPath, hoodEdgeBrush, style = Stroke(2f))

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
                        drawEjectSlot(m.slotMouth, colors)
                        drawPerspectiveStripe(w, m.topBodyBottomW, m.stripeStartY, ledgeTopY, lipTopY, trayTopY, colors)
                        drawLensAssembly(m.lensCenter, m.lensSize, shadowPaint25, specularPaint, colors)
                        drawBranding(m.brandingRect, shadowPaint8, colors, brandIcon)
                        drawFlash(m.flashCenter, m.flashSize, shadowPaint15, colors)
                        drawShutterButton(m.shutterCenter, m.shutterRadius, isShutterPressed, shadowPaint12, colors)
                        drawExposureDial(m.dialCenter, m.dialRadius, shadowPaint10, dialHighlightPaint, colors)
                        drawBottomTrayDetails(textMeasurer, trayLeft, trayTopY, trayW, m.trayH, colors)
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
