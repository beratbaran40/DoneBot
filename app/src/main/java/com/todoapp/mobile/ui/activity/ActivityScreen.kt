package com.todoapp.mobile.ui.activity

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.mobile.common.heartsLabel
import com.todoapp.mobile.domain.model.TaskCategory
import com.todoapp.mobile.domain.repository.HEART_COUNT
import com.todoapp.mobile.domain.repository.MAX_HALF_HEARTS
import com.todoapp.mobile.ui.activity.ActivityContract.BestDay
import com.todoapp.mobile.ui.activity.ActivityContract.CategoryStat
import com.todoapp.mobile.ui.activity.ActivityContract.MonthTrend
import com.todoapp.mobile.ui.activity.ActivityContract.TrendDirection
import com.todoapp.mobile.ui.activity.ActivityContract.UiAction
import com.todoapp.mobile.ui.activity.ActivityContract.UiState
import com.todoapp.mobile.ui.common.LocalReduceMotion
import com.todoapp.mobile.ui.common.components.OverdueBanner
import com.todoapp.mobile.ui.home.HomeFabMenu
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDButtonSize
import com.todoapp.uikit.components.TDGeneralProgressBar
import com.todoapp.uikit.components.TDHealthBar
import com.todoapp.uikit.components.TDHeartsDepletedDialog
import com.todoapp.uikit.components.TDLoadingBar
import com.todoapp.uikit.components.TDMonthNavigator
import com.todoapp.uikit.components.TDMonthlyBarChart
import com.todoapp.uikit.components.TDSwitch
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.theme.TDTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ActivityScreen(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
) {
    when (uiState) {
        is UiState.Loading -> ActivityLoadingContent()
        is UiState.Error -> ActivityErrorContent(message = uiState.message, onAction = onAction)
        is UiState.Success -> ActivitySuccessContent(uiState = uiState, onAction = onAction)
    }
}

@Composable
private fun ActivityLoadingContent() {
    TDLoadingBar()
}

@Composable
private fun ActivityErrorContent(
    message: String,
    onAction: (UiAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = tdPainter(R.drawable.ic_error),
            contentDescription = null,
            tint = TDTheme.colors.crossRed,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(16.dp))
        TDText(
            text = message,
            style = TDTheme.typography.heading3,
            color = TDTheme.colors.onBackground,
        )
        Spacer(Modifier.height(24.dp))
        TDButton(
            text = stringResource(com.todoapp.mobile.R.string.retry),
            onClick = { onAction(UiAction.OnRetry) },
            size = TDButtonSize.SMALL,
        )
    }
}

