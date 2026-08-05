package com.todoapp.mobile.ui.details

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.TaskCategory
import com.todoapp.mobile.ui.common.taskform.TaskCapabilities
import com.todoapp.mobile.ui.common.taskform.TaskFormType
import com.todoapp.mobile.ui.home.PendingPhoto
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

object DetailsContract {
    sealed interface UiState {
        data object Loading : UiState

        @Immutable
        data class Success(
            val isDirty: Boolean,
            val isSaving: Boolean,
            val taskId: Long,
            val taskTitle: String,
            val taskTimeStart: LocalTime?,
            val taskTimeEnd: LocalTime?,
            val taskDate: LocalDate,
            val taskDescription: String,
            val dialogSelectedDate: LocalDate?,
            @StringRes val titleError: Int?,
            val photoUrls: List<String> = emptyList(),
            val locationName: String? = null,
            val locationAddress: String? = null,
            val locationLat: Double? = null,
            val locationLng: Double? = null,
            val selectedCategory: TaskCategory = TaskCategory.PERSONAL,
            val customCategoryName: String = "",
            val selectedRecurrence: Recurrence = Recurrence.NONE,
            val reminderOffsetMinutes: Long? = 0L,
            val isAllDay: Boolean = false,
            val isReminderInPast: Boolean = false,
            // Whole-task completion. Shown/toggled only for one-time tasks; routines complete
            // per-day from the list surfaces and staged tasks derive completion from their steps.
            val isCompleted: Boolean = false,
            // Staged photo uploads not yet on the server. Drained on Save.
            val pendingPhotoUploads: List<PendingPhoto> = emptyList(),
            // Existing photoIds that the user marked for deletion. Drained on Save.
            val pendingPhotoDeleteIds: Set<Long> = emptySet(),
            // Type is fixed at creation; derived from the loaded task and shown read-only.
            val taskType: TaskFormType = TaskFormType.ONE_TIME,
            // What this task can do. Field visibility asks these, not the type — a custom task shows
            // whichever sections its capabilities enable, without every gate growing a CUSTOM arm.
            val capabilities: TaskCapabilities = TaskCapabilities(
                recurs = false,
                hasSteps = false,
                hasMultipleReminders = false,
            ),
            // Extra reminder times of a multi-reminder task; empty = the single offset reminder.
            val reminderTimes: List<LocalTime> = emptyList(),
            // Scheduled end of a bounded routine, and how far along it is ("day 12 of 30").
            val recurrenceUntil: LocalDate? = null,
            val recurrenceInterval: Int = 1,
            val recurrenceByDay: Set<DayOfWeek> = emptySet(),
            val routineDayIndex: Int? = null,
            val routineDayTotal: Int? = null,
            // Editable steps for a staged task (rename/toggle/add/remove). Reconciled on Save.
            val subtaskDrafts: List<SubtaskDraft> = emptyList(),
            val showDiscardDialog: Boolean = false,
        ) : UiState

        data class Error(
            val message: String,
            val throwable: Throwable? = null,
        ) : UiState
    }

    sealed interface UiAction {
        data object OnBackClick : UiAction

        data object OnCancelClick : UiAction

        data class OnTaskTitleEdit(
            val title: String,
        ) : UiAction

        data class OnTaskTimeStartEdit(
            val time: LocalTime,
        ) : UiAction

        data class OnTaskTimeEndEdit(
            val time: LocalTime,
        ) : UiAction

        data class OnTaskDateEdit(
            val date: LocalDate,
        ) : UiAction

        data class OnTaskDescriptionEdit(
            val description: String,
        ) : UiAction

        data class OnDialogDateSelect(
            val date: LocalDate,
        ) : UiAction

        data object OnDialogDateDeselect : UiAction

        data object OnSaveChanges : UiAction

        data object OnToggleComplete : UiAction

        data object OnRetry : UiAction

        data class OnPhotoPicked(
            val bytes: ByteArray,
            val mimeType: String,
        ) : UiAction

        data class OnPhotoDelete(
            val photoId: Long,
        ) : UiAction

        data class OnPendingPhotoCancel(
            val index: Int,
        ) : UiAction

        data class OnLocationPicked(
            val name: String,
            val address: String,
            val lat: Double?,
            val lng: Double?,
        ) : UiAction

        data object OnLocationCleared : UiAction

        data class OnCategoryChange(
            val category: TaskCategory,
        ) : UiAction

        data class OnCustomCategoryNameChange(
            val name: String,
        ) : UiAction

        data class OnRecurrenceChange(
            val recurrence: Recurrence,
        ) : UiAction

        data class OnReminderOffsetChange(
            val minutes: Long?,
        ) : UiAction

        data class OnReminderTimeAdd(
            val time: LocalTime,
        ) : UiAction

        data class OnReminderTimeRemove(
            val time: LocalTime,
        ) : UiAction

        /** null clears the scheduled end, making the routine open-ended again. */
        data class OnRecurrenceUntilChange(
            val until: LocalDate?,
        ) : UiAction

        data class OnIntervalChange(
            val interval: Int,
        ) : UiAction

        data class OnWeekdayToggle(
            val day: DayOfWeek,
        ) : UiAction

        data class OnAllDayChange(
            val isAllDay: Boolean,
        ) : UiAction

        data object OnConfirmDiscard : UiAction

        data object OnDismissDiscardDialog : UiAction

        data class OnSubtaskTitleChange(
            val index: Int,
            val title: String,
        ) : UiAction

        data class OnSubtaskToggle(
            val index: Int,
        ) : UiAction

        data class OnSubtaskRemove(
            val index: Int,
        ) : UiAction
    }

    sealed interface UiEffect {
        data class ShowToast(
            @StringRes val message: Int,
        ) : UiEffect
    }
}
