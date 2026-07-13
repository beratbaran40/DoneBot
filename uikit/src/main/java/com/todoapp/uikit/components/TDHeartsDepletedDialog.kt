package com.todoapp.uikit.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.uikit.R.drawable
import com.todoapp.uikit.previews.TDPreviewDialog
import com.todoapp.uikit.theme.TDTheme
import kotlinx.coroutines.delay

private const val BREATHING_MS = 3000
private const val BREATHING_MAX_SCALE = 1.03f
private const val BUBBLE_FADE_MS = 300
private const val BUBBLE_DELAY_MS = 50L

/**
 * Character-driven "you ran out of hearts" dialog for the Activity health bar. Forks the visual
 * language of [TDGoodbyeDialog] — the sad DoneBot mascot in a breathing halo plus a fade-in speech
 * bubble — but with a single acknowledgement CTA and no type-to-confirm. A row of empty hearts drives
 * the "streak ended" message home. Animations skip in `LocalInspectionMode` and when [reduceMotion].
 */
@Composable
fun TDHeartsDepletedDialog(
    speechBubbleText: String,
    buttonText: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes avatarRes: Int = drawable.img_donebot_sad,
    reduceMotion: Boolean = false,
) {
    val animationsEnabled = !LocalInspectionMode.current && !reduceMotion

    var bubbleVisible by remember { mutableStateOf(!animationsEnabled) }
    LaunchedEffect(animationsEnabled) {
        if (animationsEnabled) {
            delay(BUBBLE_DELAY_MS)
        }
        bubbleVisible = true
    }

    val breathingScale = if (animationsEnabled) {
        val infinite = rememberInfiniteTransition(label = "heartsBreathing")
        val scale by infinite.animateFloat(
            initialValue = 1f,
            targetValue = BREATHING_MAX_SCALE,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = BREATHING_MS, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "heartsBreathingScale",
        )
        scale
    } else {
        1f
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier.widthIn(min = 280.dp, max = 360.dp),
            shape = RoundedCornerShape(24.dp),
            color = TDTheme.colors.lightPending,
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DepletedAvatarWithHalo(avatarRes = avatarRes, breathingScale = breathingScale)

                Spacer(modifier = Modifier.height(20.dp))

                AnimatedVisibility(
                    visible = bubbleVisible,
                    enter = fadeIn(animationSpec = tween(durationMillis = BUBBLE_FADE_MS)),
                ) {
                    DepletedSpeechBubble(text = speechBubbleText)
                }

                Spacer(modifier = Modifier.height(20.dp))

                TDHealthBar(
                    halfHearts = 0,
                    contentDescription = "",
                    heartSize = 18.dp,
                    animate = false,
                )

                Spacer(modifier = Modifier.height(24.dp))

                TDButton(
                    text = buttonText,
                    onClick = onDismiss,
                    type = TDButtonType.PRIMARY,
                    fullWidth = true,
                )
            }
        }
    }
}

@Composable
private fun DepletedAvatarWithHalo(
    @DrawableRes avatarRes: Int,
    breathingScale: Float,
) {
    val haloBrush = if (TDTheme.isDark) {
        Brush.radialGradient(
            colors = listOf(
                TDTheme.colors.onBackground,
                Color.Transparent,
            ),
        )
    } else {
        Brush.radialGradient(
            colors = listOf(
                TDTheme.colors.onBackground,
                TDTheme.colors.pendingGray,
                Color.Transparent,
            ),
        )
    }
    Box(
        modifier = Modifier.size(140.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .background(brush = haloBrush, shape = CircleShape),
        )
        Box(
            modifier = Modifier
                .scale(breathingScale)
                .size(96.dp)
                .clip(CircleShape)
                .background(TDTheme.colors.lightPending),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(avatarRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape),
            )
        }
    }
}

@Composable
private fun DepletedSpeechBubble(text: String) {
    Column(
        modifier = Modifier
            .widthIn(max = 280.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(TDTheme.colors.background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TDText(
            text = text,
            style = TDTheme.typography.regularTextStyle,
            color = TDTheme.colors.onBackground,
            textAlign = TextAlign.Center,
        )
    }
}

@TDPreviewDialog
@Composable
private fun TDHeartsDepletedDialogPreviewEn() {
    TDTheme {
        TDHeartsDepletedDialog(
            speechBubbleText = "Your task streak ended! Pick up where you left off and win your hearts back.",
            buttonText = "Keep going",
            onDismiss = {},
            avatarRes = drawable.ic_bot,
        )
    }
}

@TDPreviewDialog
@Composable
private fun TDHeartsDepletedDialogPreviewTr() {
    TDTheme {
        TDHeartsDepletedDialog(
            speechBubbleText = "Görev serin sona erdi! Kaldığın yerden devam et, kalplerini yeniden kazan.",
            buttonText = "Devam et",
            onDismiss = {},
            avatarRes = drawable.ic_bot,
        )
    }
}

@TDPreviewDialog
@Composable
private fun TDHeartsDepletedDialogPreviewReducedMotion() {
    TDTheme {
        TDHeartsDepletedDialog(
            speechBubbleText = "Your task streak ended! Pick up where you left off and win your hearts back.",
            buttonText = "Keep going",
            onDismiss = {},
            avatarRes = drawable.ic_bot,
            reduceMotion = true,
        )
    }
}
