package com.todoapp.mobile.ui.pomodorosummary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.pomodorosummary.PomodoroSummaryContract.UiAction
import com.todoapp.mobile.ui.pomodorosummary.PomodoroSummaryContract.UiState
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDButtonType
import com.todoapp.uikit.components.TDFitPolicy
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.components.tdConfettiProperties
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.modifier.tdShadow
import com.todoapp.uikit.theme.TDTheme
import com.example.uikit.R as UiKitR

@Composable
fun PomodoroSummaryScreen(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
) {
    Box(
        modifier =
        Modifier
            .fillMaxSize()
            .background(TDTheme.colors.green.copy(alpha = 0.08f)),
    ) {
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            Box(
                modifier =
                Modifier
                    .size(80.dp)
                    .clip(TDTheme.shapes.circle)
                    .background(TDTheme.colors.pendingGray),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = tdPainter(UiKitR.drawable.ic_check_svg),
                    contentDescription = null,
                    tint = TDTheme.colors.white,
                    modifier = Modifier.size(40.dp),
                )
            }

            Spacer(Modifier.height(16.dp))

            TDText(
                text = stringResource(R.string.pomodoro_great_work),
                style = TDTheme.typography.heading1,
                color = TDTheme.colors.onBackground,
            )

            Spacer(Modifier.height(4.dp))

            TDText(
                text = uiState.completedAt,
                style = TDTheme.typography.subheading1,
                color = TDTheme.colors.onBackground,
            )

            Spacer(Modifier.height(32.dp))

            // A third of a 360dp screen leaves each label ~80dp. That is enough for every label to
            // wrap onto a second line, but not for a word to be BROKEN, so IntrinsicSize.Min keeps
            // the three cards level when one of them takes two lines and the others don't.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PomodoroStatCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    label = stringResource(R.string.pomodoro_focus_sessions),
                    value = uiState.focusSessions.toString(),
                )
                PomodoroStatCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    label = stringResource(R.string.pomodoro_total_focus),
                    value = stringResource(R.string.pomodoro_minutes_abbrev, uiState.totalFocusMinutes),
                )
                PomodoroStatCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    label = stringResource(R.string.pomodoro_break_time),
                    value = stringResource(R.string.pomodoro_minutes_abbrev, uiState.totalBreakMinutes),
                )
            }

            Spacer(Modifier.weight(1f))

            TDButton(
                text = stringResource(R.string.pomodoro_start_again),
                type = TDButtonType.PRIMARY,
                fullWidth = true,
                onClick = { onAction(UiAction.OnStartAgainTap) },
            )
            Spacer(Modifier.height(12.dp))
            TDButton(
                text = stringResource(R.string.pomodoro_edit_settings),
                type = TDButtonType.OUTLINE,
                fullWidth = true,
                onClick = { onAction(UiAction.OnEditSettingsTap) },
            )
            Spacer(Modifier.height(12.dp))
            TDButton(
                text = stringResource(R.string.close),
                type = TDButtonType.CANCEL,
                fullWidth = true,
                onClick = { onAction(UiAction.OnCloseTap) },
            )

            Spacer(Modifier.height(32.dp))
        }

        ConfettiOverlay()
    }
}

@Composable
private fun ConfettiOverlay() {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(UiKitR.raw.confetti))
    val progress by animateLottieCompositionAsState(composition = composition, iterations = 1)
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        dynamicProperties = tdConfettiProperties(),
    )
}

@Composable
private fun PomodoroStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val isDark = TDTheme.isDark
    val statShape = TDTheme.shapes.large
    Column(
        modifier =
        modifier
            .then(
                if (isDark) {
                    Modifier.border(1.dp, TDTheme.colors.lightGray.copy(alpha = 0.2f), statShape)
                } else {
                    Modifier.tdShadow(
                        lightShadow = TDTheme.colors.white.copy(alpha = 0.85f),
                        darkShadow = TDTheme.colors.darkPending.copy(alpha = 0.18f),
                        cornerRadius = 16.dp,
                        elevation = 5.dp,
                    )
                },
            ).clip(statShape)
            .background(TDTheme.colors.pendingGray)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TDText(
            text = value,
            style = TDTheme.typography.heading2,
            color = TDTheme.colors.white,
        )
        Spacer(Modifier.height(4.dp))
        TDText(
            text = label,
            style = TDTheme.typography.subheading1,
            color = TDTheme.colors.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            fitPolicy = TDFitPolicy.BOUNDED,
            slot = "PomodoroStatCard.label",
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun PomodoroSummaryPreview() {
    TDTheme {
        PomodoroSummaryScreen(
            uiState =
            UiState(
                focusSessions = 4,
                totalFocusMinutes = 100,
                totalBreakMinutes = 30,
                completedAt = "Thu, Apr 3 · 14:25",
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun PomodoroSummaryShortPreview() {
    TDTheme {
        PomodoroSummaryScreen(
            uiState =
            UiState(
                focusSessions = 1,
                totalFocusMinutes = 25,
                totalBreakMinutes = 5,
                completedAt = "Mon, May 13 · 09:15",
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun PomodoroSummaryLongPreview() {
    TDTheme {
        PomodoroSummaryScreen(
            uiState =
            UiState(
                focusSessions = 8,
                totalFocusMinutes = 200,
                totalBreakMinutes = 60,
                completedAt = "Wed, May 13 · 18:42",
            ),
            onAction = {},
        )
    }
}

/** The three stat cards are the tightest text slots on this screen — ~80dp each at 360dp. */
@com.todoapp.uikit.previews.TDPreviewNarrow
@Composable
private fun PomodoroSummaryNarrowPreview() {
    TDTheme {
        PomodoroSummaryScreen(
            uiState =
            UiState(
                focusSessions = 12,
                totalFocusMinutes = 300,
                totalBreakMinutes = 75,
                completedAt = "Wed, May 13 · 18:42",
            ),
            onAction = {},
        )
    }
}
