package com.todoapp.mobile.ui.journal.camera

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.todoapp.uikit.theme.TDTheme

/**
 * The ejected Polaroid print with its two-layer developing chemistry. Both overlays use
 * [graphicsLayer] alpha so the develop animation is a RenderNode property change, not a
 * recomposition. The captured [bitmap] is converted to an `ImageBitmap` once per capture.
 */
@Composable
internal fun PolaroidPrint(
    bitmap: Bitmap?,
    offsetY: Float,
    rotationZ: Float,
    devGreyAlpha: Float,
    devTealAlpha: Float,
    modifier: Modifier = Modifier,
) {
    val polaroid = TDTheme.colors.polaroid
    val imageBitmap = remember(bitmap) { bitmap?.asImageBitmap() }

    Box(
        modifier = modifier
            .graphicsLayer {
                translationY = offsetY
                this.rotationZ = rotationZ
            }
            .fillMaxWidth(0.75f)
            .aspectRatio(0.82f)
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
