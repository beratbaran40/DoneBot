package com.todoapp.mobile.ui.creationhub

import androidx.compose.runtime.Immutable
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.TaskCategory
import com.todoapp.mobile.ui.home.PendingPhoto
import java.time.LocalDate
import java.time.LocalTime

object CreationHubContract {
    enum class Step { HUB_ROOT, TASK_TYPE, TASK_CORE }

    enum class TaskType { ONE_TIME, ROUTINE, STAGED }

    @Immutable
    data class UiState(
        val step: Step = Step.HUB_ROOT,
        val taskType: TaskType? = null,
        val title: String = "",
        val date: LocalDate = LocalDate.now(),
        val reminderOffsetMinutes: Long? = null,
        val recurrence: Recurrence = Recurrence.DAILY,
        // Always carries a trailing empty row so the editor can grow inline.
        val subtaskDrafts: List<String> = listOf(""),
        // Optional "Detaylar" panel (collapsed by default).
        val detailsExpanded: Boolean = false,
        val description: String = "",
        val isAllDay: Boolean = true,
        val timeStart: LocalTime? = null,
        val timeEnd: LocalTime? = null,
        val category: TaskCategory = TaskCategory.PERSONAL,
        val customCategoryName: String = "",
        val isSecret: Boolean = false,
        val pendingPhotos: List<PendingPhoto> = emptyList(),
        val locationName: String? = null,
        val locationAddress: String? = null,
        val locationLat: Double? = null,
        val locationLng: Double? = null,
        val placeholderIndex: Int = 0,
        val titleError: Boolean = false,
        val isSaving: Boolean = false,
    )

    sealed interface UiAction {
        data object OnCreateTaskCardTap : UiAction

        data object OnJournalCardTap : UiAction

        data object OnPomodoroCardTap : UiAction

        data object OnGroupCardTap : UiAction

        data class OnTypeSelect(val type: TaskType) : UiAction

        data object OnBack : UiAction

        data class OnTitleChange(val title: String) : UiAction

        data class OnDateSelect(val date: LocalDate) : UiAction

        data class OnReminderSelect(val minutes: Long?) : UiAction

        data class OnFrequencySelect(val recurrence: Recurrence) : UiAction

        data class OnSubtaskChange(val index: Int, val text: String) : UiAction

        data class OnSubtaskRemove(val index: Int) : UiAction

        data object OnToggleDetails : UiAction

        data class OnDescriptionChange(val text: String) : UiAction

        data class OnAllDayChange(val isAllDay: Boolean) : UiAction

        data class OnTimeStartChange(val time: LocalTime) : UiAction

        data class OnTimeEndChange(val time: LocalTime) : UiAction

        data class OnCategoryChange(val category: TaskCategory) : UiAction

        data class OnCustomCategoryNameChange(val name: String) : UiAction

        data class OnSecretChange(val isSecret: Boolean) : UiAction

        data class OnPhotoPick(val bytes: ByteArray, val mimeType: String) : UiAction

        data class OnPhotoRemove(val index: Int) : UiAction

        data class OnLocationPicked(
            val name: String,
            val address: String,
            val lat: Double?,
            val lng: Double?,
        ) : UiAction

        data object OnLocationCleared : UiAction

        data object OnCreate : UiAction
    }

    sealed interface UiEffect {
        data class ShowToast(val messageRes: Int) : UiEffect
    }
}
