package com.todoapp.mobile.ui.onboarding

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.onboarding.OnboardingContract.UiAction
import com.todoapp.mobile.ui.onboarding.OnboardingContract.UiState
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDButtonSize
import com.todoapp.uikit.components.TDButtonType
import com.todoapp.uikit.components.TDSpannableText
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.image.tdPixelFilterQuality
import com.todoapp.uikit.theme.TDCornerStyle
import com.todoapp.uikit.theme.TDTheme
import kotlinx.coroutines.delay

private const val BACKGROUND_INTERVAL_MS = 1500L

/**
 * Full-bleed carousel backgrounds. `internal` rather than private so [OnboardingViewModel] can take
 * `.size` for its wrap-around instead of hardcoding the count and letting the two drift apart.
 */
internal val OnboardingBackgrounds =
    listOf(
        R.drawable.onboarding1,
        R.drawable.onboarding2,
        R.drawable.onboarding3,
        R.drawable.onboarding4,
    )

@Composable
fun OnboardingScreen(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val context = LocalContext.current
    // Read once and share with the preload below: a different decode size is a different Coil cache
    // key, so warming the cache with one value and rendering with another would waste the preload.
    val pixelArt = TDTheme.shapes.cornerStyle == TDCornerStyle.PIXEL

    // Warm Coil's memory cache off the main thread. Without it, moving from painterResource to
    // AsyncImage would trade the ANR for a blank frame the first time each background appears — on
    // the very first screen a new user ever sees.
    LaunchedEffect(context) {
        OnboardingBackgrounds.forEach { resId ->
            context.imageLoader.enqueue(onboardingBackgroundRequest(context, resId, pixelArt))
        }
    }

    // Carousel ticker. repeatOnLifecycle(RESUMED) stops it while the app is backgrounded and when the
    // screen leaves composition; the ViewModel's old `while (true)` loop did neither. onAction is read
    // through rememberUpdatedState so the effect is NOT keyed on it — NavGraph hands us a fresh
    // `viewModel::onAction` on every recomposition, which would otherwise restart the loop each tick.
    val currentOnAction by rememberUpdatedState(onAction)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(BACKGROUND_INTERVAL_MS)
                currentOnAction(UiAction.OnBackgroundTick)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Crossfade(
            modifier = Modifier.fillMaxSize(),
            targetState = uiState.bgIndex,
            animationSpec = tween(durationMillis = 600),
            label = "onboardingBackground",
        ) { idx ->
            AsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = onboardingBackgroundRequest(context, OnboardingBackgrounds[idx], pixelArt),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                filterQuality = tdPixelFilterQuality(),
                // Coil skips its async pipeline under LocalInspectionMode, so @TDPreview would render
                // this screen with no background at all. Feed the raw resource as the placeholder in
                // that case only — the IDE decodes it, the device never does.
                placeholder =
                if (LocalInspectionMode.current) {
                    tdPainter(id = OnboardingBackgrounds[idx])
                } else {
                    null
                },
            )
        }

        Box(
            modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops =
                        arrayOf(
                            0.0f to Color.Black.copy(alpha = 0.35f),
                            0.45f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.85f),
                        ),
                    ),
                ),
        )

        if (isPortrait) {
            OnboardingPortraitContent(onAction = onAction)
        } else {
            OnboardingLandscapeContent(onAction = onAction)
        }
    }
}

/**
 * Builds the request for one carousel background.
 *
 * These sit in `drawable-nodpi/` at up to 1024x1536, so each decodes to roughly 6 MB as ARGB_8888 —
 * and [painterResource] did that synchronously on the main thread every 1.5 s. That is the
 * `OnboardingPortraitContent` ANR Crashlytics reported ("ANR triggered by slow operations in main
 * thread"). Coil decodes on its own dispatcher and memory-caches the result, so repeat passes over
 * the carousel cost nothing.
 *
 * `allowRgb565` lets Coil halve bitmap memory on low-memory devices when the source has no alpha —
 * exactly the devices where the stall bites — and leaves quality untouched everywhere else. Coil's
 * own crossfade is switched off because the surrounding [Crossfade] already animates the swap; the
 * shared ImageLoader turns it on globally (Application.newImageLoader), so leaving it enabled here
 * would animate the same transition twice.
 */
