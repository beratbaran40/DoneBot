package com.todoapp.mobile.ui.pomodoro

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.common.RingtoneHolder
import com.todoapp.mobile.common.toUiMode
import com.todoapp.mobile.domain.engine.PomodoroMode
import com.todoapp.mobile.ui.pomodoro.PomodoroContract.UiAction
import com.todoapp.mobile.ui.pomodoro.PomodoroContract.UiEffect
import com.todoapp.mobile.ui.pomodoro.PomodoroContract.UiState
import com.todoapp.uikit.components.AnimatedTimeMmSs
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.modifier.neumorphicShadow
import com.todoapp.uikit.theme.TDTheme
import kotlinx.coroutines.flow.Flow

@Composable
fun PomodoroScreen(
    uiState: UiState,
    uiEffect: Flow<UiEffect>,
    onAction: (UiAction) -> Unit,
) {
    val ringtoneHolder = remember { RingtoneHolder() }
    val context = LocalContext.current

    DisposableEffect(Unit) {
        onAction(UiAction.ToggleBannerVisibility(false))
        onDispose { onAction(UiAction.ToggleBannerVisibility(true)) }
    }

    LaunchedEffect(Unit) {
        uiEffect.collect { effect ->
            when (effect) {
                is UiEffect.SessionFinished -> ringtoneHolder.play(context)
            }
        }
    }

    PomodoroContent(uiState = uiState, onAction = onAction)
}

@Composable
private fun PomodoroContent(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
) {
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    if (isPortrait) {
        PomodoroPortraitContent(uiState = uiState, onAction = onAction)
    } else {
        PomodoroLandscapeContent(uiState = uiState, onAction = onAction)
    }
}

@Composable
private fun PomodoroPortraitContent(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
) {
    // Resolve per-mode palette (handles both light & dark theme)
    val isDark = TDTheme.isDark
    val targetPalette =
        remember(uiState.mode.colorKey, isDark) {
            PomodoroModeTheme.resolve(uiState.mode.colorKey, isDark)
        }

    // Animate every color when mode changes (400ms crossfade)
    val bgColor by animateColorAsState(targetPalette.background, tween(COLOR_ANIM_MS), "pomoBg")
    val surfaceColor by animateColorAsState(targetPalette.surface, tween(COLOR_ANIM_MS), "pomoSurface")
    val contentColor by animateColorAsState(targetPalette.content, tween(COLOR_ANIM_MS), "pomoContent")
    val trackColor by animateColorAsState(targetPalette.track, tween(COLOR_ANIM_MS), "pomoTrack")
    val lightShadow by animateColorAsState(targetPalette.lightShadow, tween(COLOR_ANIM_MS), "pomoLightShadow")
    val darkShadow by animateColorAsState(targetPalette.darkShadow, tween(COLOR_ANIM_MS), "pomoDarkShadow")

    // derivedStateOf: timer sub-composables skip recomposition when non-timer fields change
    val progressFraction by remember(uiState.min, uiState.second, uiState.totalSessionSeconds) {
        derivedStateOf {
            val remaining = uiState.min * 60L + uiState.second
            val total = uiState.totalSessionSeconds
            if (total > 0L) remaining.toFloat() / total.toFloat() else 1f
        }
    }
    // Animate ring arc — smooth flow between each 1-second tick
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = PROGRESS_ANIM_MS, easing = LinearEasing),
        label = "pomoProgress",
    )

    Box(
        modifier =
        Modifier
            .fillMaxSize()
            .background(bgColor),
    ) {
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(20.dp))

            PomodoroSessionDots(
                totalSessions = uiState.totalSessions,
                currentIndex = uiState.currentSessionIndex,
                contentColor = contentColor,
                dimColor = surfaceColor,
            )

            Spacer(Modifier.weight(1f))

            // Ring with timer text overlaid in center
            ProgressRing(
                min = uiState.min,
                second = uiState.second,
                animatedProgress = animatedProgress,
                progressColor = contentColor,
                trackColor = trackColor,
                textColor = contentColor,
            )

            Spacer(Modifier.height(28.dp))

            // Mode card (slightly raised neumorphic pill)
            ModeCard(
                mode = uiState.mode,
                surfaceColor = surfaceColor,
                contentColor = contentColor,
                lightShadow = lightShadow,
                darkShadow = darkShadow,
            )

            if (!uiState.infoMessage.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                TDText(
                    text = uiState.infoMessage,
                    style = TDTheme.typography.subheading1,
                    color = contentColor.copy(alpha = 0.6f),
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier =
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onAction(UiAction.OnEndSessionTap) }
                    .background(contentColor.copy(alpha = 0.08f))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    painter = painterResource(com.example.uikit.R.drawable.ic_delete),
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.65f),
                    modifier = Modifier.size(14.dp),
                )
                TDText(
                    text = stringResource(R.string.pomodoro_end_session),
                    style = TDTheme.typography.subheading1,
                    color = contentColor.copy(alpha = 0.65f),
                )
            }

            Spacer(Modifier.weight(1f))

            ControlPanel(
                isRunning = uiState.isRunning,
                isOvertime = uiState.isOvertime,
                surfaceColor = surfaceColor,
                contentColor = contentColor,
                lightShadow = lightShadow,
                darkShadow = darkShadow,
                onAction = onAction,
            )

            Spacer(Modifier.height(40.dp))
        }

        if (uiState.showFinishEarlyDialog) {
            AlertDialog(
                onDismissRequest = { onAction(UiAction.DismissEndSessionDialog) },
                title = {
                    TDText(
                        text = stringResource(R.string.pomodoro_end_session_title),
                        style = TDTheme.typography.heading5,
                        color = TDTheme.colors.onBackground,
                    )
                },
                text = {
                    TDText(
                        text = stringResource(R.string.pomodoro_end_session_message),
                        style = TDTheme.typography.subheading1,
                        color = TDTheme.colors.onBackground.copy(alpha = 0.7f),
                    )
                },
                confirmButton = {
                    TextButton(onClick = { onAction(UiAction.ConfirmEndSession) }) {
                        TDText(
                            text = stringResource(R.string.pomodoro_end_session),
                            style = TDTheme.typography.heading6,
                            color = TDTheme.colors.crossRed,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onAction(UiAction.DismissEndSessionDialog) }) {
                        TDText(
                            text = stringResource(R.string.pomodoro_keep_going),
                            style = TDTheme.typography.heading6,
                            color = TDTheme.colors.pendingGray,
                        )
                    }
                },
                containerColor = TDTheme.colors.surface,
            )
        }
    }
}

