// Detekt mis-flags code after `?: return null` guards as unreachable (false positive in bestDay()).
@file:Suppress("UnreachableCode")

package com.todoapp.mobile.ui.activity

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoapp.mobile.common.error.toUserMessage
import com.todoapp.mobile.domain.engine.PomodoroEngine
import com.todoapp.mobile.domain.model.PomodoroDayStat
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.Task
import com.todoapp.mobile.domain.model.TaskCategory
import com.todoapp.mobile.domain.repository.ActivityPreferences
import com.todoapp.mobile.domain.repository.HealthPointsPreferences
import com.todoapp.mobile.domain.repository.MAX_HALF_HEARTS
import com.todoapp.mobile.domain.repository.MonthlyWeekBucket
import com.todoapp.mobile.domain.repository.TaskRepository
import com.todoapp.mobile.domain.usecase.ComputeHealthPointsUseCase
import com.todoapp.mobile.domain.usecase.HealthPoints
import com.todoapp.mobile.domain.usecase.ObserveOverdueSummaryUseCase
import com.todoapp.mobile.navigation.NavigationEffect
import com.todoapp.mobile.navigation.Screen
import com.todoapp.mobile.ui.activity.ActivityContract.BestDay
import com.todoapp.mobile.ui.activity.ActivityContract.CategoryStat
import com.todoapp.mobile.ui.activity.ActivityContract.MonthTrend
import com.todoapp.mobile.ui.activity.ActivityContract.TrendDirection
import com.todoapp.mobile.ui.activity.ActivityContract.UiAction
import com.todoapp.mobile.ui.activity.ActivityContract.UiState
import com.todoapp.mobile.ui.activity.ActivityContract.YearStripMonth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ActivityViewModel
@Inject
constructor(
    private val taskRepository: TaskRepository,
    private val pomodoroEngine: PomodoroEngine,
    private val pomodoroSessionRepository: com.todoapp.mobile.domain.repository.PomodoroSessionRepository,
    private val activityPreferences: ActivityPreferences,
    private val observeOverdueSummary: ObserveOverdueSummaryUseCase,
    private val computeHealthPoints: ComputeHealthPointsUseCase,
    private val healthPointsPreferences: HealthPointsPreferences,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _navEffect = Channel<NavigationEffect>()
    val navEffect = _navEffect.receiveAsFlow()

    // Captured once at init so the lookback windows stay stable across the screen's lifetime; the
    // value cannot silently shift if the device crosses midnight while Activity is open.
    private val today: LocalDate = LocalDate.now()
    private val yearStripStart: YearMonth = YearMonth.from(today).minusMonths(YEAR_STRIP_MONTHS - 1L)

    private val selectedMonthFlow = MutableStateFlow(YearMonth.from(today))
    private val slideDirectionFlow = MutableStateFlow(0)
    private val expandedWeekFlow = MutableStateFlow<Int?>(null)
    private val overdueCountFlow = MutableStateFlow(0)
    private val healthFlow = MutableStateFlow(HealthPoints(halfHearts = MAX_HALF_HEARTS, showDepletionDialog = false))

    init {
        viewModelScope.launch {
            observeOverdueSummary(today).collect { summary ->
                overdueCountFlow.value = summary.count
            }
        }
        viewModelScope.launch {
            computeHealthPoints().collect { healthFlow.value = it }
        }
        viewModelScope.launch {
            combine(
                selectedMonthFlow,
                activityPreferences.observeIncludeRecurring(),
                slideDirectionFlow,
                expandedWeekFlow,
                overdueCountFlow,
            ) { month, include, direction, expanded, overdueCount ->
                StateInputs(month, include, direction, expanded, overdueCount)
            }
                .flatMapLatest { inputs ->
                    buildSuccessState(
                        inputs.month,
                        inputs.include,
                        inputs.direction,
                        inputs.expandedWeek,
                        inputs.overdueCount,
                    )
                }
                .catch { e -> _uiState.value = UiState.Error(e.toUserMessage(context), e) }
                .collect { _uiState.value = it }
        }
    }

    private data class StateInputs(
        val month: YearMonth,
        val include: Boolean,
        val direction: Int,
        val expandedWeek: Int?,
        val overdueCount: Int,
    )

    private fun buildSuccessState(
        selectedMonth: YearMonth,
        includeRecurring: Boolean,
        slideDirection: Int,
        expandedWeekIndex: Int?,
        overdueCount: Int,
    ): Flow<UiState.Success> {
        val monthStart = selectedMonth.atDay(1)
        val monthEnd = selectedMonth.atEndOfMonth()
        val priorMonthStart = selectedMonth.minusMonths(1).atDay(1)
        val yearStart = yearStripStart.atDay(1)

        val monthBucketsFlow = taskRepository.observeMonthlyWeekBuckets(monthStart, includeRecurring)
        val currentCountFlow = taskRepository.countCompletedTasksInAMonth(monthStart, includeRecurring)
        val priorCountFlow = taskRepository.countCompletedTasksInAMonth(priorMonthStart, includeRecurring)
        val rangeMonthFlow = taskRepository.observeRange(monthStart, monthEnd)
        val heatmapMonthFlow = taskRepository.observeCompletedCountsByDateRange(monthStart, monthEnd, includeRecurring)
        val yearStripFlow = taskRepository.observeCompletedCountsByDateRange(yearStart, today, includeRecurring)
        // Lives inside buildSuccessState so month navigation re-subscribes it for free, exactly like the
        // task flows above.
        val pomodoroFlow = pomodoroSessionRepository.observeFocusByDay(monthStart, monthEnd)
        val ytdCompletedFlow = taskRepository.countCompletedTasksYearToDate(today)
        val ytdPendingFlow = taskRepository.observePendingTasksYearToDate(today)
        // Drill-in: derive the week's date range from the calendar partition (1-7, 8-14, ...).
        val expandedDailyFlow: Flow<List<com.todoapp.mobile.domain.repository.DailyBucket>> =
            if (expandedWeekIndex == null) {
                kotlinx.coroutines.flow.flowOf(emptyList())
            } else {
                val totalDays = monthStart.lengthOfMonth()
                val rangeStartDay = ((expandedWeekIndex - 1) * DAYS_IN_WEEK + 1).coerceAtMost(totalDays)
                val rangeEndDay = (expandedWeekIndex * DAYS_IN_WEEK).coerceAtMost(totalDays)
                val rangeStart = monthStart.withDayOfMonth(rangeStartDay)
                val rangeEnd = monthStart.withDayOfMonth(rangeEndDay)
                taskRepository.observeDailyBucketsByDateRange(rangeStart, rangeEnd, includeRecurring)
            }

        // Combine the per-area flows in stages: a base flow produces the bulk of state, then the
        // heatmap + year-strip + drill-in + YTD merge in via downstream combines (5-arity max).
        val baseFlow = combine(
            monthBucketsFlow,
            currentCountFlow,
            priorCountFlow,
            rangeMonthFlow,
        ) { buckets, currentCompleted, priorCompleted, rangeMonth ->
            val pendingMonth = buckets.sumOf { it.pending }
            val trend = computeMonthTrend(currentCompleted, priorCompleted)
            val bestDay = computeBestDayInMonth(rangeMonth, includeRecurring)
            val categories = computeCategoryBreakdown(rangeMonth, includeRecurring)

            val current = _uiState.value
            UiState.Success(
                selectedMonth = selectedMonth,
                monthCompleted = currentCompleted,
                monthPending = pendingMonth,
                monthlyWeekBuckets = buckets,
                monthTrend = trend,
                bestDay = bestDay,
                categoryBreakdown = categories,
                heatmapData = emptyMap(),
                yearStripBuckets = emptyList(),
                includeRecurring = includeRecurring,
                slideDirection = slideDirection,
                expandedWeekIndex = expandedWeekIndex,
                overdueCount = overdueCount,
            )
        }

        // Carry yearStart through the lambda so we don't lose it when we close over the captured
        // values; required because Flow's combine signature doesn't propagate the yearStart var.
        return combine(
            baseFlow,
            heatmapMonthFlow,
            yearStripFlow,
            // A named class rather than a fourth stage. The outer combine is at its 5-arity ceiling, and
            // widening this inner one keeps pomodoro on the same stage — a third stage would re-emit the
            // whole state graph on every pomodoro DB tick. It also retires a Triple destructuring.
            kotlinx.coroutines.flow.combine(
                ytdCompletedFlow,
                ytdPendingFlow,
                healthFlow,
                pomodoroFlow,
            ) { completed, pending, health, pomodoroDays ->
                SideInputs(completed, pending, health, pomodoroDays)
            },
            expandedDailyFlow,
        ) { state, heatmap, yearCounts, side, expandedDays ->
            // Read by name, not destructured. Detekt caps destructuring at three entries, and naming
            // this class was the point anyway — positional reads are exactly what it replaced.
            val ytdTotal = side.ytdCompleted + side.ytdPending
            val ytdProgress = if (ytdTotal > 0) side.ytdCompleted.toFloat() / ytdTotal else 0f
            state.copy(
                heatmapData = heatmap,
                yearStripBuckets = computeYearStrip(yearCounts),
                expandedWeekDays = expandedDays,
                yearlyCompleted = side.ytdCompleted,
                yearlyTotal = ytdTotal,
                yearlyProgress = ytdProgress,
                healthHalfHearts = side.health.halfHearts,
                showDepletionDialog = side.health.showDepletionDialog,
                // Bucketed against the state's own week partition, so the two charts on this screen can
                // never disagree about how many weeks the month has.
                pomodoro = toMonthStats(side.pomodoroDays, state.monthlyWeekBuckets),
            )
        }
    }

    /** The fourth slot of the second stage, named so nobody destructures a Triple by position again. */
    private data class SideInputs(
        val ytdCompleted: Int,
        val ytdPending: Int,
        val health: HealthPoints,
        val pomodoroDays: List<PomodoroDayStat>,
    )

    /**
     * Folds per-day focus rows into the month figures the screen renders.
     *
     * Days outside the month's buckets are dropped rather than clamped into the first or last week —
     * they can only come from a range mismatch, and silently folding them in would make a wrong total
     * look plausible.
     */
    private fun toMonthStats(
        days: List<PomodoroDayStat>,
        buckets: List<MonthlyWeekBucket>,
    ): ActivityContract.PomodoroMonthStats {
        val weekCounts = IntArray(buckets.size)
        days.forEach { day ->
            val date = LocalDate.ofEpochDay(day.date)
            val index = buckets.indexOfFirst { !date.isBefore(it.rangeStart) && !date.isAfter(it.rangeEnd) }
            if (index >= 0) weekCounts[index] += day.completedSessions
        }
        return ActivityContract.PomodoroMonthStats(
            focusSeconds = days.sumOf { it.focusSeconds },
            completedSessions = days.sumOf { it.completedSessions },
            // Correctly sized even when empty, which is what keeps TDWeeklyBarChart from taking its
            // "values.size != days.size" early return on a month with no sessions.
            weekSessionCounts = weekCounts.toList(),
            bestDaySeconds = days.maxOfOrNull { it.focusSeconds } ?: 0L,
        )
    }

    private fun computeMonthTrend(current: Int, prior: Int): MonthTrend? {
        if (current == 0 && prior == 0) return null
        if (prior == 0) return MonthTrend(TrendDirection.UP, percentDelta = HUNDRED_PERCENT)
        val delta = ((current - prior).toFloat() / prior * HUNDRED_PERCENT).roundToInt()
        val direction = when {
            delta > 0 -> TrendDirection.UP
            delta < 0 -> TrendDirection.DOWN
            else -> TrendDirection.FLAT
        }
        return MonthTrend(direction, percentDelta = abs(delta))
    }

    private fun computeBestDayInMonth(
        monthTasks: List<Task>,
        includeRecurring: Boolean,
    ): BestDay? {
        val pool = if (includeRecurring) monthTasks else monthTasks.filter { it.recurrence == Recurrence.NONE }
        val completedByDate = pool
            .filter { it.isCompleted }
            .groupingBy { it.date }
            .eachCount()
        val best = completedByDate.maxByOrNull { it.value } ?: return null
        if (best.value <= 0) return null
        return BestDay(date = best.key, count = best.value)
    }

    private fun computeCategoryBreakdown(
        monthTasks: List<Task>,
        includeRecurring: Boolean,
    ): List<CategoryStat> {
        val pool = if (includeRecurring) monthTasks else monthTasks.filter { it.recurrence == Recurrence.NONE }
        val completed = pool.filter { it.isCompleted }
        if (completed.isEmpty()) return emptyList()
        return completed
            .groupBy { it.category to (if (it.category == TaskCategory.OTHER) it.customCategoryName else null) }
            .map { (key, tasks) -> CategoryStat(key.first, key.second?.takeIf { it.isNotBlank() }, tasks.size) }
            .sortedByDescending { it.count }
            .take(MAX_CATEGORY_ROWS)
    }

    private fun computeYearStrip(countsByDate: Map<LocalDate, Int>): List<YearStripMonth> {
        val totalsByMonth = countsByDate.entries.groupBy({ YearMonth.from(it.key) }, { it.value })
        return (0 until YEAR_STRIP_MONTHS).map { offset ->
            val month = yearStripStart.plusMonths(offset.toLong())
            val total = totalsByMonth[month]?.sum() ?: 0
            YearStripMonth(month = month, totalCompleted = total)
        }
    }

    fun onAction(action: UiAction) {
        when (action) {
            UiAction.OnRetry -> retry()
            is UiAction.OnMonthSelected -> selectMonth(action.month)
            is UiAction.OnBarTap -> expandedWeekFlow.value = action.weekIndex
            UiAction.OnBarChartBack -> expandedWeekFlow.value = null
            UiAction.OnPomodoroTap -> navigateToPomodoro()
            UiAction.OnCompletedStatCardTap -> navigateToFilteredTasks(isCompleted = true)
            UiAction.OnPendingStatCardTap -> navigateToFilteredTasks(isCompleted = false)
            is UiAction.OnToggleIncludeRecurring ->
                viewModelScope.launch { activityPreferences.setIncludeRecurring(action.include) }
            UiAction.OnViewOverdue -> _navEffect.trySend(NavigationEffect.Navigate(Screen.Calendar))
            UiAction.OnJournalTap -> _navEffect.trySend(NavigationEffect.Navigate(Screen.Journal))
            UiAction.OnCreateHubTap -> _navEffect.trySend(NavigationEffect.Navigate(Screen.CreationHub))
            UiAction.OnHeartsDepletedDialogDismiss ->
                viewModelScope.launch { healthPointsPreferences.setDialogShown(true) }
        }
    }

    private fun selectMonth(month: YearMonth) {
        val current = selectedMonthFlow.value
        slideDirectionFlow.value = when {
            month > current -> 1
            month < current -> -1
            else -> 0
        }
        selectedMonthFlow.value = month
        // Drop any drill-in: a stale week index from the prior month would otherwise show invalid
        // ranges (e.g. W5 when the new month only has 4 weeks).
        expandedWeekFlow.value = null
    }

    private fun navigateToFilteredTasks(isCompleted: Boolean) {
        // FilteredTasks expects a single anchor day; use the first day of the selected month.
        val anchor = selectedMonthFlow.value.atDay(1)
        _navEffect.trySend(NavigationEffect.Navigate(Screen.FilteredTasks(isCompleted, anchor.toEpochDay())))
    }

    private fun navigateToPomodoro() {
        if (pomodoroEngine.state.value.isRunning) {
            _navEffect.trySend(NavigationEffect.Navigate(Screen.Pomodoro))
        } else {
            _navEffect.trySend(NavigationEffect.Navigate(Screen.AddPomodoroTimer))
        }
    }

    private fun retry() {
        _uiState.value = UiState.Loading
        // Re-emit the current selection to force the combine() to rebuild state.
        selectedMonthFlow.value = selectedMonthFlow.value
    }

    companion object {
        private const val MAX_CATEGORY_ROWS = 4
        private const val YEAR_STRIP_MONTHS = 12
        private const val HUNDRED_PERCENT = 100
        private const val DAYS_IN_WEEK = 7
    }
}