@Composable
private fun ActivitySuccessContent(
    uiState: UiState.Success,
    onAction: (UiAction) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            if (uiState.overdueCount > 0) {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    OverdueBanner(
                        count = uiState.overdueCount,
                        onView = { onAction(UiAction.OnViewOverdue) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ActivityCard {
                HealthPointsCard(
                    halfHearts = uiState.healthHalfHearts,
                    animate = !LocalReduceMotion.current,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TDMonthNavigator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                month = uiState.selectedMonth,
                onPreviousMonth = {
                    onAction(UiAction.OnMonthSelected(uiState.selectedMonth.minusMonths(1)))
                },
                onNextMonth = {
                    onAction(UiAction.OnMonthSelected(uiState.selectedMonth.plusMonths(1)))
                },
            )

            IncludeRecurringRow(
                checked = uiState.includeRecurring,
                onCheckedChange = { onAction(UiAction.OnToggleIncludeRecurring(it)) },
            )

            Spacer(modifier = Modifier.height(8.dp))

            ActivityCard {
                if (uiState.expandedWeekIndex == null) {
                    MonthSummaryHeader(
                        monthCompleted = uiState.monthCompleted,
                        monthPending = uiState.monthPending,
                        monthTrend = uiState.monthTrend,
                        bestDay = uiState.bestDay,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                BarChartContent(uiState = uiState, onAction = onAction)
            }

            Spacer(modifier = Modifier.height(12.dp))

            ActivityHeatmapSection(state = uiState, onAction = onAction)

            Spacer(modifier = Modifier.height(12.dp))

            ActivityCard {
                ActivityYearStrip(
                    selectedMonth = uiState.selectedMonth,
                    buckets = uiState.yearStripBuckets.map { it.month to it.totalCompleted },
                    onMonthClick = { month -> onAction(UiAction.OnMonthSelected(month)) },
                )
            }

            if (uiState.categoryBreakdown.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                ActivityCard {
                    CategoryBreakdownSection(stats = uiState.categoryBreakdown)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ActivityCard {
                YearlyProgressSection(
                    yearlyCompleted = uiState.yearlyCompleted,
                    yearlyTotal = uiState.yearlyTotal,
                    yearlyProgress = uiState.yearlyProgress,
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
        HomeFabMenu(
            onCreate = { onAction(UiAction.OnCreateHubTap) },
        )

        if (uiState.showDepletionDialog) {
            TDHeartsDepletedDialog(
                speechBubbleText = stringResource(com.todoapp.mobile.R.string.activity_hearts_depleted_speech),
                buttonText = stringResource(com.todoapp.mobile.R.string.activity_hearts_depleted_button),
                reduceMotion = LocalReduceMotion.current,
                onDismiss = { onAction(UiAction.OnHeartsDepletedDialogDismiss) },
            )
        }
    }
}

@Composable
private fun BarChartContent(
    uiState: UiState.Success,
    onAction: (UiAction) -> Unit,
) {
    val monthChartTitle = stringResource(com.todoapp.mobile.R.string.activity_monthly_chart_title)
    val weekChartTitle = stringResource(com.todoapp.mobile.R.string.activity_drill_in_chart_title)
    val locale = currentLocale()

    if (uiState.expandedWeekIndex != null) {
        // Android system back closes the drill-in (matches the in-card back button).
        BackHandler { onAction(UiAction.OnBarChartBack) }
    }

    AnimatedContent(
        targetState = uiState.expandedWeekIndex,
        transitionSpec = {
            // null → Int     : forward (drill-in opens, slides in from the right)
            // Int → null     : backward (drill-in closes, slides in from the left)
            // any other case : month change while drilled state stayed the same — fall back to the
            //                  ViewModel's slide direction (set by selectMonth).
            val direction = when {
                initialState == null && targetState != null -> 1
                initialState != null && targetState == null -> -1
                else -> if (uiState.slideDirection == 0) 1 else uiState.slideDirection
            }
            (slideInHorizontally(animationSpec = tween(SLIDE_DURATION_MS)) { width -> width * direction } + fadeIn(animationSpec = tween(SLIDE_DURATION_MS))) togetherWith (slideOutHorizontally(
                animationSpec = tween(SLIDE_DURATION_MS),
            ) { width -> -width * direction } + fadeOut(animationSpec = tween(SLIDE_DURATION_MS)))
        },
        label = "barChartContent",
    ) { weekIndex ->
        if (weekIndex == null) {
            TDMonthlyBarChart(
                title = monthChartTitle,
                completedValues = uiState.monthlyWeekBuckets.map { it.completed },
                pendingValues = uiState.monthlyWeekBuckets.map { it.pending },
                labels = uiState.monthlyWeekBuckets.map { bucket ->
                    stringResource(com.todoapp.mobile.R.string.bar_label_week, bucket.weekIndex)
                },
                onBarClick = { idx -> onAction(UiAction.OnBarTap(idx + 1)) },
            )
        } else {
            DrillInWeekChart(
                weekIndex = weekIndex,
                days = uiState.expandedWeekDays,
                title = weekChartTitle,
                locale = locale,
                onBack = { onAction(UiAction.OnBarChartBack) },
            )
        }
    }
}

@Composable
private fun DrillInWeekChart(
    weekIndex: Int,
    days: List<com.todoapp.mobile.domain.repository.DailyBucket>,
    title: String,
    locale: Locale,
    onBack: () -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = tdPainter(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(com.todoapp.mobile.R.string.activity_drill_in_back),
                    tint = TDTheme.colors.onBackground,
                )
            }
            TDText(
                text = stringResource(com.todoapp.mobile.R.string.activity_drill_in_week_title, weekIndex),
                style = TDTheme.typography.heading5,
                color = TDTheme.colors.onBackground,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        TDMonthlyBarChart(
            title = title,
            completedValues = days.map { it.completed },
            pendingValues = days.map { it.pending },
            labels = days.map { it.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale) },
        )
    }
}

@Composable
private fun YearlyProgressSection(
    yearlyCompleted: Int,
    yearlyTotal: Int,
    yearlyProgress: Float,
) {
    TDText(
        text = stringResource(com.todoapp.mobile.R.string.activity_screen_progress_text),
        style = TDTheme.typography.heading5,
        color = TDTheme.colors.onBackground,
    )
    Spacer(modifier = Modifier.height(8.dp))
    TDGeneralProgressBar(
        progress = yearlyProgress,
        completedCount = yearlyCompleted,
        totalCount = yearlyTotal,
    )
}

@Composable
private fun IncludeRecurringRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TDText(
            modifier = Modifier.weight(1f),
            text = stringResource(com.todoapp.mobile.R.string.activity_include_recurring),
            style = TDTheme.typography.subheading1,
            color = TDTheme.colors.onBackground,
        )
        TDSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
internal fun ActivityCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(
                color = TDTheme.colors.lightPending,
                shape = TDTheme.shapes.large,
            )
            .padding(16.dp),
        content = content,
    )
}

@Composable
private fun MonthSummaryHeader(
    monthCompleted: Int,
    monthPending: Int,
    monthTrend: MonthTrend?,
    bestDay: BestDay?,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        TDText(
            text = stringResource(
                com.todoapp.mobile.R.string.activity_month_summary,
                monthCompleted,
                monthPending,
            ),
            style = TDTheme.typography.heading5,
            color = TDTheme.colors.onBackground,
        )
        if (monthTrend != null) {
            Spacer(modifier = Modifier.height(2.dp))
            val (textRes, color) = when (monthTrend.direction) {
                TrendDirection.UP -> com.todoapp.mobile.R.string.activity_trend_up_month to TDTheme.colors.darkGreen
                TrendDirection.DOWN -> com.todoapp.mobile.R.string.activity_trend_down_month to TDTheme.colors.crossRed
                TrendDirection.FLAT -> com.todoapp.mobile.R.string.activity_trend_flat_month to TDTheme.colors.gray
            }
            val text = if (monthTrend.direction == TrendDirection.FLAT) {
                stringResource(textRes)
            } else {
                stringResource(textRes, monthTrend.percentDelta)
            }
            TDText(
                text = text,
                style = TDTheme.typography.subheading1,
                color = color,
            )
        }
        if (bestDay != null) {
            Spacer(modifier = Modifier.height(4.dp))
            val locale = currentLocale()
            val formatter = remember(locale) { DateTimeFormatter.ofPattern("MMM d", locale) }
            TDText(
                text = stringResource(
                    com.todoapp.mobile.R.string.activity_best_day_in_month,
                    formatter.format(bestDay.date),
                    bestDay.count,
                ),
                style = TDTheme.typography.subheading1,
                color = TDTheme.colors.onBackground,
            )
        }
    }
}

@Composable
private fun HealthPointsCard(
    halfHearts: Int,
    animate: Boolean,
) {
    val heartsLabel = heartsLabel(halfHearts)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TDText(
                text = stringResource(com.todoapp.mobile.R.string.activity_health_title),
                style = TDTheme.typography.heading5,
                color = TDTheme.colors.onBackground,
            )
            TDText(
                text = stringResource(
                    com.todoapp.mobile.R.string.activity_health_count,
                    heartsLabel,
                    HEART_COUNT,
                ),
                style = TDTheme.typography.subheading1,
                color = TDTheme.colors.heartFull,
            )
        }
        TDHealthBar(
            halfHearts = halfHearts,
            contentDescription = stringResource(
                com.todoapp.mobile.R.string.activity_health_content_description,
                heartsLabel,
                HEART_COUNT,
            ),
            heartCount = HEART_COUNT,
            heartSize = 20.dp,
            animate = animate,
        )
        TDText(
            text = stringResource(healthHintRes(halfHearts)),
            style = TDTheme.typography.subheading2,
            color = TDTheme.colors.gray,
        )
    }
}

private fun healthHintRes(halfHearts: Int): Int = when {
    halfHearts >= MAX_HALF_HEARTS -> com.todoapp.mobile.R.string.activity_health_hint_full
    halfHearts <= LOW_HEALTH_HALF_HEARTS -> com.todoapp.mobile.R.string.activity_health_hint_low
    else -> com.todoapp.mobile.R.string.activity_health_hint_earn
}

@Composable
private fun CategoryBreakdownSection(stats: List<CategoryStat>) {
    val maxCount = stats.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TDText(
            text = stringResource(com.todoapp.mobile.R.string.activity_category_breakdown_title),
            style = TDTheme.typography.heading5,
            color = TDTheme.colors.onBackground,
        )
        stats.forEach { stat ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = tdPainter(categoryIconRes(stat.category)),
                    contentDescription = null,
                    tint = TDTheme.colors.onBackground,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                TDText(
                    modifier = Modifier.width(96.dp),
                    text = stat.customLabel ?: stringResource(categoryLabelRes(stat.category)),
                    style = TDTheme.typography.heading6,
                    color = TDTheme.colors.onBackground,
                )
                Spacer(modifier = Modifier.size(8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .background(
                            color = TDTheme.colors.lightPending,
                            shape = TDTheme.shapes.tiny,
                        ),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(stat.count.toFloat() / maxCount)
                            .height(8.dp)
                            .background(
                                color = TDTheme.colors.mediumGreen,
                                shape = TDTheme.shapes.tiny,
                            ),
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                TDText(
                    text = stat.count.toString(),
                    style = TDTheme.typography.heading6,
                    color = TDTheme.colors.mediumGreen,
                )
            }
        }
    }
}

private fun categoryIconRes(category: TaskCategory): Int = when (category) {
    TaskCategory.PERSONAL -> R.drawable.ic_personal_label
    TaskCategory.SHOPPING -> R.drawable.ic_shopping_label
    TaskCategory.MEDICINE -> R.drawable.ic_medication_label
    TaskCategory.HEALTH -> R.drawable.ic_health_label
    TaskCategory.WORK -> R.drawable.ic_work_label
    TaskCategory.STUDY -> R.drawable.ic_study_label
    TaskCategory.BIRTHDAY -> R.drawable.ic_birthday_label
    TaskCategory.OTHER -> R.drawable.ic_label_label
}

private fun categoryLabelRes(category: TaskCategory): Int = when (category) {
    TaskCategory.PERSONAL -> com.todoapp.mobile.R.string.category_personal
    TaskCategory.SHOPPING -> com.todoapp.mobile.R.string.category_shopping
    TaskCategory.MEDICINE -> com.todoapp.mobile.R.string.category_medicine
    TaskCategory.HEALTH -> com.todoapp.mobile.R.string.category_health
    TaskCategory.WORK -> com.todoapp.mobile.R.string.category_work
    TaskCategory.STUDY -> com.todoapp.mobile.R.string.category_study
    TaskCategory.BIRTHDAY -> com.todoapp.mobile.R.string.category_birthday
    TaskCategory.OTHER -> com.todoapp.mobile.R.string.category_other
}

@Composable
internal fun currentLocale(): Locale {
    val configuration = LocalConfiguration.current
    return if (configuration.locales.isEmpty) Locale.getDefault() else configuration.locales[0]
}

internal fun yearMonthShortLabel(month: YearMonth, locale: Locale): String = month.month.getDisplayName(TextStyle.NARROW, locale)

private const val SLIDE_DURATION_MS = 280
private const val LOW_HEALTH_HALF_HEARTS = 4

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun ActivityLoadingPreview() {
    TDTheme {
        ActivityLoadingContent()
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun ActivityErrorPreview() {
    TDTheme {
        ActivityErrorContent(
            message = "Something went wrong",
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreviewNarrow
@com.todoapp.uikit.previews.TDPreview
@Composable
private fun ActivityScreenSuccessRichPreview() {
    TDTheme {
        ActivitySuccessContent(
            onAction = {},
            uiState = UiState.Success(
                selectedMonth = YearMonth.of(2026, 4),
                monthCompleted = 18,
                monthPending = 6,
                monthlyWeekBuckets = listOf(
                    com.todoapp.mobile.domain.repository.MonthlyWeekBucket(
                        weekIndex = 1,
                        rangeStart = LocalDate.of(2026, 4, 1),
                        rangeEnd = LocalDate.of(2026, 4, 7),
                        completed = 5,
                        pending = 2,
                    ),
                    com.todoapp.mobile.domain.repository.MonthlyWeekBucket(
                        weekIndex = 2,
                        rangeStart = LocalDate.of(2026, 4, 8),
                        rangeEnd = LocalDate.of(2026, 4, 14),
                        completed = 3,
                        pending = 4,
                    ),
                    com.todoapp.mobile.domain.repository.MonthlyWeekBucket(
                        weekIndex = 3,
                        rangeStart = LocalDate.of(2026, 4, 15),
                        rangeEnd = LocalDate.of(2026, 4, 21),
                        completed = 9,
                        pending = 1,
                    ),
                    com.todoapp.mobile.domain.repository.MonthlyWeekBucket(
                        weekIndex = 4,
                        rangeStart = LocalDate.of(2026, 4, 22),
                        rangeEnd = LocalDate.of(2026, 4, 28),
                        completed = 1,
                        pending = 5,
                    ),
                ),
                monthTrend = MonthTrend(TrendDirection.UP, 25),
                healthHalfHearts = 13,
                bestDay = BestDay(LocalDate.of(2026, 4, 12), 8),
                categoryBreakdown = listOf(
                    CategoryStat(TaskCategory.WORK, null, 8),
                    CategoryStat(TaskCategory.HEALTH, null, 5),
                    CategoryStat(TaskCategory.STUDY, null, 3),
                ),
                heatmapData = mapOf(
                    LocalDate.of(2026, 4, 5) to 2,
                    LocalDate.of(2026, 4, 6) to 1,
                    LocalDate.of(2026, 4, 12) to 8,
                    LocalDate.of(2026, 4, 18) to 4,
                ),
                yearStripBuckets = (0..11).map {
                    ActivityContract.YearStripMonth(
                        YearMonth.of(2026, 4).minusMonths((11 - it).toLong()),
                        totalCompleted = it * 2,
                    )
                },
                includeRecurring = true,
            ),
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun ActivityScreenEmptyMonthPreview() {
    TDTheme {
        ActivitySuccessContent(
            onAction = {},
            uiState = UiState.Success(
                selectedMonth = YearMonth.of(2026, 4),
                monthCompleted = 0,
                monthPending = 0,
                healthHalfHearts = 0,
                showDepletionDialog = true,
                monthlyWeekBuckets = listOf(
                    com.todoapp.mobile.domain.repository.MonthlyWeekBucket(
                        weekIndex = 1,
                        rangeStart = LocalDate.of(2026, 4, 1),
                        rangeEnd = LocalDate.of(2026, 4, 7),
                        completed = 0,
                        pending = 0,
                    ),
                    com.todoapp.mobile.domain.repository.MonthlyWeekBucket(
                        weekIndex = 2,
                        rangeStart = LocalDate.of(2026, 4, 8),
                        rangeEnd = LocalDate.of(2026, 4, 14),
                        completed = 0,
                        pending = 0,
                    ),
                    com.todoapp.mobile.domain.repository.MonthlyWeekBucket(
                        weekIndex = 3,
                        rangeStart = LocalDate.of(2026, 4, 15),
                        rangeEnd = LocalDate.of(2026, 4, 21),
                        completed = 0,
                        pending = 0,
                    ),
                    com.todoapp.mobile.domain.repository.MonthlyWeekBucket(
                        weekIndex = 4,
                        rangeStart = LocalDate.of(2026, 4, 22),
                        rangeEnd = LocalDate.of(2026, 4, 28),
                        completed = 0,
                        pending = 0,
                    ),
                ),
                yearStripBuckets = (0..11).map {
                    ActivityContract.YearStripMonth(
                        YearMonth.of(2026, 4).minusMonths((11 - it).toLong()),
                        0,
                    )
                },
            ),
        )
    }
}
