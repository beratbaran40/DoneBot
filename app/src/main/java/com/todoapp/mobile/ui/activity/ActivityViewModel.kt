// Detekt mis-flags code after `?: return null` guards as unreachable (false positive in bestDay()).
@file:Suppress("UnreachableCode")

package com.todoapp.mobile.ui.activity

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoapp.mobile.R
import com.todoapp.mobile.common.error.toUserMessage
import com.todoapp.mobile.domain.alarm.AlarmScheduler
import com.todoapp.mobile.domain.alarm.AlarmType
import com.todoapp.mobile.domain.engine.PomodoroEngine
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.Task
import com.todoapp.mobile.domain.model.TaskCategory
import com.todoapp.mobile.domain.model.toAlarmItem
import com.todoapp.mobile.domain.repository.ActivityPreferences
import com.todoapp.mobile.domain.repository.TaskRepository
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
import com.todoapp.mobile.ui.home.TaskFormState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
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
    private val alarmScheduler: AlarmScheduler,
    private val pomodoroEngine: PomodoroEngine,
    private val activityPreferences: ActivityPreferences,
    private val observeOverdueSummary: ObserveOverdueSummaryUseCase,
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

    init {
        viewModelScope.launch {
            observeOverdueSummary(today).collect { summary ->
                overdueCountFlow.value = summary.count
            }
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
        val rangeStreakFlow = taskRepository.observeRange(today.minusDays(STREAK_LOOKBACK_DAYS), today)
        val heatmapMonthFlow = taskRepository.observeCompletedCountsByDateRange(monthStart, monthEnd, includeRecurring)
        val yearStripFlow = taskRepository.observeCompletedCountsByDateRange(yearStart, today, includeRecurring)
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
            rangeStreakFlow,
        ) { buckets, currentCompleted, priorCompleted, rangeMonth, rangeStreak ->
            val pendingMonth = buckets.sumOf { it.pending }
            val trend = computeMonthTrend(currentCompleted, priorCompleted)
            val streak = computeStreakDays(today, rangeStreak)
            val bestDay = computeBestDayInMonth(rangeMonth, includeRecurring)
            val categories = computeCategoryBreakdown(rangeMonth, includeRecurring)

            val current = _uiState.value
            UiState.Success(
                selectedMonth = selectedMonth,
                monthCompleted = currentCompleted,
                monthPending = pendingMonth,
                monthlyWeekBuckets = buckets,
                monthTrend = trend,
                streakDays = streak,
                bestDay = bestDay,
                categoryBreakdown = categories,
                heatmapData = emptyMap(),
                yearStripBuckets = emptyList(),
                includeRecurring = includeRecurring,
                slideDirection = slideDirection,
                expandedWeekIndex = expandedWeekIndex,
                isSheetOpen = if (current is UiState.Success) current.isSheetOpen else false,
                taskFormState = if (current is UiState.Success) current.taskFormState else TaskFormState(),
                overdueCount = overdueCount,
            )
        }

        // Carry yearStart through the lambda so we don't lose it when we close over the captured
        // values; required because Flow's combine signature doesn't propagate the yearStart var.
        return combine(
            baseFlow,
            heatmapMonthFlow,
            yearStripFlow,
            kotlinx.coroutines.flow.combine(ytdCompletedFlow, ytdPendingFlow) { c, p -> c to p },
            expandedDailyFlow,
        ) { state, heatmap, yearCounts, ytdPair, expandedDays ->
            val (ytdCompleted, ytdPending) = ytdPair
            val ytdTotal = ytdCompleted + ytdPending
            val ytdProgress = if (ytdTotal > 0) ytdCompleted.toFloat() / ytdTotal else 0f
            state.copy(
                heatmapData = heatmap,
                yearStripBuckets = computeYearStrip(yearCounts),
                expandedWeekDays = expandedDays,
                yearlyCompleted = ytdCompleted,
                yearlyTotal = ytdTotal,
                yearlyProgress = ytdProgress,
            )
        }
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

    // Walks back day-by-day from `today` looking at task completions. The streak ends at the first
    // day with zero completed tasks. We cap the lookback at STREAK_LOOKBACK_DAYS to bound work.
    private fun computeStreakDays(today: LocalDate, rangeTasks: List<Task>): Int {
        val completedByDay = rangeTasks
            .filter { it.isCompleted }
            .groupingBy { it.date }
            .eachCount()
        var streak = 0
        var cursor = today
        while (streak < STREAK_LOOKBACK_DAYS.toInt()) {
            val count = completedByDay[cursor] ?: 0
            if (count <= 0) break
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
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
            UiAction.OnShowBottomSheet -> updateSuccessState { it.copy(isSheetOpen = true) }
            UiAction.OnDismissBottomSheet -> updateSuccessState { it.copy(isSheetOpen = false) }
            UiAction.OnTaskCreate -> createTask()
            is UiAction.OnTaskTitleChange ->
                updateSuccessState {
                    it.copy(taskFormState = it.taskFormState.copy(taskTitle = action.title))
                }
            is UiAction.OnDialogDateSelect ->
                updateSuccessState {
                    it.copy(taskFormState = it.taskFormState.copy(dialogSelectedDate = action.date))
                }
            UiAction.OnDialogDateDeselect ->
                updateSuccessState {
                    it.copy(taskFormState = it.taskFormState.copy(dialogSelectedDate = null))
                }
            is UiAction.OnTaskTimeStartChange ->
                updateSuccessState {
                    it.copy(taskFormState = it.taskFormState.copy(taskTimeStart = action.time))
                }
            is UiAction.OnTaskTimeEndChange ->
                updateSuccessState {
                    it.copy(taskFormState = it.taskFormState.copy(taskTimeEnd = action.time))
                }
            is UiAction.OnTaskDescriptionChange ->
                updateSuccessState {
                    it.copy(taskFormState = it.taskFormState.copy(taskDescription = action.description))
                }
            UiAction.OnToggleAdvancedSettings ->
                updateSuccessState {
                    it.copy(
                        taskFormState =
                        it.taskFormState.copy(
                            isAdvancedSettingsExpanded = !it.taskFormState.isAdvancedSettingsExpanded,
                        ),
                    )
                }
            is UiAction.OnTaskSecretChange ->
                updateSuccessState {
                    it.copy(taskFormState = it.taskFormState.copy(isTaskSecret = action.isSecret))
                }
            UiAction.OnPomodoroTap -> navigateToPomodoro()
            UiAction.OnCompletedStatCardTap -> navigateToFilteredTasks(isCompleted = true)
            UiAction.OnPendingStatCardTap -> navigateToFilteredTasks(isCompleted = false)
            is UiAction.OnToggleIncludeRecurring ->
                viewModelScope.launch { activityPreferences.setIncludeRecurring(action.include) }
            UiAction.OnViewOverdue -> _navEffect.trySend(NavigationEffect.Navigate(Screen.Calendar))
            UiAction.OnJournalTap -> _navEffect.trySend(NavigationEffect.Navigate(Screen.Journal))
            UiAction.OnCreateHubTap -> _navEffect.trySend(NavigationEffect.Navigate(Screen.CreationHub))
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

    private fun createTask() {
        val currentState = _uiState.value
        if (currentState !is UiState.Success) return
        if (!validateTaskForm(currentState)) return

        viewModelScope.launch {
            val form = currentState.taskFormState
            val task =
                Task(
                    title = form.taskTitle,
                    description = form.taskDescription,
                    date = requireNotNull(form.dialogSelectedDate) {
                        "validateTaskForm should have ensured dialogSelectedDate is non-null"
                    },
                    timeStart = requireNotNull(form.taskTimeStart) {
                        "validateTaskForm should have ensured taskTimeStart is non-null"
                    },
                    timeEnd = requireNotNull(form.taskTimeEnd) {
                        "validateTaskForm should have ensured taskTimeEnd is non-null"
                    },
                    isCompleted = false,
                    isSecret = form.isTaskSecret,
                )
            taskRepository.insert(task)
            scheduleTaskReminders(task)
            updateSuccessState { it.copy(taskFormState = TaskFormState(), isSheetOpen = false) }
        }
    }

    private fun validateTaskForm(state: UiState.Success): Boolean {
        val form = state.taskFormState
        return when {
            form.taskTitle.isBlank() -> {
                showTransientError(R.string.error_task_title_required) { s, v ->
                    s.copy(taskFormState = s.taskFormState.copy(titleErrorRes = v))
                }
                false
            }
            form.dialogSelectedDate == null -> {
                showTransientError(R.string.error_task_date_required) { s, v ->
                    s.copy(taskFormState = s.taskFormState.copy(dateErrorRes = v))
                }
                false
            }
            form.taskTimeStart == null || form.taskTimeEnd == null -> {
                showTransientError(R.string.error_task_time_required) { s, v ->
                    s.copy(taskFormState = s.taskFormState.copy(timeErrorRes = v))
                }
                false
            }
            form.taskTimeStart.isAfter(form.taskTimeEnd) -> {
                showTransientError(R.string.error_task_end_before_start) { s, v ->
                    s.copy(taskFormState = s.taskFormState.copy(timeErrorRes = v))
                }
                false
            }
            else -> true
        }
    }

    private fun scheduleTaskReminders(
        task: Task,
        remindBeforeMinutes: List<Long> = DEFAULT_REMINDER_MINUTES,
    ) {
        remindBeforeMinutes.forEach { minutes ->
            alarmScheduler.schedule(
                task.toAlarmItem(remindBeforeMinutes = minutes),
                type = AlarmType.TASK,
            )
        }
    }

    private fun showTransientError(
        @StringRes errorRes: Int,
        durationMs: Long = ERROR_DURATION_MS,
        setErrorRes: (UiState.Success, Int?) -> UiState.Success,
    ) {
        viewModelScope.launch {
            updateSuccessState { setErrorRes(it, errorRes) }
            delay(durationMs)
            updateSuccessState { setErrorRes(it, null) }
        }
    }

    private inline fun updateSuccessState(crossinline transform: (UiState.Success) -> UiState.Success) {
        _uiState.update { currentState ->
            when (currentState) {
                is UiState.Success -> transform(currentState)
                else -> currentState
            }
        }
    }

    private fun retry() {
        _uiState.value = UiState.Loading
        // Re-emit the current selection to force the combine() to rebuild state.
        selectedMonthFlow.value = selectedMonthFlow.value
    }

    companion object {
        private val DEFAULT_REMINDER_MINUTES = listOf(0L, 1L, 2L, 5L, 10L)
        private const val STREAK_LOOKBACK_DAYS = 60L
        private const val MAX_CATEGORY_ROWS = 4
        private const val YEAR_STRIP_MONTHS = 12
        private const val HUNDRED_PERCENT = 100
        private const val ERROR_DURATION_MS = 2000L
        private const val DAYS_IN_WEEK = 7
    }
}
