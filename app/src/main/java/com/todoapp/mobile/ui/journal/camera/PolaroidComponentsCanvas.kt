package com.todoapp.mobile.ui.journal.camera

import android.graphics.BlurMaskFilter
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.todoapp.uikit.theme.PolaroidColors
import kotlin.math.roundToInt

/**
 * Draws the lens housing, barrel ribs, glass element, and specular highlights. Colors come from
 * [colors] ([PolaroidColors]) because this runs inside `drawWithCache` / `onDrawBehind`, which is
 * not a `@Composable` and therefore cannot read `TDTheme.colors` directly.
 */
@Suppress("LongMethod")
internal fun DrawScope.drawLensAssembly(
    center: Offset,
    size: Float,
    shadowPaint: Paint,
    specularPaint: Paint,
    colors: PolaroidColors,
) {
    val corner = CornerRadius(size * 0.15f)
    val topLeft = Offset(center.x - size / 2f, center.y - size / 2f)
    val topRight = Offset(center.x + size / 2f, center.y - size / 2f)
    val bottomLeft = Offset(center.x - size / 2f, center.y + size / 2f)

    drawIntoCanvas { canvas ->
        canvas.drawRoundRect(
            topLeft.x - 6f, topLeft.y + 16f,
            topLeft.x + size - 6f, topLeft.y + size + 16f,
            corner.x, corner.x, shadowPaint,
        )
    }

    val rimSize = size + 8f
    drawRoundRect(
        colors.bodyCream,
        Offset(center.x - rimSize / 2f, center.y - rimSize / 2f),
        Size(rimSize, rimSize),
        CornerRadius(rimSize * 0.15f),
    )
    val innerRimSize = size + 2f
    drawRoundRect(
        colors.lensRimInner,
        Offset(center.x - innerRimSize / 2f, center.y - innerRimSize / 2f),
        Size(innerRimSize, innerRimSize),
        CornerRadius(innerRimSize * 0.15f),
    )

    drawRoundRect(
        Brush.linearGradient(listOf(colors.chassisLight, colors.nearBlack), start = topRight, end = bottomLeft),
        topLeft, Size(size, size), corner,
    )
    drawRoundRect(
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.15f), Color.Transparent), start = topRight, end = center),
        topLeft, Size(size, size), corner, style = Stroke(2f),
    )

    val numRibs = 35
    val funnelOuterSize = size * 0.85f
    val innerHoleSize = size * 0.48f
    val ribStep = (funnelOuterSize - innerHoleSize) / 2f / numRibs

    val funnelTopLeft = Offset(center.x - funnelOuterSize / 2f, center.y - funnelOuterSize / 2f)
    drawRoundRect(
        Brush.linearGradient(listOf(Color.Black, colors.chassisLight), start = topRight, end = bottomLeft),
        funnelTopLeft,
        Size(funnelOuterSize, funnelOuterSize),
        CornerRadius(corner.x * 0.85f),
        style = Stroke(3f),
    )

    for (i in 0..numRibs) {
        val currentSize = funnelOuterSize - (i * ribStep * 2)
        val currentCorner = (corner.x * 0.85f) - (i * (corner.x * 0.4f / numRibs))
        val currentTopLeft = Offset(center.x - currentSize / 2f, center.y - currentSize / 2f)
        val currentTopRight = Offset(center.x + currentSize / 2f, center.y - currentSize / 2f)
        val currentBottomLeft = Offset(center.x - currentSize / 2f, center.y + currentSize / 2f)

        drawRoundRect(
            colors.panelSeam,
            currentTopLeft,
            Size(currentSize, currentSize),
            CornerRadius(currentCorner),
            style = Stroke(1.5f),
        )
        drawRoundRect(
            Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0.12f), Color.Transparent, Color.Transparent),
                start = currentTopRight, end = currentBottomLeft,
            ),
            currentTopLeft,
            Size(currentSize, currentSize),
            CornerRadius(currentCorner),
            style = Stroke(1f),
        )
    }

    val craterTopLeft = Offset(center.x - innerHoleSize / 2f, center.y - innerHoleSize / 2f)
    drawRoundRect(
        Brush.linearGradient(
            listOf(Color.Black, colors.panelRecess),
            start = Offset(center.x + innerHoleSize / 2, center.y - innerHoleSize / 2),
            end = Offset(center.x - innerHoleSize / 2, center.y + innerHoleSize / 2),
        ),
        craterTopLeft, Size(innerHoleSize, innerHoleSize), CornerRadius(14.dp.toPx()),
    )

    val glassSize = innerHoleSize * 0.85f
    val glassTopLeft = Offset(center.x - glassSize / 2f, center.y - glassSize / 2f)
    val glassPath = Path().apply {
        addRoundRect(
            RoundRect(
                glassTopLeft.x, glassTopLeft.y,
                glassTopLeft.x + glassSize, glassTopLeft.y + glassSize,
                CornerRadius(10.dp.toPx()),
            ),
        )
    }

    drawPath(
        glassPath,
        Brush.radialGradient(listOf(colors.glassLight, colors.nearBlack), center = center, radius = glassSize * 0.8f),
    )

    clipPath(glassPath) {
        // Broad diffuse highlight (convex glass illusion)
        withTransform({ rotate(-30f, center) }) {
            drawOval(
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)),
                Offset(center.x - glassSize, center.y - glassSize * 0.6f),
                Size(glassSize * 2f, glassSize * 0.8f),
            )
        }

        // Sharp window reflection via PathOperation.Difference
        withTransform({ rotate(-35f, center) }) {
            drawIntoCanvas { canvas ->
                val mainPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            Rect(
                                center.x - glassSize * 0.15f, center.y - glassSize * 0.28f,
                                center.x + glassSize * 0.15f, center.y - glassSize * 0.12f,
                            ),
                            CornerRadius(glassSize * 0.1f),
                        ),
                    )
                }
                val mainSubPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            Rect(
                                center.x - glassSize * 0.12f, center.y - glassSize * 0.24f,
                                center.x + glassSize * 0.18f, center.y - glassSize * 0.08f,
                            ),
                            CornerRadius(glassSize * 0.1f),
                        ),
                    )
                }
                canvas.drawPath(
                    Path().apply { op(mainPath, mainSubPath, PathOperation.Difference) },
                    specularPaint,
                )

                val dotPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            Rect(
                                center.x + glassSize * 0.18f, center.y - glassSize * 0.15f,
                                center.x + glassSize * 0.28f, center.y - glassSize * 0.08f,
                            ),
                            CornerRadius(glassSize * 0.05f),
                        ),
                    )
                }
                val dotSubPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            Rect(
                                center.x + glassSize * 0.21f, center.y - glassSize * 0.12f,
                                center.x + glassSize * 0.31f, center.y - glassSize * 0.05f,
                            ),
                            CornerRadius(glassSize * 0.05f),
                        ),
                    )
                }
                canvas.drawPath(
                    Path().apply { op(dotPath, dotSubPath, PathOperation.Difference) },
                    specularPaint,
                )
            }
        }
    }
}