@Composable
private fun PomodoroLandscapeContent(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
) {
    val isDark = TDTheme.isDark
    val targetPalette =
        remember(uiState.mode.colorKey, isDark) {
            PomodoroModeTheme.resolve(uiState.mode.colorKey, isDark)
        }

    val bgColor by animateColorAsState(targetPalette.background, tween(COLOR_ANIM_MS), "pomoBgL")
    val surfaceColor by animateColorAsState(targetPalette.surface, tween(COLOR_ANIM_MS), "pomoSurfaceL")
    val contentColor by animateColorAsState(targetPalette.content, tween(COLOR_ANIM_MS), "pomoContentL")
    val trackColor by animateColorAsState(targetPalette.track, tween(COLOR_ANIM_MS), "pomoTrackL")
    val lightShadow by animateColorAsState(targetPalette.lightShadow, tween(COLOR_ANIM_MS), "pomoLightShadowL")
    val darkShadow by animateColorAsState(targetPalette.darkShadow, tween(COLOR_ANIM_MS), "pomoDarkShadowL")

    val progressFraction by remember(uiState.min, uiState.second, uiState.totalSessionSeconds) {
        derivedStateOf {
            val remaining = uiState.min * 60L + uiState.second
            val total = uiState.totalSessionSeconds
            if (total > 0L) remaining.toFloat() / total.toFloat() else 1f
        }
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = PROGRESS_ANIM_MS, easing = LinearEasing),
        label = "pomoProgressL",
    )

    Box(
        modifier =
        Modifier
            .fillMaxSize()
            .background(bgColor),
    ) {
        Row(
            modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
        ) {
            Column(
                modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                PomodoroSessionDots(
                    totalSessions = uiState.totalSessions,
                    currentIndex = uiState.currentSessionIndex,
                    contentColor = contentColor,
                    dimColor = surfaceColor,
                )
                Spacer(Modifier.height(12.dp))
                ProgressRing(
                    min = uiState.min,
                    second = uiState.second,
                    animatedProgress = animatedProgress,
                    progressColor = contentColor,
                    trackColor = trackColor,
                    textColor = contentColor,
                    size = 220.dp,
                )
            }

            Column(
                modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                ModeCard(
                    mode = uiState.mode,
                    surfaceColor = surfaceColor,
                    contentColor = contentColor,
                    lightShadow = lightShadow,
                    darkShadow = darkShadow,
                )

                if (!uiState.infoMessage.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    TDText(
                        text = uiState.infoMessage,
                        style = TDTheme.typography.subheading1,
                        color = contentColor.copy(alpha = 0.6f),
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier =
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onAction(UiAction.OnEndSessionTap) }
                        .background(contentColor.copy(alpha = 0.08f))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        painter = painterResource(com.example.uikit.R.drawable.ic_delete),
                        contentDescription = null,
                        tint = contentColor.copy(alpha = 0.65f),
                        modifier = Modifier.size(14.dp),
                    )
                    TDText(
                        text = stringResource(R.string.pomodoro_end_session),
                        style = TDTheme.typography.subheading1,
                        color = contentColor.copy(alpha = 0.65f),
                    )
                }

                Spacer(Modifier.height(16.dp))

                ControlPanel(
                    isRunning = uiState.isRunning,
                    isOvertime = uiState.isOvertime,
                    surfaceColor = surfaceColor,
                    contentColor = contentColor,
                    lightShadow = lightShadow,
                    darkShadow = darkShadow,
                    onAction = onAction,
                )
            }
        }

        if (uiState.showFinishEarlyDialog) {
            AlertDialog(
                onDismissRequest = { onAction(UiAction.DismissEndSessionDialog) },
                title = {
                    TDText(
                        text = stringResource(R.string.pomodoro_end_session_title),
                        style = TDTheme.typography.heading5,
                        color = TDTheme.colors.onBackground,
                    )
                },
                text = {
                    TDText(
                        text = stringResource(R.string.pomodoro_end_session_message),
                        style = TDTheme.typography.subheading1,
                        color = TDTheme.colors.onBackground.copy(alpha = 0.7f),
                    )
                },
                confirmButton = {
                    TextButton(onClick = { onAction(UiAction.ConfirmEndSession) }) {
                        TDText(
                            text = stringResource(R.string.pomodoro_end_session),
                            style = TDTheme.typography.heading6,
                            color = TDTheme.colors.crossRed,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onAction(UiAction.DismissEndSessionDialog) }) {
                        TDText(
                            text = stringResource(R.string.pomodoro_keep_going),
                            style = TDTheme.typography.heading6,
                            color = TDTheme.colors.pendingGray,
                        )
                    }
                },
                containerColor = TDTheme.colors.surface,
            )
        }
    }
}

