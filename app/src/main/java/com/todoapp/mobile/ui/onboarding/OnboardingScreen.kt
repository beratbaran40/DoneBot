package com.todoapp.mobile.ui.onboarding

import android.content.res.Configuration
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.onboarding.OnboardingContract.UiAction
import com.todoapp.mobile.ui.onboarding.OnboardingContract.UiState
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDButtonSize
import com.todoapp.uikit.components.TDButtonType
import com.todoapp.uikit.components.TDSpannableText
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.theme.TDTheme

private val OnboardingImages =
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
    val bgImages = rememberSaveable { OnboardingImages }

    Box(modifier = Modifier.fillMaxSize()) {
        Crossfade(
            modifier = Modifier.fillMaxSize(),
            targetState = uiState.bgIndex,
            animationSpec = tween(durationMillis = 600),
            label = "onboardingBackground",
        ) { idx ->
            Image(
                modifier = Modifier.fillMaxSize(),
                painter = painterResource(id = bgImages[idx]),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
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
            painter = painterResource(id = R.drawable.logo_text),
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
                painter = painterResource(id = R.drawable.logo_text),
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
