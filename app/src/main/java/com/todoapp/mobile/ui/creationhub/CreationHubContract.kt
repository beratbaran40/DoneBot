package com.todoapp.mobile.ui.creationhub

import androidx.compose.runtime.Immutable
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.TaskCategory
import com.todoapp.mobile.ui.home.PendingPhoto
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

object CreationHubContract {
    enum class Step { HUB_ROOT, TASK_TYPE, TASK_CORE }

    enum class TaskType { ONE_TIME, ROUTINE, STAGED, GROUP, CUSTOM }

    /** A group the current user administers — carries both ids: local for members, remote for create. */
    @Immutable
    data class GroupOption(val localId: Long, val remoteId: Long, val name: String)

    /** A group member shown in the assignee picker. */
    @Immutable
    data class AssigneeOption(
        val userId: Long,
        val displayName: String,
        val avatarUrl: String?,
        val initials: String,
    )

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
        // A custom task shows every section at once and derives its capabilities from what the user
        // actually fills in — a repeat is "recurrence != NONE", steps are "a step was typed". There
        // are deliberately no capability toggles: an extra up-front choice bought nothing, since the
        // form itself already asks the same questions.
        val recurrenceUntil: LocalDate? = null,
        val reminderTimes: List<LocalTime> = emptyList(),
        val recurrenceInterval: Int = 1,
        /** WEEKLY only; empty = the start date's own weekday (legacy behaviour). */
        val recurrenceByDay: Set<DayOfWeek> = emptySet(),
        val titleError: Boolean = false,
        val isSaving: Boolean = false,
        // Idempotency key for the group create. Minted once per creation session (this VM is recreated per
        // screen visit) and reused on retry after a failed save, so a double-tap or a silent OkHttp
        // connection-retry of the POST dedups to a single group task. See Y6.
        val clientTaskId: String = UUID.randomUUID().toString(),
        // Group task (only meaningful when taskType == GROUP).
        val adminGroups: List<GroupOption> = emptyList(),
        val selectedGroupLocalId: Long? = null,
        val selectedGroupRemoteId: Long? = null,
        val groupMembers: List<AssigneeOption> = emptyList(),
        // null = unassigned (the valid default — a group-wide task).
        val selectedAssigneeId: Long? = null,
        val priority: String? = null,
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

        data class OnRecurrenceUntilSelect(val until: LocalDate?) : UiAction

        data class OnReminderTimeAdd(val time: LocalTime) : UiAction

        data class OnReminderTimeRemove(val time: LocalTime) : UiAction

        data class OnIntervalChange(val interval: Int) : UiAction

        data class OnWeekdayToggle(val day: DayOfWeek) : UiAction

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

        data class OnGroupSelect(val localId: Long, val remoteId: Long) : UiAction

        data class OnAssigneeSelect(val userId: Long?) : UiAction

        data class OnPrioritySelect(val priority: String?) : UiAction

        data object OnCreate : UiAction
    }

    sealed interface UiEffect {
        data class ShowToast(val messageRes: Int) : UiEffect
    }
}
