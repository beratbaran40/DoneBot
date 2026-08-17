package com.todoapp.mobile.ui.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.common.focusDuration
import com.todoapp.mobile.ui.activity.ActivityContract.PomodoroMonthStats
import com.todoapp.mobile.ui.activity.ActivityContract.UiAction
import com.todoapp.mobile.ui.activity.ActivityContract.UiState
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.components.TDWeeklyBarChart
import com.todoapp.uikit.theme.TDTheme

/**
 * The month's focus totals, below the category breakdown.
 *
 * Every figure here comes from recorded sessions, so a month before the feature shipped reads as empty
 * rather than as zero effort — which is the honest rendering, and the same distinction the admin panel
 * draws with nulls.
 */
@Composable
internal fun ActivityPomodoroSection(
    state: UiState.Success,
    onAction: (UiAction) -> Unit,
) {
    val stats = state.pomodoro

    ActivityCard {
        TDText(
            text = stringResource(R.string.activity_pomodoro_title),
            style = TDTheme.typography.heading3,
            color = TDTheme.colors.onBackground,
        )

        if (!stats.hasData) {
            PomodoroEmptyState(onAction = onAction)
            return@ActivityCard
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PomodoroStatTile(
                label = stringResource(R.string.activity_pomodoro_focus_time),
                value = focusDuration(LocalContext.current, stats.focusSeconds),
                modifier = Modifier.weight(1f),
            )
            PomodoroStatTile(
                label = stringResource(R.string.activity_pomodoro_sessions),
                value = stats.completedSessions.toString(),
                modifier = Modifier.weight(1f),
            )
            PomodoroStatTile(
                label = stringResource(R.string.activity_pomodoro_best_day),
                value = focusDuration(LocalContext.current, stats.bestDaySeconds),
                modifier = Modifier.weight(1f),
            )
        }

        // Guarded rather than assumed: TDWeeklyBarChart takes an early return when values and days
        // disagree in size, and Modifier.weight(0f) further down would crash on an empty week list.
        if (stats.weekSessionCounts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            TDWeeklyBarChart(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.activity_pomodoro_chart_title),
                // Session COUNTS, never minutes. The component sizes its plot as
                // `maxY * minGridStep` — one gridline per unit — so 300 focus minutes would ask for a
                // 7200dp plot and silently break the scroll. Counts are the magnitude it was built for,
                // and "sessions per week" is the better chart anyway.
                values = stats.weekSessionCounts,
                // Explicit labels sized to the real week count; the default is seven Mon–Sun labels,
                // which would trip the size check on any month that partitions into five weeks.
                days = stats.weekSessionCounts.indices.map {
                    stringResource(R.string.activity_pomodoro_week_label, it + 1)
                },
                height = 160.dp,
                // Never auto-scale here. A heavy user reaching 60 sessions in a week would ask for a
                // 1440dp plot by the same arithmetic.
                autoScaleHeightToMaxY = false,
            )
        }
    }
}

@Composable
private fun PomodoroEmptyState(onAction: (UiAction) -> Unit) {
    Spacer(modifier = Modifier.height(8.dp))
    TDText(
        text = stringResource(R.string.activity_pomodoro_empty),
        style = TDTheme.typography.subheading2,
        color = TDTheme.colors.pendingGray,
    )
    Spacer(modifier = Modifier.height(12.dp))
    // OnPomodoroTap and ActivityViewModel.navigateToPomodoro already existed, fully wired and never
    // dispatched from anywhere. This is the first caller.
    TDButton(
        text = stringResource(R.string.activity_pomodoro_start),
        onClick = { onAction(UiAction.OnPomodoroTap) },
    )
}

/**
 * A local tile rather than a promotion of `PomodoroSummaryScreen.PomodoroStatCard`.
 *
 * Two call sites do not justify moving this into `:uikit`, which would then owe it its own EN+TR
 * strings and previews. If a third appears, promote both to `TDStatTile` and delete the copies.
 */
@Composable
private fun PomodoroStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TDText(
            text = value,
            style = TDTheme.typography.heading3,
            color = TDTheme.colors.onBackground,
        )
        Spacer(modifier = Modifier.height(2.dp))
        TDText(
            text = label,
            style = TDTheme.typography.subheading3,
            color = TDTheme.colors.pendingGray,
        )
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────────────────────────

private fun previewState(pomodoro: PomodoroMonthStats) = UiState.Success(
    selectedMonth = java.time.YearMonth.of(2026, 8),
    monthCompleted = 18,
    monthPending = 6,
    monthlyWeekBuckets = emptyList(),
    pomodoro = pomodoro,
)

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun ActivityPomodoroSectionPopulatedPreview() {
    TDTheme {
        ActivityPomodoroSection(
            state = previewState(
                PomodoroMonthStats(
                    // 3h 20m, and a five-week month — the partition the default seven Mon–Sun labels
                    // would silently break.
                    focusSeconds = 12_000L,
                    completedSessions = 8,
                    weekSessionCounts = listOf(3, 2, 0, 3, 0),
                    bestDaySeconds = 4_500L,
                ),
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun ActivityPomodoroSectionEmptyPreview() {
    TDTheme {
        // What every month before this feature shipped looks like: empty, not zero.
        ActivityPomodoroSection(state = previewState(PomodoroMonthStats()), onAction = {})
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun ActivityPomodoroSectionUnderAnHourPreview() {
    TDTheme {
        // Exercises the other branch of focusDuration — "45m", with no hours part.
        ActivityPomodoroSection(
            state = previewState(
                PomodoroMonthStats(
                    focusSeconds = 2_700L,
                    completedSessions = 2,
                    weekSessionCounts = listOf(2, 0, 0, 0),
                    bestDaySeconds = 1_500L,
                ),
            ),
            onAction = {},
        )
    }
}