/** Draws the flash unit: outer housing, recessed window, xenon bulb, and glass reflection. */
internal fun DrawScope.drawFlash(center: Offset, size: Float, shadowPaint: Paint, colors: PolaroidColors) {
    val corner = CornerRadius(size * 0.18f)
    val topLeft = Offset(center.x - size / 2f, center.y - size / 2f)
    val topRight = Offset(center.x + size / 2f, center.y - size / 2f)
    val bottomLeft = Offset(center.x - size / 2f, center.y + size / 2f)

    drawIntoCanvas { canvas ->
        canvas.drawRoundRect(
            topLeft.x - 4f, topLeft.y + 10f,
            topLeft.x + size - 4f, topLeft.y + size + 10f,
            corner.x, corner.x, shadowPaint,
        )
    }

    drawRoundRect(
        Brush.linearGradient(listOf(colors.housingLight, colors.housingDark), start = topRight, end = bottomLeft),
        topLeft, Size(size, size), corner,
    )
    drawRoundRect(
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.2f), Color.Transparent), start = topRight, end = center),
        topLeft, Size(size, size), corner, style = Stroke(1.5f),
    )

    val innerSize = size * 0.65f
    val innerTopLeft = Offset(center.x - innerSize / 2f, center.y - innerSize / 2f)
    drawRoundRect(
        Brush.linearGradient(
            listOf(colors.nearBlack, colors.housingRecess),
            start = Offset(center.x + innerSize / 2, center.y - innerSize / 2),
            end = Offset(center.x - innerSize / 2, center.y + innerSize / 2),
        ),
        innerTopLeft, Size(innerSize, innerSize), CornerRadius(innerSize * 0.15f),
    )

    val glassSize = innerSize * 0.90f
    val glassTopLeft = Offset(center.x - glassSize / 2f, center.y - glassSize / 2f)
    val glassPath = Path().apply {
        addRoundRect(
            RoundRect(
                glassTopLeft.x, glassTopLeft.y,
                glassTopLeft.x + glassSize, glassTopLeft.y + glassSize,
                CornerRadius(glassSize * 0.1f),
            ),
        )
    }

    drawPath(
        glassPath,
        Brush.radialGradient(listOf(colors.panelSeam, Color.Black), center = center, radius = glassSize * 0.8f),
    )

    clipPath(glassPath) {
        val bulbW = glassSize * 0.45f
        val bulbH = glassSize * 0.35f
        val bulbTopLeft = Offset(center.x - bulbW / 2f, center.y - bulbH / 2f)
        drawRoundRect(colors.flashBulb, bulbTopLeft, Size(bulbW, bulbH), CornerRadius(bulbW * 0.2f))
        drawRoundRect(
            Brush.linearGradient(
                listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent),
                start = bulbTopLeft, end = Offset(bulbTopLeft.x, bulbTopLeft.y + bulbH),
            ),
            bulbTopLeft, Size(bulbW, bulbH), CornerRadius(bulbW * 0.2f),
        )

        val reflectionPath = Path().apply {
            moveTo(glassTopLeft.x, glassTopLeft.y)
            lineTo(glassTopLeft.x + glassSize, glassTopLeft.y)
            lineTo(glassTopLeft.x + glassSize, glassTopLeft.y + glassSize * 0.5f)
            quadraticTo(center.x, center.y - glassSize * 0.1f, glassTopLeft.x, glassTopLeft.y + glassSize * 0.6f)
            close()
        }
        drawPath(
            reflectionPath,
            Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.05f)),
                start = Offset(center.x + glassSize / 2, center.y - glassSize / 2),
                end = Offset(center.x - glassSize / 2, center.y + glassSize / 2),
            ),
        )
    }
}

