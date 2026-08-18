package com.todoapp.mobile.ui.banner

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.todoapp.mobile.R
import com.todoapp.mobile.common.RingtoneHolder
import com.todoapp.mobile.domain.engine.PomodoroMode
import com.todoapp.mobile.ui.banner.BannerContract.UiAction
import com.todoapp.mobile.ui.banner.BannerContract.UiState
import com.todoapp.mobile.ui.pomodoro.ModeColorKey
import com.todoapp.mobile.ui.pomodoro.PomodoroModeTheme
import com.todoapp.uikit.components.TDPomodoroBanner
import com.todoapp.uikit.theme.TDTheme
import kotlinx.coroutines.flow.Flow

@Composable
fun BannerOverlay(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
    uiEffect: Flow<BannerContract.UiEffect>,
) {
    val ringToneHolder = remember { RingtoneHolder() }
    val context = LocalContext.current

    DisposableEffect(ringToneHolder) {
        onDispose { ringToneHolder.stop() }
    }

    LaunchedEffect(uiEffect) {
        uiEffect.collect { effect ->
            when (effect) {
                is BannerContract.UiEffect.SessionFinished -> {
                    ringToneHolder.play(context)
                }
            }
        }
    }

    BannerContent(uiState, onAction)
}

@Composable
fun BannerContent(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
) {
    if (!uiState.isVisible) return

    val isDark = TDTheme.isDark
    val palette =
        PomodoroModeTheme.resolve(
            colorKey = uiState.mode.toModeColorKey(),
            isDark = isDark,
            palette = TDTheme.palette,
            colors = TDTheme.colors,
        )

    TDPomodoroBanner(
        minutes = uiState.minutes ?: 0,
        seconds = uiState.seconds ?: 0,
        isBannerActivated = uiState.isBannerActivated,
        isOverTime = uiState.isOverTime ?: false,
        modeLabel = stringResource(uiState.mode.toLabelRes()),
        modeIconRes = uiState.mode.toIconRes(),
        backgroundColor = palette.surface,
        contentColor = palette.content,
        onClick = { onAction(UiAction.OnBannerTap) },
    )
}

private fun PomodoroMode.toModeColorKey(): ModeColorKey = when (this) {
    PomodoroMode.Focus -> ModeColorKey.Focus
    PomodoroMode.ShortBreak -> ModeColorKey.ShortBreak
    PomodoroMode.LongBreak -> ModeColorKey.LongBreak
    PomodoroMode.OverTime -> ModeColorKey.OverTime
}

private fun PomodoroMode.toLabelRes(): Int = when (this) {
    PomodoroMode.Focus -> R.string.pomodoro_mode_focus
    PomodoroMode.ShortBreak -> R.string.pomodoro_mode_short_break
    PomodoroMode.LongBreak -> R.string.pomodoro_mode_long_break
    PomodoroMode.OverTime -> R.string.pomodoro_mode_overtime
}

@DrawableRes
private fun PomodoroMode.toIconRes(): Int = when (this) {
    PomodoroMode.Focus -> R.drawable.ic_focus
    PomodoroMode.ShortBreak -> R.drawable.ic_short_break
    PomodoroMode.LongBreak -> R.drawable.ic_long_break
    PomodoroMode.OverTime -> R.drawable.ic_overtime
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun BannerContentPreview() {
    TDTheme {
        BannerContent(
            uiState =
            UiState(
                isVisible = true,
                isBannerActivated = true,
                minutes = 25,
                seconds = 0,
                mode = PomodoroMode.Focus,
                isOverTime = false,
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun BannerContentShortBreakPreview() {
    TDTheme {
        BannerContent(
            uiState =
            UiState(
                isVisible = true,
                isBannerActivated = true,
                minutes = 4,
                seconds = 12,
                mode = PomodoroMode.ShortBreak,
                isOverTime = false,
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun BannerContentLongBreakPreview() {
    TDTheme {
        BannerContent(
            uiState =
            UiState(
                isVisible = true,
                isBannerActivated = true,
                minutes = 14,
                seconds = 0,
                mode = PomodoroMode.LongBreak,
                isOverTime = false,
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun BannerContentOverTimePreview() {
    TDTheme {
        BannerContent(
            uiState =
            UiState(
                isVisible = true,
                isBannerActivated = true,
                minutes = 0,
                seconds = 30,
                mode = PomodoroMode.OverTime,
                isOverTime = true,
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun BannerContentHiddenPreview() {
    TDTheme {
        BannerContent(
            uiState =
            UiState(
                isVisible = false,
                isBannerActivated = false,
                minutes = 0,
                seconds = 0,
                mode = PomodoroMode.Focus,
                isOverTime = false,
            ),
            onAction = {},
        )
    }
}
