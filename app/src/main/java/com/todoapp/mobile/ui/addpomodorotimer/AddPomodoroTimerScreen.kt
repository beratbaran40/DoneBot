package com.todoapp.mobile.ui.addpomodorotimer

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.addpomodorotimer.AddPomodoroTimerContract.UiAction
import com.todoapp.mobile.ui.addpomodorotimer.AddPomodoroTimerContract.UiState
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDButtonType
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.modifier.tdShadow
import com.todoapp.uikit.theme.TDTheme
import com.example.uikit.R as UiKitR

@Composable
fun AddPomodoroTimerScreen(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
) {
    AddPomodoroTimerContent(uiState, onAction)
}

@Composable
private fun AddPomodoroTimerContent(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
) {
    val totalMinutes =
        remember(
            uiState.sessionCount,
            uiState.focusTime,
            uiState.shortBreak,
            uiState.longBreak,
            uiState.sectionCount,
        ) {
            val s = uiState.sessionCount.toInt()
            val f = uiState.focusTime.toInt()
            val sh = uiState.shortBreak.toInt()
            val lo = uiState.longBreak.toInt()
            val sec = uiState.sectionCount.toInt()
            var t = s * f
            for (i in 1 until s) {
                t += if (i % sec == 0) lo else sh
            }
            t
        }

    Column(
        modifier =
        Modifier
            .statusBarsPadding()
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            TDText(
                text = stringResource(R.string.pomodoro_configure_timer),
                style = TDTheme.typography.heading2,
                color = TDTheme.colors.onBackground,
            )
            TDText(
                text = stringResource(R.string.pomodoro_configure_timer_subtitle),
                style = TDTheme.typography.subheading1,
                color = TDTheme.colors.onBackground.copy(alpha = 0.55f),
            )
        }

        PomodoroPreviewCard(
            totalMinutes = totalMinutes,
            sessionCount = uiState.sessionCount.toInt(),
            focusTime = uiState.focusTime.toInt(),
            shortBreak = uiState.shortBreak.toInt(),
            longBreak = uiState.longBreak.toInt(),
        )

        Spacer(Modifier.height(24.dp))
        PomodoroSectionHeader(stringResource(R.string.pomodoro_sessions_label))
        Spacer(Modifier.height(10.dp))

        PomodoroStepperCard(
            value = uiState.sessionCount.toInt(),
            label = stringResource(R.string.pomodoro_session_count),
            unit = "",
            iconRes = UiKitR.drawable.ic_plus,
            cardBgColor = TDTheme.colors.lightPending,
            iconTintColor = TDTheme.colors.darkPending,
            range = 1..15,
            step = 1,
            onValueChange = { onAction(UiAction.OnSessionCountChange(it)) },
        )
        Spacer(Modifier.height(10.dp))
        PomodoroStepperCard(
            value = uiState.sectionCount.toInt(),
            label = stringResource(R.string.pomodoro_sessions_per_cycle),
            unit = "",
            iconRes = UiKitR.drawable.ic_clock,
            cardBgColor = TDTheme.colors.lightPending,
            iconTintColor = TDTheme.colors.darkPending,
            range = 1..10,
            step = 1,
            onValueChange = { onAction(UiAction.OnSectionCountChange(it)) },
        )

        Spacer(Modifier.height(24.dp))
        PomodoroSectionHeader(stringResource(R.string.pomodoro_section_duration))
        Spacer(Modifier.height(10.dp))

        PomodoroStepperCard(
            value = uiState.focusTime.toInt(),
            label = stringResource(R.string.pomodoro_focus_time),
            unit = stringResource(R.string.pomodoro_unit_min),
            iconRes = R.drawable.ic_focus,
            cardBgColor = TDTheme.colors.lightGreen,
            iconTintColor = TDTheme.colors.darkGreen,
            range = 5..120,
            step = 5,
            onValueChange = { onAction(UiAction.OnFocusTimeChange(it)) },
        )
        Spacer(Modifier.height(10.dp))
        PomodoroStepperCard(
            value = uiState.shortBreak.toInt(),
            label = stringResource(R.string.pomodoro_short_break_label),
            unit = stringResource(R.string.pomodoro_unit_min),
            iconRes = R.drawable.ic_short_break,
            cardBgColor = TDTheme.colors.lightRed,
            iconTintColor = TDTheme.colors.crossRed,
            range = 1..30,
            step = 1,
            onValueChange = { onAction(UiAction.OnShortBreakChange(it)) },
        )
        Spacer(Modifier.height(10.dp))
        PomodoroStepperCard(
            value = uiState.longBreak.toInt(),
            label = stringResource(R.string.pomodoro_long_break_label),
            unit = stringResource(R.string.pomodoro_unit_min),
            iconRes = R.drawable.ic_long_break,
            cardBgColor = TDTheme.colors.lightPending,
            iconTintColor = TDTheme.colors.darkPending,
            range = 5..60,
            step = 5,
            onValueChange = { onAction(UiAction.OnLongBreakChange(it)) },
        )

        Spacer(Modifier.height(28.dp))
        Row(Modifier.fillMaxWidth()) {
            TDButton(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.cancel),
                type = TDButtonType.CANCEL,
            ) {
                onAction(UiAction.OnCancelTap)
            }
            Spacer(Modifier.width(16.dp))
            TDButton(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.start),
                type = TDButtonType.PRIMARY,
            ) {
                onAction(UiAction.OnStartTap)
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun PomodoroPreviewCard(
    totalMinutes: Int,
    sessionCount: Int,
    focusTime: Int,
    shortBreak: Int,
    longBreak: Int,
    modifier: Modifier = Modifier,
) {
    val isDark = TDTheme.isDark
    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .then(
                if (isDark) {
                    Modifier.border(1.dp, TDTheme.colors.pendingGray.copy(alpha = 0.35f), TDTheme.shapes.large)
                } else {
                    Modifier.tdShadow(
                        lightShadow = TDTheme.colors.white.copy(alpha = 0.9f),
                        darkShadow = TDTheme.colors.darkPending.copy(alpha = 0.15f),
                        cornerRadius = 16.dp,
                        elevation = 8.dp,
                    )
                },
            ).clip(TDTheme.shapes.large)
            .background(TDTheme.colors.background)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = tdPainter(UiKitR.drawable.ic_sand_clock),
                contentDescription = null,
                tint = TDTheme.colors.darkPending,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            TDText(
                text = stringResource(R.string.pomodoro_total_min_preview, totalMinutes),
                style = TDTheme.typography.heading4,
                color = TDTheme.colors.darkPending,
            )
        }
        Spacer(Modifier.height(4.dp))
        TDText(
            text = stringResource(R.string.pomodoro_session_breakdown, sessionCount, focusTime, shortBreak, longBreak),
            style = TDTheme.typography.subheading1,
            color = TDTheme.colors.onBackground.copy(alpha = 0.55f),
        )
    }
}

