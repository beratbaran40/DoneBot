package com.todoapp.mobile.ui.journal.camera

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.uikit.R
import com.todoapp.mobile.R.string
import com.todoapp.mobile.ui.common.LockScreenOrientation
import com.todoapp.mobile.ui.journal.camera.PolaroidCameraContract.UiAction
import com.todoapp.mobile.ui.journal.camera.PolaroidCameraContract.UiEffect
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDButtonSize
import com.todoapp.uikit.components.TDButtonType
import com.todoapp.uikit.extensions.collectWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Composable
fun PolaroidCameraScreen(
    uiEffect: Flow<UiEffect>,
    onAction: (UiAction) -> Unit,
    onPhotoSaved: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    // The Polaroid camera is a portrait-only skeuomorphic experience (vertical print eject, 1:1
    // viewfinder). Lock it so rotating the device can't squash the body or blow the print up.
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

    uiEffect.collectWithLifecycle { effect ->
        when (effect) {
            is UiEffect.NavigateBackWithPhoto -> onPhotoSaved(effect.path)
            is UiEffect.ShowError ->
                Toast.makeText(context, context.getString(effect.messageRes), Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler { onBack() }

    PolaroidCameraContent(
        onSavePhoto = { bitmap -> onAction(UiAction.OnSavePhoto(bitmap)) },
        onBack = onBack,
    )
}

/**
 * Owns the capture + animation state and the camera plumbing, then renders the stateless
 * [PolaroidCameraBody]. All of this is genuine view/animation state, not app state, so it
 * intentionally stays out of the ViewModel (which owns only persistence + navigation).
 */
@Composable
private fun PolaroidCameraContent(
    onSavePhoto: (Bitmap) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var photoState by remember { mutableStateOf(PhotoState.Idle) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isShutterPressed by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }

    val flashAlpha = remember { Animatable(0f) }
    val ejectOffsetY = remember { Animatable(-800f) }
    val printRotation = remember { Animatable(0f) }
    val devGreyAlpha = remember { Animatable(1f) }
    val devTealAlpha = remember { Animatable(1f) }

    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraSelector = remember(lensFacing) {
        CameraSelector.Builder().requireLensFacing(lensFacing).build()
    }

    val showPrint by remember {
        derivedStateOf { photoState != PhotoState.Idle && photoState != PhotoState.Capturing }
    }
    val currentPhotoState by rememberUpdatedState(photoState)
    val currentLensFacing by rememberUpdatedState(lensFacing)

    // Recycle the previous bitmap when it's replaced (retake) or the screen leaves composition.
    // The value is captured in the effect block on purpose: reading the live `capturedBitmap` inside
    // onDispose would recycle the NEW bitmap (the one we're about to draw) when the key changes,
    // crashing with "Canvas: trying to use a recycled bitmap".
    DisposableEffect(capturedBitmap) {
        val bitmapToRecycle = capturedBitmap
        onDispose { bitmapToRecycle?.recycle() }
    }

    val handleShutterClick: () -> Unit = remember {
        {
            if (currentPhotoState == PhotoState.Idle || currentPhotoState == PhotoState.Done) {
                coroutineScope.launch {
                    if (currentPhotoState == PhotoState.Done) {
                        launch { ejectOffsetY.animateTo(2500f, tween(EJECT_AWAY_MS, easing = FastOutSlowInEasing)) }
                    }

                    photoState = PhotoState.Capturing
                    flashAlpha.snapTo(1f)
                    launch { flashAlpha.animateTo(0f, tween(FLASH_MS)) }

                    takePhoto(
                        context = context,
                        imageCapture = imageCapture,
                        mirror = currentLensFacing == CameraSelector.LENS_FACING_FRONT,
                        onImageCaptured = { bitmap ->
                            capturedBitmap = bitmap
                            photoState = PhotoState.Ejecting
                            coroutineScope.launch {
                                ejectOffsetY.snapTo(-800f)
                                devGreyAlpha.snapTo(1f)
                                devTealAlpha.snapTo(1f)

                                val randomTilt = (-MAX_TILT_DEG..MAX_TILT_DEG).random().toFloat()
                                launch { printRotation.animateTo(randomTilt, tween(EJECT_MS)) }
                                ejectOffsetY.animateTo(40f, tween(EJECT_MS, easing = LinearOutSlowInEasing))

                                photoState = PhotoState.Developing
                                devGreyAlpha.animateTo(0f, tween(DEV_GREY_MS, easing = LinearEasing))
                                devTealAlpha.animateTo(0f, tween(DEV_TEAL_MS, easing = FastOutSlowInEasing))

                                photoState = PhotoState.Done
                            }
                        },
                        onError = { photoState = PhotoState.Idle },
                    )
                }
            }
        }
    }

    val handleRetake: () -> Unit = remember {
        {
            if (currentPhotoState == PhotoState.Done) {
                photoState = PhotoState.Idle
                coroutineScope.launch {
                    ejectOffsetY.animateTo(2500f, tween(RETAKE_MS, easing = FastOutSlowInEasing))
                    capturedBitmap = null
                }
            }
        }
    }

    PolaroidCameraBody(
        photoState = photoState,
        capturedBitmap = capturedBitmap,
        isShutterPressed = isShutterPressed,
        showPrint = showPrint,
        flashAlpha = flashAlpha.value,
        ejectOffsetY = ejectOffsetY.value,
        printRotation = printRotation.value,
        devGreyAlpha = devGreyAlpha.value,
        devTealAlpha = devTealAlpha.value,
        cameraSelector = cameraSelector,
        imageCapture = imageCapture,
        onShutterStateChange = { isShutterPressed = it },
        onShutterClick = handleShutterClick,
        onFlip = {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
        },
        onRetake = handleRetake,
        onSave = { capturedBitmap?.let(onSavePhoto) },
        onBack = onBack,
    )
}

/** Stateless layout: skeuomorphic body, live preview, ejected print, flash overlay, and chrome. */
@Composable
internal fun PolaroidCameraBody(
    photoState: PhotoState,
    capturedBitmap: Bitmap?,
    isShutterPressed: Boolean,
    showPrint: Boolean,
    flashAlpha: Float,
    ejectOffsetY: Float,
    printRotation: Float,
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // Blurred backdrop sampled from the sharp 1:1 viewfinder below.
        BlurredCameraBackdrop(previewView, Modifier.fillMaxSize())

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.3f)
                    .statusBarsPadding()
                    .padding(top = 16.dp, start = 4.dp, end = 4.dp, bottom = 8.dp)
                    .zIndex(2f),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.aspectRatio(1.15f)) {
                    SkeuomorphicPolaroidCanvas(
                        isShutterPressed = isShutterPressed,
                        onShutterStateChange = onShutterStateChange,
                        onShutterClick = onShutterClick,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds()
                    .zIndex(1f),
            ) {
                // Sharp 1:1 viewfinder — what you frame here is exactly the square that's captured.
                CameraViewfinder(
                    previewView = previewView,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp)),
                )

                if (photoState == PhotoState.Idle || photoState == PhotoState.Done) {
                    IconButton(
                        onClick = onFlip,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .navigationBarsPadding()
                            .padding(12.dp),
                    ) {
                        Icon(
                            painter = painterResource(com.todoapp.mobile.R.drawable.ic_flip_camera),
                            contentDescription = stringResource(string.polaroid_camera_flip_content_description),
                            tint = Color.White,
                        )
                    }
                }

                if (showPrint) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        PolaroidPrint(
                            bitmap = capturedBitmap,
                            offsetY = ejectOffsetY,
                            rotationZ = printRotation,
                            devGreyAlpha = devGreyAlpha,
                            devTealAlpha = devTealAlpha,
                        )
                    }
                }

                if (photoState == PhotoState.Done) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TDButton(
                            modifier = Modifier.weight(1f),
                            text = stringResource(string.polaroid_retake),
                            type = TDButtonType.CANCEL,
                            size = TDButtonSize.SMALL,
                            fullWidth = true,
                            onClick = onRetake,
                        )
                        TDButton(
                            modifier = Modifier.weight(1f),
                            text = stringResource(string.polaroid_save_to_journal),
                            type = TDButtonType.PRIMARY,
                            size = TDButtonSize.SMALL,
                            fullWidth = true,
                            onClick = onSave,
                        )
                    }
                }
            }
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
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(string.cd_navigate_back),
                tint = Color.White,
            )
        }
    }
}

private const val FLASH_MS = 350
private const val EJECT_MS = 1200
private const val EJECT_AWAY_MS = 300
private const val RETAKE_MS = 500
private const val DEV_GREY_MS = 1500
private const val DEV_TEAL_MS = 3500
private const val MAX_TILT_DEG = 3
