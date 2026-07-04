package com.todoapp.mobile.ui.home

import androidx.compose.runtime.Immutable
import com.todoapp.mobile.domain.model.DayMode
import com.todoapp.mobile.domain.model.Subtask
import com.todoapp.mobile.domain.model.Task
import com.todoapp.mobile.ui.settings.PermissionType
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

object HomeContract {
    @Immutable
    data class GroupSelectionItem(
        val groupId: Long,
        val name: String,
    )

    sealed interface UiState {
        data object Loading : UiState

        @Immutable
        data class Success(
            val selectedDate: LocalDate,
            val displayedMonth: YearMonth = YearMonth.now(),
            val tasks: List<Task>,
            val completedTaskCountThisWeek: Int,
            val pendingTaskCountThisWeek: Int,
            val isSheetOpen: Boolean,
            val isDeleteDialogOpen: Boolean,
            val isFinishRoutineDialogOpen: Boolean = false,
            val isSecretModeEnabled: Boolean,
            val taskFormState: TaskFormState = TaskFormState(),
            val pendingDeleteTask: Task? = null,
            val availableGroups: List<GroupSelectionItem> = emptyList(),
            val pendingPermissions: List<PermissionType> = emptyList(),
            val selectedFilter: HomeFilter = HomeFilter.TODAY,
            val displayName: String = "",
            val dayMode: DayMode = DayMode.MIDDAY,
            val isEndOfDayMoment: Boolean = false,
            val isSuggestCardDismissedToday: Boolean = false,
            val yesterdayCompletedCount: Int = 0,
            val currentTimeFormatted: String = "",
            val lastRecurringFilter: HomeFilter = HomeFilter.DAILY,
            val overdueDates: Set<LocalDate> = emptySet(),
            val hasOverdueBeforeDisplayedMonth: Boolean = false,
            val overdueCount: Int = 0,
            val taskDatesInMonth: Set<LocalDate> = emptySet(),
            val expandedStagedTaskId: Long? = null,
            val expandedSubtasks: List<Subtask> = emptyList(),
            val isRefreshing: Boolean = false,
            val isSignedIn: Boolean = false,
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

        data object OnDialogDateDeselect : UiAction

        data class OnTaskCheck(
            val task: Task,
        ) : UiAction

        data class OnTaskTitleChange(
            val title: String,
        ) : UiAction

        data class OnTaskTimeStartChange(
            val time: LocalTime,
        ) : UiAction

        data class OnTaskTimeEndChange(
            val time: LocalTime,
        ) : UiAction

        data class OnTaskDateChange(
            val date: LocalDate,
        ) : UiAction

        data class OnTaskDescriptionChange(
            val description: String,
        ) : UiAction

        data object OnShowBottomSheet : UiAction

        data object OnDismissBottomSheet : UiAction

        data object OnTaskCreate : UiAction

        data class OnTaskLongPress(
            val task: Task,
        ) : UiAction

        data object OnDeleteDialogDismiss : UiAction

        data object OnDeleteDialogConfirm : UiAction

        data object OnFinishRoutineDialogConfirm : UiAction

        data object OnFinishRoutineDialogDismiss : UiAction

        data object OnRetry : UiAction

        data object OnPullToRefresh : UiAction

        data class OnDialogDateSelect(
            val date: LocalDate,
        ) : UiAction

        data class OnMoveTask(
            val from: Int,
            val to: Int,
        ) : UiAction

        data object OnPomodoroTap : UiAction

        data object OnToggleAdvancedSettings : UiAction

        data class OnTaskSecretChange(
            val isSecret: Boolean,
        ) : UiAction

        data class OnReminderOffsetChange(
            val minutes: Long?,
        ) : UiAction

        data class OnTaskClick(
            val task: Task,
        ) : UiAction

        data object OnSuccessfulBiometricAuthenticationHandle : UiAction

        data class OnToggleTaskSecret(
            val task: Task,
        ) : UiAction

        data class OnBiometricSuccessForSecretToggle(
            val task: Task,
        ) : UiAction

        data object OnUndoDelete : UiAction

        data object OnCompletedStatCardTap : UiAction

        data object OnPendingStatCardTap : UiAction

        data class OnGroupSelectionChanged(
            val groupId: Long?,
        ) : UiAction

        data object OnPreviousMonth : UiAction

        data object OnNextMonth : UiAction

        data class OnPendingPhotoAdd(
            val bytes: ByteArray,
            val mimeType: String,
        ) : UiAction

        data class OnPendingPhotoRemove(
            val index: Int,
        ) : UiAction

        data class DismissPermission(
            val type: PermissionType,
        ) : UiAction

        data class PermissionGranted(
            val type: PermissionType,
        ) : UiAction

        data object RefreshPermissions : UiAction

        data class OnCategoryChange(
            val category: com.todoapp.mobile.domain.model.TaskCategory,
        ) : UiAction

        data class OnCustomCategoryNameChange(
            val name: String,
        ) : UiAction

        data class OnRecurrenceChange(
            val recurrence: com.todoapp.mobile.domain.model.Recurrence,
        ) : UiAction

        data class OnAllDayChange(
            val isAllDay: Boolean,
        ) : UiAction

        data class OnLocationPicked(
            val name: String,
            val address: String,
            val lat: Double?,
            val lng: Double?,
        ) : UiAction

        data object OnLocationCleared : UiAction

        data class OnFilterChange(
            val filter: HomeFilter,
        ) : UiAction

        data object OnSuggestCardPrimaryAction : UiAction

        data object OnSuggestCardSecondaryAction : UiAction

        data object OnSuggestCardDismiss : UiAction

        data object OnTimeTick : UiAction

        data object OnJumpToEarliestOverdue : UiAction

        data object OnJournalTap : UiAction

        data object OnCreateHubTap : UiAction

        data class OnStagedExpandToggle(
            val taskId: Long,
        ) : UiAction

        data class OnSubtaskToggle(
            val subtaskId: Long,
            val isCompleted: Boolean,
        ) : UiAction
    }

    enum class HomeFilter {
        TODAY,
        DAILY,
        WEEKLY,
        MONTHLY,
        YEARLY,
    }

    sealed interface UiEffect {
        data class ShowToast(
            val message: String,
        ) : UiEffect

        data object ShowBiometricAuthenticator : UiEffect

        data class ShowBiometricForSecretToggle(
            val task: Task,
        ) : UiEffect

        data class ShowError(
            val message: String,
        ) : UiEffect
    }
}
