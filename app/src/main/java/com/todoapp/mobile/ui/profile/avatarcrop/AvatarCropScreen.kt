package com.todoapp.mobile.ui.profile.avatarcrop

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.uikit.R
import com.todoapp.mobile.R.string
import com.todoapp.mobile.ui.common.components.computeCroppedBitmap
import com.todoapp.mobile.ui.profile.avatarcrop.AvatarCropContract.UiAction
import com.todoapp.mobile.ui.profile.avatarcrop.AvatarCropContract.UiEffect
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDButtonSize
import com.todoapp.uikit.components.TDButtonType
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.extensions.collectWithLifecycle
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreviewWide
import com.todoapp.uikit.theme.TDTheme
import kotlinx.coroutines.flow.Flow

@Composable
fun AvatarCropScreen(
    source: String,
    uiEffect: Flow<UiEffect>,
    onAction: (UiAction) -> Unit,
    onCropped: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var isSaving by remember { mutableStateOf(false) }

    uiEffect.collectWithLifecycle { effect ->
        when (effect) {
            is UiEffect.NavigateBackWithCroppedPath -> onCropped(effect.path)
            is UiEffect.ShowError -> {
                isSaving = false
                Toast.makeText(context, context.getString(effect.messageRes), Toast.LENGTH_SHORT).show()
            }
        }
    }

    BackHandler { onBack() }

    AvatarCropContent(
        source = source,
        isSaving = isSaving,
        onConfirm = { bitmap ->
            isSaving = true
            onAction(UiAction.OnCropConfirmed(bitmap))
        },
        onBack = onBack,
    )
}

/**
 * Owns the source bitmap + pan/zoom gesture state, then renders the stateless [AvatarCropBody].
 * This is genuine view state, not app state, so it stays out of the ViewModel (which owns only
 * persistence + navigation).
 */
@Composable
private fun AvatarCropContent(
    source: String,
    isSaving: Boolean,
    onConfirm: (Bitmap) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var boxSizePx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(source) {
        val request = ImageRequest.Builder(context)
            .data(Uri.parse(source))
            .allowHardware(false) // software bitmap so we can read pixels for the crop
            .memoryCachePolicy(CachePolicy.DISABLED) // we solely own the decoded bitmap → safe to recycle
            .build()
        val result = context.imageLoader.execute(request)
        val bitmap = (result as? SuccessResult)?.drawable.let { it as? BitmapDrawable }?.bitmap
        if (bitmap == null) onBack() else sourceBitmap = bitmap
    }

    // Recycle the source bitmap when the screen leaves composition (captured in the effect block,
    // never read live inside onDispose).
    DisposableEffect(sourceBitmap) {
        val bitmapToRecycle = sourceBitmap
        onDispose { bitmapToRecycle?.recycle() }
    }

    AvatarCropBody(
        sourceBitmap = sourceBitmap,
        scale = scale,
        offset = offset,
        isSaving = isSaving,
        onSizeChanged = { boxSizePx = it },
        onTransform = { pan, zoom ->
            scale = (scale * zoom).coerceIn(1f, MAX_SCALE)
            offset += pan
        },
        onConfirm = {
            val bitmap = sourceBitmap
            if (bitmap != null && boxSizePx > 0f) {
                onConfirm(computeCroppedBitmap(bitmap, boxSizePx, scale, offset.x, offset.y))
            }
        },
        onBack = onBack,
    )
}

/** Stateless layout: full-bleed crop surface, circular preview mask, Done button, floating back. */
@Composable
internal fun AvatarCropBody(
    sourceBitmap: Bitmap?,
    scale: Float,
    offset: Offset,
    isSaving: Boolean,
    onSizeChanged: (Float) -> Unit,
    onTransform: (pan: Offset, zoom: Float) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
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
                text = stringResource(string.avatar_crop_screen_title),
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
                        .onSizeChanged { onSizeChanged(it.width.toFloat()) }
                        .pointerInput(sourceBitmap) {
                            detectTransformGestures { _, pan, zoom, _ -> onTransform(pan, zoom) }
                        },
                ) {
                    if (sourceBitmap != null) {
                        val imageBitmap = remember(sourceBitmap) { sourceBitmap.asImageBitmap() }
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
                        CircularMaskOverlay(modifier = Modifier.matchParentSize())
                    } else {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }

            TDButton(
                text = stringResource(string.avatar_crop_done),
                isEnable = sourceBitmap != null && !isSaving,
                type = TDButtonType.PRIMARY,
                size = TDButtonSize.MEDIUM,
                fullWidth = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                onClick = onConfirm,
            )
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp)
                .zIndex(2f),
        ) {
            Icon(
                painter = tdPainter(R.drawable.ic_arrow_back),
                contentDescription = stringResource(string.cd_navigate_back),
                tint = Color.White,
            )
        }

        if (isSaving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .zIndex(3f),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

/** Dims everything outside the circular avatar area and draws a thin white ring guide. */
@Composable
private fun CircularMaskOverlay(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
    ) {
        val radius = size.minDimension / 2f
        drawRect(Color.Black.copy(alpha = 0.45f))
        drawCircle(color = Color.Transparent, radius = radius, blendMode = BlendMode.Clear)
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = radius,
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}

private const val MAX_SCALE = 5f

@TDPreviewWide
@Composable
private fun AvatarCropIdlePreview() {
    TDTheme {
        AvatarCropBody(
            sourceBitmap = sampleBitmap(),
            scale = 1f,
            offset = Offset.Zero,
            isSaving = false,
            onSizeChanged = {},
            onTransform = { _, _ -> },
            onConfirm = {},
            onBack = {},
        )
    }
}

@TDPreviewWide
@Composable
private fun AvatarCropSavingPreview() {
    TDTheme {
        AvatarCropBody(
            sourceBitmap = sampleBitmap(),
            scale = 1.4f,
            offset = Offset.Zero,
            isSaving = true,
            onSizeChanged = {},
            onTransform = { _, _ -> },
            onConfirm = {},
            onBack = {},
        )
    }
}

private fun sampleBitmap(): Bitmap = Bitmap.createBitmap(600, 800, Bitmap.Config.ARGB_8888).apply {
    eraseColor(android.graphics.Color.rgb(69, 102, 236))
}
