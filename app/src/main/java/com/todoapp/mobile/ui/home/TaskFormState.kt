package com.todoapp.mobile.ui.home

import androidx.annotation.StringRes
import androidx.compose.runtime.Stable
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.TaskCategory
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Stable
data class TaskFormState(
    val taskTitle: String = "",
    val dialogSelectedDate: LocalDate? = null,
    val taskTimeStart: LocalTime? = null,
    val taskTimeEnd: LocalTime? = null,
    val taskDescription: String = "",
    val isAdvancedSettingsExpanded: Boolean = false,
    val isTaskSecret: Boolean = false,
    @StringRes val titleErrorRes: Int? = null,
    @StringRes val dateErrorRes: Int? = null,
    @StringRes val timeErrorRes: Int? = null,
    val selectedGroupId: Long? = null,
    val selectedPriority: String? = null,
    val selectedAssigneeId: Long? = null,
    val pendingPhotos: List<PendingPhoto> = emptyList(),
    val existingPhotos: List<ExistingPhoto> = emptyList(),
    val photoIdsToDelete: Set<Long> = emptySet(),
    /**
     * Minutes before the task's due time at which to fire the reminder alarm.
     * 0L = on-time, positive = N minutes before, null = no reminder.
     */
    val reminderOffsetMinutes: Long? = 0L,
    val selectedCategory: TaskCategory = TaskCategory.PERSONAL,
    val customCategoryName: String = "",
    val selectedRecurrence: Recurrence = Recurrence.NONE,
    val isAllDay: Boolean = false,
    val locationName: String? = null,
    val locationAddress: String? = null,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    // Idempotency key for the eventual create. Minted once per fresh form (a new TaskFormState) and kept
    // stable across edits (copy) so a double-tap or a silent OkHttp connection-retry of the POST dedups to
    // a single server task (the backend dedups on clientTaskId). See Y6.
    val clientTaskId: String = UUID.randomUUID().toString(),
) {
    companion object {
        fun smartDefault(today: LocalDate, now: LocalTime, lastReminderOffset: Long?): TaskFormState {
            val isWeekend = today.dayOfWeek == DayOfWeek.SATURDAY || today.dayOfWeek == DayOfWeek.SUNDAY
            val rounded = roundToNearestHalfHour(now.plusHours(1))
            return TaskFormState(
                dialogSelectedDate = today,
                isAllDay = isWeekend,
                taskTimeStart = if (isWeekend) null else rounded,
                taskTimeEnd = if (isWeekend) null else rounded.plusMinutes(30),
                reminderOffsetMinutes = lastReminderOffset ?: 0L,
            )
        }

        private fun roundToNearestHalfHour(time: LocalTime): LocalTime {
            val minutes = time.minute
            return when {
                minutes < 15 -> time.withMinute(0).withSecond(0).withNano(0)
                minutes < 45 -> time.withMinute(30).withSecond(0).withNano(0)
                else -> time.withMinute(0).withSecond(0).withNano(0).plusHours(1)
            }
        }
    }
}

data class PendingPhoto(
    val bytes: ByteArray,
    val mimeType: String,
)

data class ExistingPhoto(
    val id: Long,
    val url: String,
)

sealed interface TaskFormUiAction {
    data object Dismiss : TaskFormUiAction

    data object Create : TaskFormUiAction

    data class TitleChange(
        val title: String,
    ) : TaskFormUiAction

    data class DateSelect(
        val date: LocalDate,
    ) : TaskFormUiAction

    data object DateDeselect : TaskFormUiAction

    data class TimeStartChange(
        val time: LocalTime,
    ) : TaskFormUiAction

    data class TimeEndChange(
        val time: LocalTime,
    ) : TaskFormUiAction

    data class DescriptionChange(
        val description: String,
    ) : TaskFormUiAction

    data object ToggleAdvancedSettings : TaskFormUiAction

    data class SecretChange(
        val isSecret: Boolean,
    ) : TaskFormUiAction

    data class GroupSelectionChanged(
        val groupId: Long?,
    ) : TaskFormUiAction

    data class PriorityChange(
        val priority: String?,
    ) : TaskFormUiAction

    data class AssigneeChange(
        val userId: Long?,
    ) : TaskFormUiAction

    data class PhotoPicked(
        val bytes: ByteArray,
        val mimeType: String,
    ) : TaskFormUiAction

    data class PhotoRemoveAt(
        val index: Int,
    ) : TaskFormUiAction

    data class ExistingPhotoToggleDelete(
        val photoId: Long,
    ) : TaskFormUiAction

    data class ReminderOffsetChange(
        val minutes: Long?,
    ) : TaskFormUiAction

    data class CategoryChange(
        val category: TaskCategory,
    ) : TaskFormUiAction

    data class CustomCategoryNameChange(
        val name: String,
    ) : TaskFormUiAction

    data class RecurrenceChange(
        val recurrence: Recurrence,
    ) : TaskFormUiAction

    data class AllDayChange(
        val isAllDay: Boolean,
    ) : TaskFormUiAction

    data class LocationPicked(
        val name: String,
        val address: String,
        val lat: Double?,
        val lng: Double?,
    ) : TaskFormUiAction

    data object LocationCleared : TaskFormUiAction
}
