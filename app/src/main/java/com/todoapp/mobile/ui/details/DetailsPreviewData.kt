@file:Suppress("MagicNumber", "LongParameterList")

package com.todoapp.mobile.ui.details

import androidx.annotation.StringRes
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.TaskCategory
import com.todoapp.mobile.domain.model.TaskType
import com.todoapp.mobile.ui.common.taskform.TaskCapabilities
import com.todoapp.mobile.ui.details.DetailsContract.UiState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

object DetailsPreviewData {
    /**
     * [capabilities] is the parameter that matters here, and it used to be missing.
     *
     * Almost every section of the detail screen is gated on it — the frequency block, the reminder
     * editor, the scheduled end, the step editor, the completion card — so a preview that passed
     * `taskType = ROUTINE` but left the capabilities at their all-false default rendered the plain
     * one-off body under a Routine header. Every routine and staged preview in the file was showing
     * the wrong screen.
     */
    @Suppress("LongParameterList")
    fun successState(
        isDirty: Boolean = false,
        isSaving: Boolean = false,
        taskTitle: String = "Sample Task",
        taskTimeStart: LocalTime? = LocalTime.of(9, 0),
        taskTimeEnd: LocalTime? = LocalTime.of(10, 0),
        taskDate: LocalDate = LocalDate.now(),
        taskDescription: String = "Sample description",
        @StringRes titleError: Int? = null,
        selectedCategory: TaskCategory = TaskCategory.PERSONAL,
        customCategoryName: String = "",
        selectedRecurrence: Recurrence = Recurrence.NONE,
        reminderOffsetMinutes: Long? = 0L,
        isAllDay: Boolean = false,
        taskType: TaskType = TaskType.ONE_TIME,
        capabilities: TaskCapabilities = TaskCapabilities(
            recurs = false,
            hasSteps = false,
            hasMultipleReminders = false,
        ),
        reminderTimes: List<LocalTime> = emptyList(),
        recurrenceUntil: LocalDate? = null,
        recurrenceInterval: Int = 1,
        recurrenceByDay: Set<DayOfWeek> = emptySet(),
        routineDayIndex: Int? = null,
        routineDayTotal: Int? = null,
        subtaskDrafts: List<SubtaskDraft> = emptyList(),
        isCompleted: Boolean = false,
    ) = UiState.Success(
        isDirty = isDirty,
        isSaving = isSaving,
        taskId = 1L,
        taskTitle = taskTitle,
        taskTimeStart = taskTimeStart,
        taskTimeEnd = taskTimeEnd,
        taskDate = taskDate,
        taskDescription = taskDescription,
        titleError = titleError,
        selectedCategory = selectedCategory,
        customCategoryName = customCategoryName,
        selectedRecurrence = selectedRecurrence,
        reminderOffsetMinutes = reminderOffsetMinutes,
        isAllDay = isAllDay,
        taskType = taskType,
        capabilities = capabilities,
        reminderTimes = reminderTimes,
        recurrenceUntil = recurrenceUntil,
        recurrenceInterval = recurrenceInterval,
        recurrenceByDay = recurrenceByDay,
        routineDayIndex = routineDayIndex,
        routineDayTotal = routineDayTotal,
        subtaskDrafts = subtaskDrafts,
        isCompleted = isCompleted,
    )
}
