package com.todoapp.mobile.ui.journal.camera

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.todoapp.uikit.theme.TDTheme

/**
 * The ejected Polaroid print with its two-layer developing chemistry. Both overlays use
 * [graphicsLayer] alpha so the develop animation is a RenderNode property change, not a
 * recomposition. The captured [bitmap] is converted to an `ImageBitmap` once per capture.
 *
 * [ejectProgress] runs 0..1: at 0 the print sits entirely below its container's bottom edge (which
 * the caller aligns with the camera's film slot, so it reads as still inside the camera), at 1 its
 * bottom edge rests in the slot mouth. Translation is derived from the measured height rather than
 * a pixel constant, so it stays correct at any print size. The tilt scales with the same progress
 * and pivots on the bottom edge, so the print leaves the slot straight and settles askew.
 *
 * The caller sizes this via [modifier] — the print is as wide as the slot it comes out of.
 */
@Composable
internal fun PolaroidPrint(
    bitmap: Bitmap?,
    ejectProgress: Float,
    tiltDegrees: Float,
    devGreyAlpha: Float,
    devTealAlpha: Float,
    modifier: Modifier = Modifier,
) {
    val polaroid = TDTheme.colors.polaroid
    val imageBitmap = remember(bitmap) { bitmap?.asImageBitmap() }

    Box(
        modifier = modifier
            .graphicsLayer {
                transformOrigin = TransformOrigin(0.5f, 1f)
                translationY = (1f - ejectProgress) * size.height
                rotationZ = tiltDegrees * ejectProgress
            }
            .shadow(16.dp, RoundedCornerShape(4.dp))
            .background(polaroid.printPaper, RoundedCornerShape(4.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 48.dp),
        ) {
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(polaroid.panelSeam),
                )
            }
            if (devTealAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = devTealAlpha }
                        .background(polaroid.developTeal),
                )
            }
            if (devGreyAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = devGreyAlpha }
                        .background(polaroid.developGrey),
                )
            }
        }
    }
}

/** Width / height of a Polaroid print, border included. */
internal const val PRINT_ASPECT = 0.82f