/** Draws the red shutter button with cream collar, shadow, and a pressed-state specular highlight. */
internal fun DrawScope.drawShutterButton(
    center: Offset,
    radius: Float,
    isPressed: Boolean,
    shadowPaint: Paint,
    colors: PolaroidColors,
) {
    val outerCollarRadius = radius * 1.35f

    drawIntoCanvas { canvas ->
        canvas.drawOval(
            center.x - outerCollarRadius - 4f, center.y - outerCollarRadius + 8f,
            center.x + outerCollarRadius - 4f, center.y + outerCollarRadius + 8f,
            shadowPaint,
        )
    }

    drawCircle(
        Brush.linearGradient(listOf(colors.shutterCollarLight, colors.shutterCollarDark)),
        outerCollarRadius, center,
    )

    val pressOffset = if (isPressed) Offset(-1f, 3f) else Offset.Zero
    val buttonCenter = center + pressOffset

    drawCircle(Color.Black.copy(alpha = 0.4f), radius * 1.05f, Offset(center.x, center.y + 4f))
    drawCircle(
        Brush.radialGradient(
            listOf(colors.shutterRedHighlight, colors.shutterRed),
            center = buttonCenter, radius = radius * 1.5f,
        ),
        radius, buttonCenter,
    )
    drawCircle(
        Brush.linearGradient(
            listOf(Color.Transparent, Color.Black.copy(alpha = 0.25f)),
            start = Offset(buttonCenter.x - radius, buttonCenter.y - radius),
            end = Offset(buttonCenter.x, buttonCenter.y),
        ),
        radius, buttonCenter,
    )
    drawCircle(
        Brush.sweepGradient(
            0.0f to Color.Black.copy(alpha = 0.1f),
            0.15f to Color.White.copy(alpha = 0.1f),
            0.3f to Color.Black.copy(alpha = 0.1f),
            0.45f to Color.White.copy(alpha = 0.1f),
            0.6f to Color.Black.copy(alpha = 0.1f),
            0.75f to Color.White.copy(alpha = 0.1f),
            1.0f to Color.Black.copy(alpha = 0.1f),
            center = buttonCenter,
        ),
        radius, buttonCenter,
    )
    drawCircle(Color.White.copy(alpha = 0.15f), radius * 0.98f, buttonCenter, style = Stroke(1f))

    withTransform({ rotate(-35f, buttonCenter) }) {
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                color = Color.White.copy(alpha = if (isPressed) 0.4f else 0.75f)
                asFrameworkPaint().maskFilter = BlurMaskFilter(3f, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.drawOval(
                buttonCenter.x - radius * 0.2f, buttonCenter.y - radius * 0.7f,
                buttonCenter.x + radius * 0.4f, buttonCenter.y - radius * 0.4f,
                paint,
            )
        }
    }
}