@Composable
private fun ProgressRing(
    modifier: Modifier = Modifier,
    min: Int,
    second: Int,
    animatedProgress: Float,
    progressColor: androidx.compose.ui.graphics.Color,
    trackColor: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    size: Dp = 320.dp,
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        PomodoroTimerRing(
            progress = animatedProgress,
            progressColor = progressColor,
            trackColor = trackColor,
            size = size,
            strokeWidth = 16.dp,
        )
        AnimatedTimeMmSs(
            minutes = min,
            seconds = second,
            style = TDTheme.typography.pomodoro,
            color = textColor,
            digitModifier = Modifier.widthIn(min = 64.dp),
        )
    }
}

@Composable
private fun ModeCard(
    mode: PomodoroModeUi,
    surfaceColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    lightShadow: androidx.compose.ui.graphics.Color,
    darkShadow: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
        modifier
            .neumorphicShadow(
                lightShadow = lightShadow,
                darkShadow = darkShadow,
                cornerRadius = 20.dp,
                elevation = 8.dp,
            ).clip(RoundedCornerShape(20.dp))
            .background(surfaceColor)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painter = painterResource(mode.iconRes),
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
private fun ControlPanel(
    isRunning: Boolean,
    isOvertime: Boolean,
    surfaceColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    lightShadow: androidx.compose.ui.graphics.Color,
    darkShadow: androidx.compose.ui.graphics.Color,
    onAction: (UiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    PomodoroControls(
        isRunning = isRunning,
        isOvertime = isOvertime,
        surfaceColor = surfaceColor,
        contentColor = contentColor,
        lightShadow = lightShadow,
        darkShadow = darkShadow,
        onAction = onAction,
        modifier = modifier,
    )
}

private const val COLOR_ANIM_MS = 400
private const val PROGRESS_ANIM_MS = 900

// ── Previews ──────────────────────────────────────────────────────────────────

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun Preview_Focus() {
    TDTheme {
        PomodoroContent(
            uiState =
            UiState(
                min = 24,
                second = 57,
                mode = PomodoroMode.Focus.toUiMode(),
                totalSessionSeconds = 25L * 60L,
                totalSessions = 15,
                currentSessionIndex = 2,
                isRunning = true,
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun Preview_ShortBreak() {
    TDTheme {
        PomodoroContent(
            uiState =
            UiState(
                min = 4,
                second = 30,
                mode = PomodoroMode.ShortBreak.toUiMode(),
                totalSessionSeconds = 5L * 60L,
                totalSessions = 15,
                currentSessionIndex = 5,
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun Preview_LongBreak() {
    TDTheme {
        PomodoroContent(
            uiState =
            UiState(
                min = 18,
                second = 0,
                mode = PomodoroMode.LongBreak.toUiMode(),
                totalSessionSeconds = 20L * 60L,
                totalSessions = 15,
                currentSessionIndex = 7,
                isRunning = true,
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun Preview_OverTime() {
    TDTheme {
        PomodoroContent(
            uiState =
            UiState(
                min = 0,
                second = 42,
                mode = PomodoroMode.OverTime.toUiMode(),
                totalSessionSeconds = 25L * 60L,
                totalSessions = 15,
                currentSessionIndex = 2,
                isRunning = true,
                isOvertime = true,
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun Preview_Paused() {
    TDTheme {
        PomodoroContent(
            uiState =
            UiState(
                min = 12,
                second = 30,
                mode = PomodoroMode.Focus.toUiMode(),
                totalSessionSeconds = 25L * 60L,
                totalSessions = 15,
                currentSessionIndex = 2,
                isRunning = false,
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun Preview_FinishEarlyDialog() {
    TDTheme {
        PomodoroContent(
            uiState =
            UiState(
                min = 8,
                second = 15,
                mode = PomodoroMode.Focus.toUiMode(),
                totalSessionSeconds = 25L * 60L,
                totalSessions = 15,
                currentSessionIndex = 2,
                isRunning = true,
                showFinishEarlyDialog = true,
            ),
            onAction = {},
        )
    }
}