@Composable
private fun PomodoroSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TDText(
            text = title.uppercase(),
            style = TDTheme.typography.subheading2,
            color = TDTheme.colors.onBackground.copy(alpha = 0.45f),
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = TDTheme.colors.onBackground.copy(alpha = 0.1f),
        )
    }
}

@Composable
private fun PomodoroStepperCard(
    value: Int,
    label: String,
    unit: String,
    @DrawableRes iconRes: Int,
    cardBgColor: Color,
    iconTintColor: Color,
    range: IntRange,
    step: Int,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val canDecrement = value > range.first
    val canIncrement = value < range.last
    val isDark = TDTheme.isDark

    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .then(
                if (isDark) {
                    Modifier.border(1.dp, TDTheme.colors.lightGray.copy(alpha = 0.25f), TDTheme.shapes.large)
                } else {
                    Modifier.tdShadow(
                        lightShadow = TDTheme.colors.white.copy(alpha = 0.85f),
                        darkShadow = iconTintColor.copy(alpha = 0.18f),
                        cornerRadius = 16.dp,
                        elevation = 6.dp,
                    )
                },
            ).clip(TDTheme.shapes.large)
            .background(cardBgColor)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = tdPainter(iconRes),
            contentDescription = null,
            tint = iconTintColor,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(10.dp))
        // The label carries the weight, replacing a Spacer that carried it before. Unweighted and
        // measured first, "Odaklanma Süresi" claimed its full ~128dp of a ~296dp card and the
        // stepper behind it — two 36dp buttons, the value and the "dk" unit — was measured against
        // what was left, so the unit was the first thing to be cut. The label is the only part of
        // this row that can afford a second line, so it is the part that should give way.
        TDText(
            modifier = Modifier.weight(1f),
            text = label,
            style = TDTheme.typography.heading6,
            color = TDTheme.colors.onBackground,
        )

        IconButton(
            onClick = {
                val next = (value - step).coerceAtLeast(range.first)
                if (next != value) onValueChange(next.toFloat())
            },
            modifier = Modifier.size(36.dp),
        ) {
            Box(
                modifier =
                Modifier
                    .size(28.dp)
                    .clip(TDTheme.shapes.small)
                    .border(
                        width = 1.dp,
                        color = iconTintColor.copy(alpha = if (canDecrement) 0.5f else 0.2f),
                        shape = TDTheme.shapes.small,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                TDText(
                    text = "\u2212",
                    style = TDTheme.typography.heading5,
                    color = iconTintColor.copy(alpha = if (canDecrement) 1f else 0.3f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        TDText(
            text = "$value",
            style = TDTheme.typography.heading4,
            color = iconTintColor,
            modifier = Modifier.widthIn(min = 52.dp),
            textAlign = TextAlign.Center,
        )

        IconButton(
            onClick = {
                val next = (value + step).coerceAtMost(range.last)
                if (next != value) onValueChange(next.toFloat())
            },
            modifier = Modifier.size(36.dp),
        ) {
            Box(
                modifier =
                Modifier
                    .size(28.dp)
                    .clip(TDTheme.shapes.small)
                    .border(
                        width = 1.dp,
                        color = iconTintColor.copy(alpha = if (canIncrement) 0.5f else 0.2f),
                        shape = TDTheme.shapes.small,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = tdPainter(UiKitR.drawable.ic_plus),
                    contentDescription = null,
                    tint = iconTintColor.copy(alpha = if (canIncrement) 1f else 0.3f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        if (unit.isNotEmpty()) {
            Spacer(Modifier.width(4.dp))
            TDText(
                text = unit,
                style = TDTheme.typography.subheading2,
                color = iconTintColor.copy(alpha = 0.7f),
                modifier = Modifier.widthIn(min = 40.dp),
            )
        }
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun PomodoroStepperCardPreview() {
    TDTheme {
        Column(
            Modifier
                .background(TDTheme.colors.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PomodoroStepperCard(
                value = 25,
                label = stringResource(R.string.pomodoro_focus_time),
                unit = stringResource(R.string.pomodoro_unit_min),
                iconRes = UiKitR.drawable.ic_sand_clock,
                cardBgColor = TDTheme.colors.lightGreen,
                iconTintColor = TDTheme.colors.darkGreen,
                range = 5..120,
                step = 5,
                onValueChange = {},
            )
            PomodoroStepperCard(
                value = 5,
                label = stringResource(R.string.pomodoro_short_break_label),
                unit = stringResource(R.string.pomodoro_unit_min),
                iconRes = UiKitR.drawable.ic_sand_clock,
                cardBgColor = TDTheme.colors.lightRed,
                iconTintColor = TDTheme.colors.crossRed,
                range = 1..30,
                step = 1,
                onValueChange = {},
            )
        }
    }
}

/** The stepper's label competes with two buttons, the value and the unit for one row of a card. */
@com.todoapp.uikit.previews.TDPreviewNarrow
@Composable
private fun PomodoroStepperCardNarrowPreview() {
    TDTheme {
        Column(
            Modifier
                .background(TDTheme.colors.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PomodoroStepperCard(
                value = 120,
                label = stringResource(R.string.pomodoro_focus_time),
                unit = stringResource(R.string.pomodoro_unit_min),
                iconRes = UiKitR.drawable.ic_sand_clock,
                cardBgColor = TDTheme.colors.lightGreen,
                iconTintColor = TDTheme.colors.darkGreen,
                range = 5..120,
                step = 5,
                onValueChange = {},
            )
            PomodoroStepperCard(
                value = 15,
                label = stringResource(R.string.pomodoro_long_break_label),
                unit = stringResource(R.string.pomodoro_unit_min),
                iconRes = UiKitR.drawable.ic_sand_clock,
                cardBgColor = TDTheme.colors.lightRed,
                iconTintColor = TDTheme.colors.crossRed,
                range = 1..60,
                step = 1,
                onValueChange = {},
            )
        }
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun AddPomodoroTimerContentPreview() {
    TDTheme {
        AddPomodoroTimerContent(
            uiState = UiState(),
            onAction = {},
        )
    }
}
