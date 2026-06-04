package com.todoapp.mobile.ui.calendar

import androidx.compose.runtime.Immutable
import com.todoapp.mobile.ui.home.TaskFormState
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

object CalendarContract {
    sealed interface UiState {
        data object Loading : UiState

        @Immutable
        data class Success(
            val selectedDate: LocalDate? = null,
            val selectedMonth: YearMonth = YearMonth.now(),
            val taskDatesInMonth: Set<LocalDate> = emptySet(),
            val personalTaskItems: List<PersonalTaskCalendarItem> = emptyList(),
            val groupTaskItems: List<GroupTaskCalendarItem> = emptyList(),
            val viewerPhotoUrl: String? = null,
            val isSheetOpen: Boolean = false,
            val taskFormState: TaskFormState = TaskFormState(),
            val overdueDates: Set<LocalDate> = emptySet(),
            val hasOverdueBeforeDisplayedMonth: Boolean = false,
            val overdueCount: Int = 0,
            val isRefreshing: Boolean = false,
        ) : UiState

        data class Error(
            val message: String,
            val throwable: Throwable? = null,
        ) : UiState
    }

    sealed interface UiAction {
        data class OnDateSelect(
            val date: LocalDate,
        ) : UiAction

        data object OnDateDeselect : UiAction

        data object OnMonthForward : UiAction

        data object OnMonthBack : UiAction

        data object OnRetry : UiAction

        data class OnTaskClick(
            val taskId: Long,
        ) : UiAction

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

        data object OnSuccessfulBiometricAuthenticationHandle : UiAction

        data class OnGroupTaskPhotoOpen(
            val url: String,
        ) : UiAction

        data object OnGroupTaskPhotoDismiss : UiAction

        data class OnGroupTaskClick(
            val groupId: Long,
            val taskId: Long,
        ) : UiAction

        data object OnJumpToEarliestOverdue : UiAction

        data object OnJournalTap : UiAction

        data object OnRefresh : UiAction
    }

    @Immutable
    data class PersonalTaskCalendarItem(
        val taskId: Long,
        val title: String,
        val description: String?,
        val dueAtEpochMs: Long,
        val isCompleted: Boolean,
        val photoUrl: String?,
        val isRecurringInstance: Boolean = false,
        val locationName: String? = null,
        val locationAddress: String? = null,
        val locationLat: Double? = null,
        val locationLng: Double? = null,
    )

    @Immutable
    data class GroupTaskCalendarItem(
        val taskId: Long,
        val groupId: Long?,
        val title: String,
        val priority: String?,
        val dueAtEpochMs: Long,
        val assigneeName: String?,
        val assigneeAvatarUrl: String?,
        val assigneeInitials: String,
        val photoUrl: String?,
        val isCompleted: Boolean,
    )

    sealed interface UiEffect {
        data object ShowBiometricAuthenticator : UiEffect
    }
}
