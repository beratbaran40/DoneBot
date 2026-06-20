package com.todoapp.mobile.ui.activity

import androidx.compose.runtime.Immutable
import com.todoapp.mobile.domain.model.TaskCategory
import com.todoapp.mobile.domain.repository.DailyBucket
import com.todoapp.mobile.domain.repository.MonthlyWeekBucket
import com.todoapp.mobile.ui.home.TaskFormState
import java.time.LocalDate
import java.time.LocalTime
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
            val streakDays: Int = 0,
            val bestDay: BestDay? = null,
            val categoryBreakdown: List<CategoryStat> = emptyList(),
            val heatmapData: Map<LocalDate, Int> = emptyMap(),
            val yearStripBuckets: List<YearStripMonth> = emptyList(),
            val includeRecurring: Boolean = false,
            val slideDirection: Int = 0,
            val expandedWeekIndex: Int? = null,
            val expandedWeekDays: List<DailyBucket> = emptyList(),
            val yearlyCompleted: Int = 0,
            val yearlyTotal: Int = 0,
            val yearlyProgress: Float = 0f,
            val isSheetOpen: Boolean = false,
            val taskFormState: TaskFormState = TaskFormState(),
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

        data object OnShowBottomSheet : UiAction

        data object OnDismissBottomSheet : UiAction

        data object OnTaskCreate : UiAction

        data class OnTaskTitleChange(
            val title: String,
        ) : UiAction

        data class OnDialogDateSelect(
            val date: LocalDate,
        ) : UiAction

        data object OnDialogDateDeselect : UiAction

        data class OnTaskTimeStartChange(
            val time: LocalTime,
        ) : UiAction

        data class OnTaskTimeEndChange(
            val time: LocalTime,
        ) : UiAction

        data class OnTaskDescriptionChange(
            val description: String,
        ) : UiAction

        data object OnToggleAdvancedSettings : UiAction

        data class OnTaskSecretChange(
            val isSecret: Boolean,
        ) : UiAction

        data object OnPomodoroTap : UiAction

        data object OnCompletedStatCardTap : UiAction

        data object OnPendingStatCardTap : UiAction

        data class OnToggleIncludeRecurring(
            val include: Boolean,
        ) : UiAction

        data object OnViewOverdue : UiAction

        data object OnJournalTap : UiAction

        data object OnCreateHubTap : UiAction
    }

    sealed interface UiEffect
}
