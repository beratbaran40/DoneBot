package com.todoapp.mobile.ui.pomodoro

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.pomodoro.PomodoroContract.UiAction
import com.todoapp.uikit.components.AnimatedTimeMmSs
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.modifier.tdShadow
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme
import com.example.uikit.R as UiKitR

/**
 * The pieces both orientations of the timer share: the ring with its countdown, the mode pill and
 * the end-session chip.
 */

@Composable
fun PomodoroProgressRing(
    min: Int,
    second: Int,
    progress: Float,
    progressColor: Color,
    trackColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = ProgressRingReferenceSize,
) {
    // Scale the timer text to the ring so mm:ss always fits inside. Portrait and tablets keep the
    // full 320dp ring (scale 1, unchanged); phone landscape uses a 220dp ring and the text shrinks
    // with it.
    val scale = size / ProgressRingReferenceSize
    val timerStyle = TDTheme.typography.pomodoro.let { it.copy(fontSize = (it.fontSize.value * scale).sp) }

    // The digit minimum exists to stop the countdown twitching as the numbers change, which only
    // happens when the face gives its digits different advances — Poppins runs from 376 to 677 per
    // em. A tabular face has no such twitch, and forcing its digits into a wider cell would push the
    // row past the ring's inner diameter, because a monospace `:` also claims a full digit advance.
    // So ask the face rather than assume it.
    val measurer = rememberTextMeasurer()
    val digitMinWidth =
        remember(timerStyle, measurer, scale) {
            val widths = (0..9).map { measurer.measure(it.toString(), timerStyle).size.width }
            if (widths.distinct().size == 1) 0.dp else ProgressRingDigitMinWidth * scale
        }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        PomodoroTimerRing(
            progress = progress,
            progressColor = progressColor,
            trackColor = trackColor,
            size = size,
            strokeWidth = RingStrokeWidth,
        )
        AnimatedTimeMmSs(
            minutes = min,
            seconds = second,
            style = timerStyle,
            color = textColor,
            digitModifier = Modifier.widthIn(min = digitMinWidth),
        )
    }
}

@Composable
fun PomodoroModeCard(
    mode: PomodoroModeUi,
    surfaceColor: Color,
    contentColor: Color,
    lightShadow: Color,
    darkShadow: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
        modifier
            .tdShadow(
                lightShadow = lightShadow,
                darkShadow = darkShadow,
                cornerRadius = ModeCardCornerRadius,
                elevation = ModeCardElevation,
            ).clip(TDTheme.shapes.xLarge)
            .background(surfaceColor)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painter = tdPainter(mode.iconRes),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(22.dp),
        )
        TDText(
            text = stringResource(mode.titleRes),
            style = TDTheme.typography.heading5,
            color = contentColor,
        )
    }
}

@Composable
fun PomodoroEndSessionChip(
    contentColor: Color,
    onAction: (UiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
        modifier
            .clip(TDTheme.shapes.large)
            .clickable { onAction(UiAction.OnEndSessionTap) }
            .background(contentColor.copy(alpha = CHIP_FILL_ALPHA))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            painter = tdPainter(UiKitR.drawable.ic_delete),
            contentDescription = null,
            tint = contentColor.copy(alpha = CHIP_INK_ALPHA),
            modifier = Modifier.size(14.dp),
        )
        TDText(
            text = stringResource(R.string.pomodoro_end_session),
            style = TDTheme.typography.subheading1,
            color = contentColor.copy(alpha = CHIP_INK_ALPHA),
        )
    }
}

// Timer text scales with the ring: 96sp text / 64dp digit slots at the reference 320dp ring,
// shrinking proportionally for the smaller 220dp phone-landscape ring so mm:ss never overflows.
internal val ProgressRingReferenceSize = 320.dp
private val ProgressRingDigitMinWidth = 64.dp

private val RingStrokeWidth = 16.dp
private val ModeCardCornerRadius = 20.dp
private val ModeCardElevation = 8.dp
private const val CHIP_FILL_ALPHA = 0.08f
private const val CHIP_INK_ALPHA = 0.65f

// ── Previews ──────────────────────────────────────────────────────────────────

@TDPreview
@Composable
private fun PomodoroProgressRingLandscapePreview() {
    // The 220dp ring used on phone landscape: the timer text must stay inside the circle.
    TDTheme {
        PomodoroProgressRing(
            min = 24,
            second = 57,
            progress = 0.7f,
            progressColor = TDTheme.colors.purple,
            trackColor = TDTheme.colors.lightGray,
            textColor = TDTheme.colors.onBackground,
            size = 220.dp,
        )
    }
}

@TDPreview
@Composable
private fun PomodoroModeCardPreview() {
    TDTheme {
        val palette = PomodoroModeTheme.resolve(ModeColorKey.Focus, TDTheme.isDark)
        PomodoroModeCard(
            mode = PomodoroModeUiPreset.Focus.value,
            surfaceColor = palette.surface,
            contentColor = palette.content,
            lightShadow = palette.lightShadow,
            darkShadow = palette.darkShadow,
        )
    }
}
