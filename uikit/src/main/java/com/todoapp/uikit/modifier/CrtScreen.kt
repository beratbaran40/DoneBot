package com.todoapp.uikit.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.todoapp.uikit.theme.PaletteKit
import com.todoapp.uikit.theme.TDTheme
import kotlin.math.roundToInt

/**
 * Below this the bands stop reading as a screen and start eating the picture: at the 3dp pitch a
 * 40dp circle holds only ~13 of them, which is enough to shred a mascot's face but not enough to
 * suggest a raster. Same idea as `PixelCornerShape`'s stair clamp — the effect has to earn its
 * pixels or step aside.
 */
private val ScanlineMinDiameter = 64.dp

private val ScanlinePitch = 3.dp
private val ScanlineBand = 1.dp
private const val SCANLINE_ALPHA = 0.43f
private const val VIGNETTE_ALPHA = 0.47f
private const val VIGNETTE_SMALL_ALPHA = 0.27f
private const val VIGNETTE_INNER_STOP = 0.55f

/**
 * Renders a circular surface as a small CRT: the artwork inside it is a picture on a screen rather
 * than a sticker on a background. Ground, then the content, then scan bands, a corner vignette and
 * the bezel.
 *
 * Only the Terminal kit has any use for this, so every other kit gets the receiver back untouched.
 * Chain it AFTER the circular clip — each layer is drawn as a circle, so the effect stays inside the
 * same silhouette the caller already established.
 *
 * [diameter] is the drawn size, used only to decide whether there is room for scan bands. Pass
 * `bezel = false` where the call site already draws its own ring — in this kit those border tokens
 * are green anyway, so they read as the bezel and a second ring at the same radius would just look
 * like a mistake.
 */
@Composable
fun Modifier.crtScreen(diameter: Dp, bezel: Boolean = true, bezelWidth: Dp = 2.dp): Modifier {
    if (TDTheme.palette != PaletteKit.TERMINAL) {
        return this
    }
    val colors = TDTheme.colors
    // The card token, so the screen sits on the same surface every other card does. The bands are
    // the ground showing through in the dark, and a ruled line on paper in the light.
    val ground = colors.lightPending
    val band = if (TDTheme.isDark) colors.background else colors.gridLine
    val bezelColor = colors.lightGray
    val banded = diameter >= ScanlineMinDiameter
    val vignetteAlpha = if (banded) VIGNETTE_ALPHA else VIGNETTE_SMALL_ALPHA

    return this.drawWithCache {
        val radius = size.minDimension / 2f
        val middle = Offset(size.width / 2f, size.height / 2f)
        val bezelPx = bezelWidth.toPx()
        val bands =
            if (banded) {
                val cell = ScanlinePitch.toPx().roundToInt().coerceAtLeast(2)
                scanlineBrush(
                    baseColor = Color.Transparent,
                    lineColor = band.copy(alpha = SCANLINE_ALPHA),
                    cellPx = cell,
                    bandPx = ScanlineBand.toPx().roundToInt().coerceIn(1, cell - 1),
                )
            } else {
                null
            }
        val vignette =
            Brush.radialGradient(
                VIGNETTE_INNER_STOP to Color.Transparent,
                1f to Color.Black.copy(alpha = vignetteAlpha),
                center = middle,
                radius = radius,
            )

        onDrawWithContent {
            drawCircle(color = ground, radius = radius)
            drawContent()
            bands?.let { drawCircle(brush = it, radius = radius) }
            drawCircle(brush = vignette, radius = radius)
            if (bezel) {
                drawCircle(color = bezelColor, radius = radius - bezelPx / 2f, style = Stroke(bezelPx))
            }
        }
    }
}
