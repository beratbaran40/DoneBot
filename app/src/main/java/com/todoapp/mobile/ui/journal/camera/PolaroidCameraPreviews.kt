package com.todoapp.mobile.ui.journal.camera

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1.15f)) {
            SkeuomorphicPolaroidCanvas(isShutterPressed = false, onShutterStateChange = {}, onShutterClick = {})
        }
    }
}

@TDPreview
@Composable
private fun PolaroidCanvasShutterPressedPreview() {
    TDTheme {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1.15f)) {
            SkeuomorphicPolaroidCanvas(isShutterPressed = true, onShutterStateChange = {}, onShutterClick = {})
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
        ejectOffsetY = 0f,
        printRotation = -2f,
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
            offsetY = 0f,
            rotationZ = -2f,
            devGreyAlpha = devGreyAlpha,
            devTealAlpha = devTealAlpha,
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
