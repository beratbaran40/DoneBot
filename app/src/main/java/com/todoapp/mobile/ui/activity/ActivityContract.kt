package com.todoapp.mobile.ui.activity

import androidx.compose.runtime.Immutable
import com.todoapp.mobile.domain.model.TaskCategory
import com.todoapp.mobile.domain.repository.DailyBucket
import com.todoapp.mobile.domain.repository.MAX_HALF_HEARTS
import com.todoapp.mobile.domain.repository.MonthlyWeekBucket
import java.time.LocalDate
import java.time.YearMonth

object ActivityContract {
    enum class TrendDirection { UP, DOWN, FLAT }

    @Immutable
    data class MonthTrend(
        val direction: TrendDirection,
        val percentDelta: Int,
    )

    @Immutable
    data class BestDay(
        val date: LocalDate,
        val count: Int,
    )

    @Immutable
    data class CategoryStat(
        val category: TaskCategory,
        val customLabel: String?,
        val count: Int,
    )

    /**
     * Focus totals for the selected month.
     *
     * [focusSeconds] sums elapsed time, never planned time — a run abandoned six minutes into
     * twenty-five contributes six minutes, which is the whole reason partial sessions are recorded.
     *
     * [weekSessionCounts] is sized and bucketed from [UiState.Success.monthlyWeekBuckets], not from its
     * own partitioning: two charts on one screen disagreeing about how many weeks a month has is a bug
     * nobody reports, they just stop trusting the screen.
     */
    @Immutable
    data class PomodoroMonthStats(
        val focusSeconds: Long = 0L,
        val completedSessions: Int = 0,
        val weekSessionCounts: List<Int> = emptyList(),
        val bestDaySeconds: Long = 0L,
    ) {
        val hasData: Boolean get() = completedSessions > 0 || focusSeconds > 0L
    }

    @Immutable
    data class YearStripMonth(
        val month: YearMonth,
        val totalCompleted: Int,
    )

    sealed interface UiState {
        data object Loading : UiState

        @Immutable
        data class Success(
            val selectedMonth: YearMonth,
            val monthCompleted: Int,
            val monthPending: Int,
            val monthlyWeekBuckets: List<MonthlyWeekBucket>,
            val monthTrend: MonthTrend? = null,
            val healthHalfHearts: Int = MAX_HALF_HEARTS,
            val showDepletionDialog: Boolean = false,
            val bestDay: BestDay? = null,
            val categoryBreakdown: List<CategoryStat> = emptyList(),
            val heatmapData: Map<LocalDate, Int> = emptyMap(),
            val yearStripBuckets: List<YearStripMonth> = emptyList(),
            val includeRecurring: Boolean = false,
            val slideDirection: Int = 0,
            val expandedWeekIndex: Int? = null,
            val expandedWeekDays: List<DailyBucket> = emptyList(),
            val pomodoro: PomodoroMonthStats = PomodoroMonthStats(),
            val yearlyCompleted: Int = 0,
            val yearlyTotal: Int = 0,
            val yearlyProgress: Float = 0f,
            val overdueCount: Int = 0,
        ) : UiState

        data class Error(
            val message: String,
            val throwable: Throwable? = null,
        ) : UiState
    }

    sealed interface UiAction {
        data object OnRetry : UiAction

        data class OnMonthSelected(
            val month: YearMonth,
        ) : UiAction

        data class OnBarTap(
            val weekIndex: Int,
        ) : UiAction

        data object OnBarChartBack : UiAction

        data object OnPomodoroTap : UiAction

        data object OnCompletedStatCardTap : UiAction

        data object OnPendingStatCardTap : UiAction

        data class OnToggleIncludeRecurring(
            val include: Boolean,
        ) : UiAction

        data object OnViewOverdue : UiAction

        data object OnJournalTap : UiAction

        data object OnCreateHubTap : UiAction

        data object OnHeartsDepletedDialogDismiss : UiAction
    }

    sealed interface UiEffect
}
