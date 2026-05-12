package com.todoapp.mobile.ui.details

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.TaskCategory
import com.todoapp.mobile.ui.home.PendingPhoto
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
            // Staged photo uploads not yet on the server. Drained on Save.
            val pendingPhotoUploads: List<PendingPhoto> = emptyList(),
            // Existing photoIds that the user marked for deletion. Drained on Save.
            val pendingPhotoDeleteIds: Set<Long> = emptySet(),
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

        data class OnAllDayChange(
            val isAllDay: Boolean,
        ) : UiAction
    }

    sealed interface UiEffect {
        data class ShowToast(
            @StringRes val message: Int,
        ) : UiEffect
    }
}
