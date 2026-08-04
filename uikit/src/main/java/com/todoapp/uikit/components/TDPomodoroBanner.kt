package com.todoapp.uikit.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreviewWide
import com.todoapp.uikit.theme.TDTheme

@Composable
fun TDPomodoroBanner(
    isBannerActivated: Boolean,
    minutes: Int = 25,
    seconds: Int = 40,
    isOverTime: Boolean,
    modeLabel: String = "Focus",
    @DrawableRes modeIconRes: Int = R.drawable.ic_sand_clock,
    backgroundColor: Color = TDTheme.colors.green,
    contentColor: Color = TDTheme.colors.onBackground,
    onClick: () -> Unit,
) {
    if (!isBannerActivated) return

    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }

    val bannerHeight by animateDpAsState(
        targetValue = if (started) 54.dp else 0.dp,
        animationSpec = tween(600),
        label = "Banner Height Animation",
    )

    val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val animatedStatusBarTopPadding by animateDpAsState(
        targetValue = if (started) statusBarTopPadding else 0.dp,
        animationSpec = tween(600),
        label = "StatusBarTopPaddingAnimation",
    )

    val animatedBg = remember { Animatable(backgroundColor) }
    LaunchedEffect(backgroundColor) {
        animatedBg.animateTo(backgroundColor, tween(400))
    }

    val timerStyle = TDTheme.typography.heading4

    Row(
        modifier =
        Modifier
            .clickable { onClick() }
            .background(animatedBg.value)
            .padding(top = animatedStatusBarTopPadding)
            .fillMaxWidth()
            .height(bannerHeight)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Left: mode icon + label
        Icon(
            painter = tdPainter(modeIconRes),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp),
        )
        TDText(
            text = modeLabel,
            style = TDTheme.typography.subheading2,
            color = contentColor,
        )

        Spacer(Modifier.weight(1f))

        // Center: animated timer
        Row(
            modifier = Modifier.width(104.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedTimeMmSs(
                minutes = minutes,
                seconds = seconds,
                style = timerStyle,
                color = contentColor,
            )
        }

        Spacer(Modifier.weight(1f))

        // Right: subtle forward arrow
        Icon(
            painter = tdPainter(R.drawable.ic_arrow_forward),
            contentDescription = null,
            tint = contentColor.copy(alpha = 0.55f),
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
fun AnimatedTimeMmSs(
    minutes: Int,
    seconds: Int,
    modifier: Modifier = Modifier,
    digitModifier: Modifier = Modifier,
    style: TextStyle,
    color: Color,
) {
    val safeMinutes = minutes.coerceAtLeast(0)
    val safeSeconds = seconds.coerceAtLeast(0)

    val mTens = (safeMinutes / 10) % 10
    val mOnes = safeMinutes % 10
    val sTens = (safeSeconds / 10) % 10
    val sOnes = safeSeconds % 10

    Row(modifier = modifier) {
        AnimatedDigit(mTens, modifier = digitModifier, style, color)
        AnimatedDigit(mOnes, modifier = digitModifier, style, color)
        TDText(text = ":", style = style, color = color)
        AnimatedDigit(sTens, modifier = digitModifier, style, color)
        AnimatedDigit(sOnes, modifier = digitModifier, style, color)
    }
}

@Composable
private fun AnimatedDigit(
    digit: Int,
    modifier: Modifier = Modifier,
    style: TextStyle,
    color: Color,
) {
    AnimatedContent(
        targetState = digit,
        label = "timer_digit",
        modifier = modifier,
        contentAlignment = Alignment.Center,
        transitionSpec = {
            (slideInVertically { it } + fadeIn()) togetherWith
                (slideOutVertically { -it } + fadeOut()) using
                SizeTransform(clip = true) { _, _ -> snap() }
        },
    ) { d ->
        TDText(
            text = d.toString(),
            style = style,
            color = color,
        )
    }
}

@TDPreviewWide
@Composable
private fun TdPomodoroBannerFocusPreview() {
    TDTheme {
        TDPomodoroBanner(
            isBannerActivated = true,
            minutes = 24,
            seconds = 57,
            isOverTime = false,
            modeLabel = "Focus",
            modeIconRes = R.drawable.ic_sand_clock,
            backgroundColor = TDTheme.colors.green.copy(alpha = 0.25f),
            contentColor = TDTheme.colors.onBackground,
            onClick = {},
        )
    }
}

@TDPreviewWide
@Composable
private fun TdPomodoroBannerOverTimePreview() {
    TDTheme {
        TDPomodoroBanner(
            isBannerActivated = true,
            minutes = 0,
            seconds = 42,
            isOverTime = true,
            modeLabel = "Overtime",
            modeIconRes = R.drawable.ic_sand_clock,
            backgroundColor = TDTheme.colors.red.copy(alpha = 0.25f),
            contentColor = TDTheme.colors.onBackground,
            onClick = {},
        )
    }
}

@TDPreviewWide
@Composable
private fun TdPomodoroBannerShortBreakPreview() {
    TDTheme {
        TDPomodoroBanner(
            isBannerActivated = true,
            minutes = 4,
            seconds = 15,
            isOverTime = false,
            modeLabel = "Short Break",
            modeIconRes = R.drawable.ic_sand_clock,
            backgroundColor = TDTheme.colors.lightOrange,
            contentColor = TDTheme.colors.onBackground,
            onClick = {},
        )
    }
}

@TDPreviewWide
@Composable
private fun TdPomodoroBannerLongBreakPreview() {
    TDTheme {
        TDPomodoroBanner(
            isBannerActivated = true,
            minutes = 14,
            seconds = 30,
            isOverTime = false,
            modeLabel = "Long Break",
            modeIconRes = R.drawable.ic_sand_clock,
            backgroundColor = TDTheme.colors.bgColorPurple,
            contentColor = TDTheme.colors.onBackground,
            onClick = {},
        )
    }
}
