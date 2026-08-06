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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.window.DialogProperties
import com.example.uikit.R.drawable
import com.todoapp.uikit.extensions.ObscuredTouchGuard
import com.todoapp.uikit.image.rememberPixelPainter
import com.todoapp.uikit.theme.TDTheme
import com.todoapp.uikit.theme.tdCorner
import kotlinx.coroutines.delay

private const val BREATHING_MS = 3000
private const val BREATHING_MAX_SCALE = 1.03f
private const val BUBBLE_FADE_MS = 300
private const val BUBBLE_DELAY_MS = 50L

/**
 * The shared body of every dialog where DoneBot speaks to the user: the mascot in a soft radial halo,
 * breathing gently, with a speech bubble that fades in a beat later and whatever the moment calls for
 * underneath.
 *
 * It exists because there were two of these — [TDGoodbyeDialog] and [TDHeartsDepletedDialog] — with
 * the halo, the breathing curve and the fade timing copied byte for byte between them, right down to
 * two identical private `AvatarWithHalo` functions. A third copy is where that stops being tolerable.
 *
 * Callers supply everything below the bubble through [content], which is a plain `ColumnScope` — the
 * spacing between the pieces is the caller's, since a type-to-confirm field, a row of hearts and a
 * pair of buttons all want different rhythm.
 *
 * Animations skip in `LocalInspectionMode` (so previews render a stable frame) and when [reduceMotion]
 * is set.
 *
 * @param dismissable false pins the dialog open — back press and outside taps do nothing and
 *   [onDismiss] is not called. Used while an irreversible action is already in flight.
 * @param obscuredTouchGuard adds the tapjacking guard. Belongs on destructive confirmations; not
 *   worth the overlay elsewhere.
 * @param bubbleColor defaults to a bubble that reads as a distinct shape against the dialog surface.
 *   [TDGoodbyeDialog] deliberately overrides it to the surface colour, where the copy floats without
 *   a visible bubble behind it.
 */
@Composable
fun TDMascotDialog(
    speechBubbleText: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes avatarRes: Int = drawable.img_donebot_sad,
    reduceMotion: Boolean = false,
    dismissable: Boolean = true,
    obscuredTouchGuard: Boolean = false,
    bubbleColor: Color = TDTheme.colors.background,
    bubbleTextColor: Color = TDTheme.colors.onBackground,
    content: @Composable ColumnScope.() -> Unit,
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
        val infinite = rememberInfiniteTransition(label = "mascotBreathing")
        val scale by infinite.animateFloat(
            initialValue = 1f,
            targetValue = BREATHING_MAX_SCALE,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = BREATHING_MS, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "mascotBreathingScale",
        )
        scale
    } else {
        1f
    }

    Dialog(
        onDismissRequest = { if (dismissable) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = dismissable,
            dismissOnClickOutside = dismissable,
        ),
    ) {
        if (obscuredTouchGuard) ObscuredTouchGuard()
        Surface(
            modifier = modifier.widthIn(min = 280.dp, max = 360.dp),
            shape = tdCorner(24.dp),
            color = TDTheme.colors.lightPending,
            tonalElevation = 8.dp,
        ) {
            Column(
                // verticalScroll keeps everything reachable when the dialog is taller than a short
                // landscape viewport.
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MascotWithHalo(avatarRes = avatarRes, breathingScale = breathingScale)

                Spacer(modifier = Modifier.height(20.dp))

                AnimatedVisibility(
                    visible = bubbleVisible,
                    enter = fadeIn(animationSpec = tween(durationMillis = BUBBLE_FADE_MS)),
                ) {
                    MascotSpeechBubble(
                        text = speechBubbleText,
                        color = bubbleColor,
                        textColor = bubbleTextColor,
                    )
                }

                content()
            }
        }
    }
}

@Composable
private fun MascotWithHalo(
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
                painter = rememberPixelPainter(painterResource(avatarRes), 88.dp),
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
private fun MascotSpeechBubble(
    text: String,
    color: Color,
    textColor: Color,
) {
    Column(
        modifier = Modifier
            .widthIn(max = 280.dp)
            .clip(TDTheme.shapes.large)
            .background(color)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TDText(
            text = text,
            style = TDTheme.typography.regularTextStyle,
            color = textColor,
            textAlign = TextAlign.Center,
        )
    }
}
