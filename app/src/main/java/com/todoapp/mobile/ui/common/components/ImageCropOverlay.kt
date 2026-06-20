package com.todoapp.mobile.ui.common.components

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.todoapp.mobile.R
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDButtonSize
import com.todoapp.uikit.components.TDButtonType
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import com.example.uikit.R as UiKitR

private const val JPEG_QUALITY = 90

/**
 * Full-screen pan/zoom crop overlay shared by every task-photo entry point. Reuses the avatar
 * crop's pixel math ([computeCroppedBitmap]) but outputs JPEG bytes (not a file path) so it slots
 * straight into the existing `onPick(bytes, mime)` photo flow — no nav route or ViewModel needed.
 * Mask is a square (rounded-rect) by default; pass [circular] = true for an avatar-style circle.
 *
 * [source] is any Coil image model — a URL/URI [String] OR a [ByteArray] — so a just-cropped
 * in-memory image can be re-cropped without a file/URL round-trip.
 */
@Composable
fun ImageCropOverlay(
    source: Any,
    onCropped: (ByteArray) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    circular: Boolean = false,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        ImageCropContent(
            source = source,
            circular = circular,
            onCropped = onCropped,
            onDismiss = onDismiss,
            modifier = modifier,
        )
    }
}

// Composable-local one-shot crop with no ViewModel; Dispatchers.Default is fine here (no DI seam).
@Suppress("InjectDispatcher")
@Composable
private fun ImageCropContent(
    source: Any,
    circular: Boolean,
    onCropped: (ByteArray) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var boxSizePx by remember { mutableFloatStateOf(0f) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(source) {
        val request = ImageRequest.Builder(context)
            .data(source)
            .allowHardware(false) // software bitmap so we can read pixels for the crop
            .memoryCachePolicy(CachePolicy.DISABLED) // we solely own the decoded bitmap → safe to recycle
            .build()
        val result = context.imageLoader.execute(request)
        val bitmap = (result as? SuccessResult)?.drawable.let { it as? BitmapDrawable }?.bitmap
        if (bitmap == null) onDismiss() else sourceBitmap = bitmap
    }

    DisposableEffect(sourceBitmap) {
        val toRecycle = sourceBitmap
        onDispose { toRecycle?.recycle() }
    }

    BackHandler { onDismiss() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(56.dp))
            TDText(
                text = stringResource(R.string.image_crop_title),
                style = TDTheme.typography.heading3,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .aspectRatio(1f)
                        .clipToBounds()
                        .onSizeChanged { boxSizePx = it.width.toFloat() }
                        .pointerInput(sourceBitmap) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, CROP_MAX_SCALE)
                                offset += pan
                            }
                        },
                ) {
                    val bitmap = sourceBitmap
                    if (bitmap != null) {
                        val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .matchParentSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offset.x
                                    translationY = offset.y
                                },
                        )
                        CropMaskOverlay(circular = circular, modifier = Modifier.matchParentSize())
                    } else {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }

            TDButton(
                text = stringResource(R.string.image_crop_done),
                isEnable = sourceBitmap != null && !isSaving,
                type = TDButtonType.PRIMARY,
                size = TDButtonSize.MEDIUM,
                fullWidth = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                onClick = {
                    val bitmap = sourceBitmap
                    if (bitmap != null && boxSizePx > 0f && !isSaving) {
                        isSaving = true
                        val boxPx = boxSizePx
                        val s = scale
                        val ox = offset.x
                        val oy = offset.y
                        scope.launch {
                            val bytes = withContext(Dispatchers.Default) {
                                val cropped = computeCroppedBitmap(bitmap, boxPx, s, ox, oy)
                                ByteArrayOutputStream().use { out ->
                                    cropped.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                                    cropped.recycle()
                                    out.toByteArray()
                                }
                            }
                            onCropped(bytes)
                        }
                    }
                },
            )
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp),
        ) {
            Icon(
                painter = painterResource(UiKitR.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.cd_navigate_back),
                tint = Color.White,
            )
        }

        if (isSaving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

/** Dims outside the crop window and draws a thin white guide — circle or rounded square. */
@Composable
private fun CropMaskOverlay(
    circular: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
    ) {
        if (circular) {
            val radius = size.minDimension / 2f
            drawRect(Color.Black.copy(alpha = 0.45f))
            drawCircle(color = Color.Transparent, radius = radius, blendMode = BlendMode.Clear)
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = radius,
                style = Stroke(width = 2.dp.toPx()),
            )
        } else {
            // Square crop window == the whole box; just a white ring guide on the edge.
            drawRect(
                color = Color.White.copy(alpha = 0.9f),
                topLeft = Offset.Zero,
                size = Size(size.width, size.height),
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }
}

// The image cannot decode in a preview (no Coil/network), so this captures the overlay chrome in its
// loading state: black backdrop, title, back arrow and the disabled "Done" button.
@TDPreview
@Composable
private fun ImageCropOverlayLoadingPreview() {
    TDTheme {
        ImageCropOverlay(source = "", onCropped = {}, onDismiss = {})
    }
}
