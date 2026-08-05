package com.todoapp.mobile.ui.journal.camera

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.example.uikit.R
import com.todoapp.mobile.R.string
import com.todoapp.uikit.image.tdPainter

/**
 * Stateless layout: framed live viewfinder on top, skeuomorphic camera body below it, and the
 * shutter / action controls at the bottom.
 *
 * The print ejects **upwards** out of the film slot drawn at the top of the body. To make that read
 * as one physical motion, the body reports where its slot ended up (via [PolaroidBodyMetrics]) and
 * the print is placed in a layer that stops exactly at the slot mouth and clips to it — so the part
 * of the print that hasn't come out yet is genuinely hidden inside the camera.
 */
@Composable
internal fun PolaroidCameraBody(
    photoState: PhotoState,
    capturedBitmap: Bitmap?,
    isShutterPressed: Boolean,
    showPrint: Boolean,
    flashAlpha: Float,
    ejectProgress: Float,
    printTilt: Float,
    devGreyAlpha: Float,
    devTealAlpha: Float,
    cameraSelector: CameraSelector,
    imageCapture: ImageCapture,
    onShutterStateChange: (Boolean) -> Unit,
    onShutterClick: () -> Unit,
    onFlip: () -> Unit,
    onRetake: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    val previewView = rememberBoundPreview(imageCapture, cameraSelector)
    val density = LocalDensity.current

    // Both are captured in root coordinates; the difference is the slot's position inside this Box.
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    var slotMouthInRoot by remember { mutableStateOf(Rect.Zero) }
    val slotMouth = slotMouthInRoot.translate(-rootOrigin.x, -rootOrigin.y)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onGloballyPositioned { rootOrigin = it.positionInRoot() },
    ) {
        // Blurred backdrop sampled from the sharp 1:1 viewfinder above.
        BlurredCameraBackdrop(previewView, Modifier.fillMaxSize())

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(VIEWFINDER_WEIGHT)
                    .statusBarsPadding()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f),
                ) {
                    // Sharp 1:1 viewfinder — what you frame here is exactly the square that's captured.
                    PolaroidViewfinder(previewView = previewView, modifier = Modifier.fillMaxSize())

                    // Framing controls live inside the frame, on the preview they act on. Hidden once
                    // a print is out: it covers the viewfinder, and the print layer would paint over
                    // this anyway. Retake returns to Idle and brings the control back.
                    if (photoState == PhotoState.Idle) {
                        IconButton(
                            onClick = onFlip,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.35f), CircleShape),
                        ) {
                            Icon(
                                painter = tdPainter(com.todoapp.mobile.R.drawable.ic_flip_camera),
                                contentDescription = stringResource(string.polaroid_camera_flip_content_description),
                                tint = Color.White,
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(BODY_WEIGHT)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .aspectRatio(PolaroidBodyMetrics.BODY_ASPECT)
                        .onGloballyPositioned { coords ->
                            slotMouthInRoot = PolaroidBodyMetrics(coords.size.toSize(), density)
                                .slotMouthInBox
                                .translate(coords.positionInRoot())
                        },
                ) {
                    SkeuomorphicPolaroidCanvas(
                        isShutterPressed = isShutterPressed,
                        onShutterStateChange = onShutterStateChange,
                        onShutterClick = onShutterClick,
                    )
                }
            }

            PolaroidCameraControlBar(
                photoState = photoState,
                onShutterPressChange = onShutterStateChange,
                onShutterClick = onShutterClick,
                onRetake = onRetake,
                onSave = onSave,
            )
        }

        if (showPrint && slotMouth.bottom > 0f) {
            EjectedPrintLayer(
                slotMouth = slotMouth,
                bitmap = capturedBitmap,
                ejectProgress = ejectProgress,
                printTilt = printTilt,
                devGreyAlpha = devGreyAlpha,
                devTealAlpha = devTealAlpha,
            )
        }

        // Flash overlay: always composed, visibility driven by graphicsLayer alpha (no recomposition).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f)
                .graphicsLayer { alpha = flashAlpha }
                .background(Color.White),
        )

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp)
                .zIndex(11f),
        ) {
            Icon(
                painter = tdPainter(R.drawable.ic_arrow_back),
                contentDescription = stringResource(string.cd_navigate_back),
                tint = Color.White,
            )
        }
    }
}

/**
 * Hosts the ejecting print in a layer that ends at the film slot and clips to it. The print is as
 * wide as the slot, unless the room above the slot is too short for that — a shorter screen shrinks
 * the print rather than clipping its top off.
 */
@Composable
private fun EjectedPrintLayer(
    slotMouth: Rect,
    bitmap: Bitmap?,
    ejectProgress: Float,
    printTilt: Float,
    devGreyAlpha: Float,
    devTealAlpha: Float,
) {
    val density = LocalDensity.current
    val layerHeight = with(density) { slotMouth.bottom.toDp() }
    val slotWidth = with(density) { slotMouth.width.toDp() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(layerHeight)
            .clipToBounds(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val printWidth = minOf(slotWidth, maxHeight * PRINT_ASPECT * PRINT_HEADROOM)
        PolaroidPrint(
            bitmap = bitmap,
            ejectProgress = ejectProgress,
            tiltDegrees = printTilt,
            devGreyAlpha = devGreyAlpha,
            devTealAlpha = devTealAlpha,
            modifier = Modifier
                .width(printWidth)
                .aspectRatio(PRINT_ASPECT),
        )
    }
}

// The body is width-limited by its aspect ratio, so any extra height in its slot is dead space.
// These weights hand the surplus to the viewfinder on tall screens while still leaving the body
// enough room to stay full width on short ones.
private const val VIEWFINDER_WEIGHT = 1.1f
private const val BODY_WEIGHT = 1f
private const val PRINT_HEADROOM = 0.98f