/** Draws the exposure compensation dial — three concentric circles with a directional highlight. */
internal fun DrawScope.drawExposureDial(
    center: Offset,
    radius: Float,
    shadowPaint: Paint,
    highlightPaint: Paint,
    colors: PolaroidColors,
) {
    drawIntoCanvas { canvas ->
        canvas.drawOval(
            center.x - radius - 4f, center.y - radius + 8f,
            center.x + radius - 4f, center.y + radius + 8f,
            shadowPaint,
        )
    }

    drawCircle(
        Brush.linearGradient(
            listOf(colors.housingLight, colors.nearBlack),
            start = Offset(center.x + radius, center.y - radius),
            end = Offset(center.x - radius, center.y + radius),
        ),
        radius, center,
    )
    drawCircle(colors.trayDeep, radius * 0.88f, center)
    val topRadius = radius * 0.82f
    drawCircle(
        Brush.linearGradient(
            listOf(colors.panelRecess, colors.panelSeam),
            start = Offset(center.x + topRadius, center.y - topRadius),
            end = Offset(center.x - topRadius, center.y + topRadius),
        ),
        topRadius, center,
    )

    withTransform({ rotate(45f, center) }) {
        drawIntoCanvas { canvas ->
            canvas.drawOval(
                center.x - topRadius * 0.6f, center.y - topRadius * 0.8f,
                center.x + topRadius * 0.6f, center.y - topRadius * 0.2f,
                highlightPaint,
            )
        }
    }
}

/** Draws the DoneBot logo badge where the camera originally had its "Supercolor 1000" sticker. */
internal fun DrawScope.drawBranding(
    bounds: Rect,
    shadowPaint: Paint,
    colors: PolaroidColors,
    brandIcon: ImageBitmap,
    brandFilterQuality: FilterQuality,
) {
    val stickerSize = bounds.width
    val stickerX = bounds.left
    val stickerY = bounds.top

    drawIntoCanvas { canvas ->
        canvas.drawRoundRect(
            stickerX - 2f, stickerY + 6f,
            stickerX + stickerSize - 2f, stickerY + stickerSize + 6f,
            6.dp.toPx(), 6.dp.toPx(), shadowPaint,
        )
    }

    drawRoundRect(
        colors.stickerBeige,
        Offset(stickerX, stickerY),
        Size(stickerSize, stickerSize),
        CornerRadius(6.dp.toPx()),
    )
    drawRoundRect(
        Color.White.copy(alpha = 0.4f),
        Offset(stickerX + 1f, stickerY + 1f),
        Size(stickerSize - 2f, stickerSize - 2f),
        CornerRadius(5.dp.toPx()),
        style = Stroke(1f),
    )

    val pad = stickerSize * 0.1f
    val iconSize = (stickerSize - pad * 2).roundToInt()
    drawImage(
        image = brandIcon,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(brandIcon.width, brandIcon.height),
        dstOffset = IntOffset((stickerX + pad).roundToInt(), (stickerY + pad).roundToInt()),
        dstSize = IntSize(iconSize, iconSize),
        filterQuality = brandFilterQuality,
    )
}

