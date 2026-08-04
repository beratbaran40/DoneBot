package com.todoapp.mobile.ui.groups.grouptaskdetail

import androidx.compose.runtime.Immutable
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.Subtask
import com.todoapp.mobile.domain.model.TaskCategory
import com.todoapp.mobile.ui.groups.groupdetail.GroupDetailContract
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

object GroupTaskDetailContract {
    @Immutable
    data class TaskUiModel(
        val id: Long,
        val title: String,
        val description: String?,
        val priority: String?,
        val dueTime: String?,
        val rawDueDate: Long?,
        val isAllDay: Boolean = false,
        val timeStart: LocalTime? = null,
        val timeEnd: LocalTime? = null,
        val isCompleted: Boolean,
        val assigneeName: String?,
        val assigneeInitials: String?,
        val assigneeAvatarUrl: String?,
        val assigneeUserId: Long?,
        val isAssignedToMe: Boolean,
        val canDelete: Boolean,
        val canComplete: Boolean = false,
        val photoUrls: List<String> = emptyList(),
        val locationName: String? = null,
        val locationAddress: String? = null,
        val locationLat: Double? = null,
        val locationLng: Double? = null,
        /** The rule as raw fields — the chip needs string resources, so it renders in the screen. */
        val category: TaskCategory = TaskCategory.PERSONAL,
        val customCategoryName: String? = null,
        val recurrence: Recurrence = Recurrence.NONE,
        val recurrenceInterval: Int = 1,
        val recurrenceByDay: Set<DayOfWeek> = emptySet(),
        val subtasks: List<Subtask> = emptyList(),
        /** For a bounded routine: "Day 12 of 30". Null when the routine is open-ended or one-off. */
        val routineDayIndex: Int? = null,
        val routineDayTotal: Int? = null,
    )

    sealed interface UiState {
        data object Loading : UiState

        @Immutable
        data class Success(
            val task: TaskUiModel,
            val groupName: String,
            val members: List<GroupDetailContract.GroupMemberUiItem> = emptyList(),
            val isEditSheetOpen: Boolean = false,
            val editTitle: String = "",
            val editDescription: String = "",
            val editDate: LocalDate? = null,
            val editIsAllDay: Boolean = false,
            val editTimeStart: LocalTime? = null,
            val editTimeEnd: LocalTime? = null,
            val editAssigneeId: Long? = null,
            val isSaving: Boolean = false,
            val editLocationName: String? = null,
            val editLocationAddress: String? = null,
            val editLocationLat: Double? = null,
            val editLocationLng: Double? = null,
        ) : UiState

        data class Error(
            val message: String,
        ) : UiState
    }

    sealed interface UiAction {
        data object OnBackTap : UiAction

        data object OnToggleComplete : UiAction

        /** Ticks one step of a staged group task. Shared with the group, like everything else here. */
        data class OnSubtaskToggle(val subtaskId: Long, val isCompleted: Boolean) : UiAction

        data object OnEditTap : UiAction

        data object OnEditDismiss : UiAction

        data object OnEditSave : UiAction

        data class OnEditTitleChange(
            val title: String,
        ) : UiAction

        data class OnEditDescriptionChange(
            val description: String,
        ) : UiAction

        data class OnEditDateSelect(
            val date: LocalDate,
        ) : UiAction

        data object OnEditDateDeselect : UiAction

        data class OnEditAllDayChange(
            val isAllDay: Boolean,
        ) : UiAction

        data class OnEditTimeStartChange(
            val time: LocalTime,
        ) : UiAction

        data class OnEditTimeEndChange(
            val time: LocalTime,
        ) : UiAction

        data class OnEditAssigneeChange(
            val userId: Long?,
        ) : UiAction

        data class OnPhotoPicked(
            val bytes: ByteArray,
            val mimeType: String,
        ) : UiAction

        data class OnPhotoDelete(
            val photoId: Long,
        ) : UiAction

        data class OnPhotoReport(
            val relativeUrl: String,
        ) : UiAction

        data class OnEditLocationPicked(
            val name: String,
            val address: String,
            val lat: Double?,
            val lng: Double?,
        ) : UiAction

        data object OnEditLocationCleared : UiAction
    }

    sealed interface UiEffect {
        data class ShowToast(
            val message: String,
        ) : UiEffect
    }
}
