package com.todoapp.mobile.ui.journal.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.todoapp.uikit.theme.TDTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import timber.log.Timber

/**
 * Creates a single CameraX [PreviewView] and binds the [Preview] + [imageCapture] use cases,
 * rebinding whenever [cameraSelector] changes (the flip control). Returns `null` inside
 * `@Preview`/inspection mode. The same instance feeds both the sharp [CameraViewfinder] and the
 * [BlurredCameraBackdrop] (which snapshots its frames), so only one live Preview use case is bound.
 */
@Composable
internal fun rememberBoundPreview(
    imageCapture: ImageCapture,
    cameraSelector: CameraSelector,
): PreviewView? {
    if (LocalInspectionMode.current) return null

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    val preview = remember {
        Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
    }

    LaunchedEffect(cameraSelector) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            runCatching {
                val cameraProvider = future.get()
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
            }.onFailure { Timber.tag(TAG).e(it, "Failed to bind camera") }
        }, ContextCompat.getMainExecutor(context))
    }

    return previewView
}

/** Sharp 1:1 viewfinder — what is shown here is exactly the square that gets captured. */
@Composable
internal fun CameraViewfinder(previewView: PreviewView?, modifier: Modifier = Modifier) {
    if (previewView == null) {
        Box(
            modifier = modifier.background(TDTheme.colors.polaroid.previewBackdrop),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "Camera Preview", color = Color.Gray)
        }
        return
    }
    AndroidView(factory = { previewView }, modifier = modifier)
}

/**
 * Full-screen blurred backdrop. CameraX allows only one live Preview use case, so instead of a
 * second feed this samples [previewView]'s rendered frames a few times per second and draws the
 * latest one heavily blurred. The lag is invisible behind the blur, and the captured photo is
 * unaffected (this never touches the ImageCapture pipeline).
 */
@Composable
internal fun BlurredCameraBackdrop(previewView: PreviewView?, modifier: Modifier = Modifier) {
    if (previewView == null) {
        Box(modifier = modifier.background(Color.Black))
        return
    }

    var snapshot by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(previewView) {
        while (isActive) {
            delay(BACKDROP_REFRESH_MS)
            runCatching { previewView.bitmap }.getOrNull()?.let { snapshot = it.asImageBitmap() }
        }
    }

    val frame = snapshot
    if (frame != null) {
        Image(
            bitmap = frame,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.blur(BACKDROP_BLUR_RADIUS),
        )
    } else {
        Box(modifier = modifier.background(Color.Black))
    }
}

/**
 * Captures a frame from [imageCapture], corrects sensor rotation, and (only when [mirror], i.e. the
 * front camera) flips it so the saved image matches the on-screen preview. Recycles the intermediate
 * decoded bitmap; the rotated result is owned by the caller.
 */
internal fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    mirror: Boolean,
    onImageCaptured: (Bitmap) -> Unit,
    onError: () -> Unit,
) {
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.capacity())
                    buffer.get(bytes)
                    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)

                    val matrix = Matrix().apply {
                        postRotate(image.imageInfo.rotationDegrees.toFloat())
                        if (mirror) postScale(-1f, 1f)
                    }
                    val rotated = Bitmap.createBitmap(
                        decoded, 0, 0, decoded.width, decoded.height, matrix, true,
                    )
                    if (rotated !== decoded) decoded.recycle()
                    onImageCaptured(rotated)
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Polaroid capture failed")
                    onError()
                } finally {
                    image.close()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Timber.tag(TAG).e(exception, "Polaroid capture error")
                onError()
            }
        },
    )
}

private const val BACKDROP_REFRESH_MS = 250L
private val BACKDROP_BLUR_RADIUS = 32.dp
private const val TAG = "PolaroidCamera"
