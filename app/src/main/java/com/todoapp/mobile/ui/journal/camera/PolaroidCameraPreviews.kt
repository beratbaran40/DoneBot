package com.todoapp.mobile.ui.journal.camera

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.previews.TDPreviewWide
import com.todoapp.uikit.theme.TDTheme

// ──────────────────────────── Full screen (PolaroidCameraBody) ────────────────────────────

@TDPreviewWide
@Composable
private fun PolaroidCameraIdlePreview() {
    TDTheme {
        PolaroidBodyPreviewHost(photoState = PhotoState.Idle, showPrint = false)
    }
}

@TDPreviewWide
@Composable
private fun PolaroidCameraEjectingPreview() {
    TDTheme {
        PolaroidBodyPreviewHost(
            photoState = PhotoState.Ejecting,
            showPrint = true,
            ejectProgress = 0.45f,
            devGreyAlpha = 1f,
            devTealAlpha = 1f,
            bitmap = rememberSampleBitmap(),
        )
    }
}

@TDPreviewWide
@Composable
private fun PolaroidCameraDevelopingPreview() {
    TDTheme {
        PolaroidBodyPreviewHost(
            photoState = PhotoState.Developing,
            showPrint = true,
            devGreyAlpha = 0.45f,
            devTealAlpha = 0.7f,
            bitmap = rememberSampleBitmap(),
        )
    }
}

@TDPreviewWide
@Composable
private fun PolaroidCameraDonePreview() {
    TDTheme {
        PolaroidBodyPreviewHost(
            photoState = PhotoState.Done,
            showPrint = true,
            bitmap = rememberSampleBitmap(),
        )
    }
}

// ──────────────────────────── Camera body canvas ────────────────────────────

@TDPreview
@Composable
private fun PolaroidCanvasRestingPreview() {
    TDTheme {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(PolaroidBodyMetrics.BODY_ASPECT)) {
            SkeuomorphicPolaroidCanvas(isShutterPressed = false, onShutterStateChange = {}, onShutterClick = {})
        }
    }
}

@TDPreview
@Composable
private fun PolaroidCanvasShutterPressedPreview() {
    TDTheme {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(PolaroidBodyMetrics.BODY_ASPECT)) {
            SkeuomorphicPolaroidCanvas(isShutterPressed = true, onShutterStateChange = {}, onShutterClick = {})
        }
    }
}

// ──────────────────────────── Shutter button ────────────────────────────

@TDPreview
@Composable
private fun PolaroidShutterButtonPreview() {
    TDTheme {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        ) {
            PolaroidShutterButton(enabled = true, onPressChange = {}, onClick = {})
            PolaroidShutterButton(enabled = false, onPressChange = {}, onClick = {})
        }
    }
}

// ──────────────────────────── Ejected print ────────────────────────────

@TDPreview
@Composable
private fun PolaroidPrintDevelopingPreview() {
    TDTheme {
        PrintPreviewHost(devGreyAlpha = 0.45f, devTealAlpha = 0.7f)
    }
}

@TDPreview
@Composable
private fun PolaroidPrintDevelopedPreview() {
    TDTheme {
        PrintPreviewHost(devGreyAlpha = 0f, devTealAlpha = 0f)
    }
}

// ──────────────────────────── Preview helpers ────────────────────────────

@Composable
private fun PolaroidBodyPreviewHost(
    photoState: PhotoState,
    showPrint: Boolean,
    ejectProgress: Float = 1f,
    devGreyAlpha: Float = 0f,
    devTealAlpha: Float = 0f,
    bitmap: Bitmap? = null,
) {
    val imageCapture = remember { ImageCapture.Builder().build() }
    PolaroidCameraBody(
        photoState = photoState,
        capturedBitmap = bitmap,
        isShutterPressed = false,
        showPrint = showPrint,
        flashAlpha = 0f,
        ejectProgress = ejectProgress,
        printTilt = -2f,
        devGreyAlpha = devGreyAlpha,
        devTealAlpha = devTealAlpha,
        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
        imageCapture = imageCapture,
        onShutterStateChange = {},
        onShutterClick = {},
        onFlip = {},
        onRetake = {},
        onSave = {},
        onBack = {},
    )
}

@Composable
private fun PrintPreviewHost(devGreyAlpha: Float, devTealAlpha: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        PolaroidPrint(
            bitmap = rememberSampleBitmap(),
            ejectProgress = 1f,
            tiltDegrees = -2f,
            devGreyAlpha = devGreyAlpha,
            devTealAlpha = devTealAlpha,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .aspectRatio(PRINT_ASPECT),
        )
    }
}

@Composable
private fun rememberSampleBitmap(): Bitmap = remember {
    Bitmap.createBitmap(PREVIEW_W, PREVIEW_H, Bitmap.Config.ARGB_8888).apply {
        eraseColor(android.graphics.Color.rgb(120, 150, 110))
    }
}

private const val PREVIEW_W = 240
private const val PREVIEW_H = 320