/** Decoded width for the 8-bit kit's onboarding art — coarse enough to read as whole cells. */
private const val ONBOARDING_PIXEL_WIDTH_PX = 160

private fun onboardingBackgroundRequest(
    context: Context,
    @DrawableRes resId: Int,
    pixelArt: Boolean,
): ImageRequest = ImageRequest
    .Builder(context)
    .data(resId)
    .allowRgb565(true)
    .crossfade(false)
    // The 8-bit kit decodes the art at a deliberately coarse width; paired with the call site's
    // FilterQuality.None, Coil's own downsample-then-magnify turns the render into pixel art without
    // any extra bitmap plumbing — and decodes LESS than the default path, not more.
    .apply { if (pixelArt) size(ONBOARDING_PIXEL_WIDTH_PX) }
    .build()

@Composable
private fun OnboardingPortraitContent(onAction: (UiAction) -> Unit) {
    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            modifier =
            Modifier
                .size(96.dp)
                .statusBarsPadding(),
            painter = tdPainter(id = R.drawable.logo_text),
            contentDescription = null,
        )

        Spacer(modifier = Modifier.weight(1f))

        OnboardingTextBlock()

        Spacer(modifier = Modifier.height(24.dp))

        OnboardingActions(
            onAction = onAction,
            modifier = Modifier.navigationBarsPadding(),
        )
    }
}

@Composable
private fun OnboardingLandscapeContent(onAction: (UiAction) -> Unit) {
    Row(
        modifier =
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier =
            Modifier
                .fillMaxHeight()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                modifier = Modifier.size(200.dp),
                painter = tdPainter(id = R.drawable.logo_text),
                contentDescription = null,
            )
        }

        Column(
            modifier =
            Modifier
                .fillMaxHeight()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            OnboardingTextBlock(textAlign = TextAlign.Start)
            Spacer(modifier = Modifier.height(24.dp))
            OnboardingActions(onAction = onAction)
        }
    }
}

@Composable
private fun OnboardingTextBlock(textAlign: TextAlign = TextAlign.Start) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TDText(
            modifier = Modifier.fillMaxWidth(),
            textAlign = textAlign,
            color = TDTheme.colors.white,
            style = TDTheme.typography.heading1,
            text = stringResource(id = R.string.onboarding_title),
        )

        Spacer(modifier = Modifier.height(12.dp))

        TDText(
            modifier = Modifier.fillMaxWidth(),
            textAlign = textAlign,
            color = TDTheme.colors.white,
            style = TDTheme.typography.regularTextStyle,
            text = stringResource(id = R.string.onboarding_description),
        )
    }
}

@Composable
private fun OnboardingActions(
    onAction: (UiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TDButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.onboarding_get_started),
            isEnable = true,
            type = TDButtonType.PRIMARY,
            size = TDButtonSize.MEDIUM,
            icon = null,
            onClick = { onAction(UiAction.OnGetStartedClick) },
        )

        Spacer(modifier = Modifier.height(12.dp))

        TDSpannableText(
            modifier =
            Modifier
                .clickable { onAction(UiAction.OnLoginClick) }
                .padding(bottom = 8.dp),
            fullText = stringResource(id = R.string.onboarding_login_span),
            spanText = stringResource(id = R.string.onboarding_login_text_span),
            style = TDTheme.typography.regularTextStyle.copy(color = TDTheme.colors.white.copy(alpha = 0.85f)),
            spanStyle =
            SpanStyle(
                color = TDTheme.colors.white,
                fontWeight = FontWeight.Bold,
            ),
            textAlign = TextAlign.Center,
        )
    }
}

@com.todoapp.uikit.previews.TDPreviewWide
@Composable
fun OnboardingScreenLandScapePreview(
    @PreviewParameter(OnboardingScreenPreviewProvider::class) uiState: UiState,
) {
    TDTheme {
        OnboardingScreen(
            uiState = uiState,
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
fun OnboardingScreenPreview(
    @PreviewParameter(OnboardingScreenPreviewProvider::class) uiState: UiState,
) {
    TDTheme {
        OnboardingScreen(
            uiState = uiState,
            onAction = {},
        )
    }
}