/**
 * Draws the bottom chassis: speaker grille panel and model text. The film ejection slot is *not*
 * here — it sits at the top of the body (see [drawEjectSlot]) so the print ejects upwards, and the
 * grille is centred in the tray to fill the space the slot used to take.
 */
internal fun DrawScope.drawBottomTrayDetails(
    textMeasurer: TextMeasurer,
    startX: Float,
    startY: Float,
    trayW: Float,
    trayH: Float,
    colors: PolaroidColors,
) {
    val panelWidth = trayW * 0.90f
    val panelHeight = trayH * 0.40f
    val panelX = startX + (trayW - panelWidth) / 2f
    val panelY = startY + (trayH * 0.30f)

    drawRoundRect(
        colors.trayDeep,
        Offset(panelX - 2f, panelY - 2f),
        Size(panelWidth + 4f, panelHeight + 4f),
        CornerRadius(6.dp.toPx()),
    )
    drawRoundRect(
        colors.panelRecess,
        Offset(panelX, panelY),
        Size(panelWidth, panelHeight),
        CornerRadius(6.dp.toPx()),
    )

    val numRibs = 6
    val ribSpacing = panelHeight / (numRibs + 1)
    for (i in 1..numRibs) {
        val yPos = panelY + (i * ribSpacing)
        drawLine(
            colors.panelSeam,
            Offset(panelX + 8f, yPos),
            Offset(panelX + panelWidth - 8f, yPos),
            strokeWidth = 3f,
        )
        drawLine(
            Color.White.copy(alpha = 0.03f),
            Offset(panelX + 8f, yPos + 2f),
            Offset(panelX + panelWidth - 8f, yPos + 2f),
            strokeWidth = 2f,
        )
    }

    drawText(
        textMeasurer,
        "POLAROID LAND CAMERA",
        Offset(panelX + (trayW * 0.04f), panelY + (panelHeight * 0.30f)),
        TextStyle(
            color = colors.textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 1.sp,
        ),
    )
}

/**
 * Draws the film ejection slot cut into the top edge of the cream body — the opening the developed
 * print rises out of. [mouth] is the visible opening; the surrounding bezel and lip are derived from
 * it, so the layout can anchor the print to exactly the same rect.
 *
 * The lip gradient runs the opposite way to a bottom-mounted slot: with light coming from the top
 * left, the edge that catches it on a top-facing opening is the near (lower) lip, not the far one.
 */
internal fun DrawScope.drawEjectSlot(mouth: Rect, colors: PolaroidColors) {
    val bezel = 4f
    drawRoundRect(
        colors.panelRecess,
        Offset(mouth.left - bezel, mouth.top - bezel),
        Size(mouth.width + bezel * 2f, mouth.height + bezel * 2f),
        CornerRadius(6.dp.toPx()),
    )
    drawRoundRect(
        colors.trayDeep,
        mouth.topLeft,
        mouth.size,
        CornerRadius(4.dp.toPx()),
    )

    val lipHeight = mouth.height * 0.5f
    val lipTop = mouth.bottom - lipHeight - 2f
    drawRoundRect(
        Brush.verticalGradient(
            0.0f to colors.trayDeep,
            0.4f to colors.panelRecess,
            0.6f to colors.slotLip,
            1.0f to colors.panelSeam,
            startY = lipTop,
            endY = lipTop + lipHeight,
        ),
        Offset(mouth.left + bezel, lipTop),
        Size(mouth.width - bezel * 2f, lipHeight),
        CornerRadius(3.dp.toPx()),
    )

    // Hairline highlight along the cream just below the opening, so the slot reads as cut in.
    drawLine(
        Color.White.copy(alpha = 0.65f),
        Offset(mouth.left - bezel, mouth.bottom + bezel + 1f),
        Offset(mouth.right + bezel, mouth.bottom + bezel + 1f),
        strokeWidth = 2f,
    )
}
