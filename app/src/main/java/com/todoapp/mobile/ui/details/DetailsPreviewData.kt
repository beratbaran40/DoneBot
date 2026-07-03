@file:Suppress("MagicNumber", "LongParameterList")

package com.todoapp.mobile.ui.details

import androidx.annotation.StringRes
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.TaskCategory
import com.todoapp.mobile.ui.common.taskform.TaskFormType
import com.todoapp.mobile.ui.details.DetailsContract.UiState
import java.time.LocalDate
import java.time.LocalTime

object DetailsPreviewData {
    fun successState(
        isDirty: Boolean = false,
        isSaving: Boolean = false,
        taskTitle: String = "Sample Task",
        taskTimeStart: LocalTime? = LocalTime.of(9, 0),
        taskTimeEnd: LocalTime? = LocalTime.of(10, 0),
        taskDate: LocalDate = LocalDate.now(),
        taskDescription: String = "Sample description",
        dialogSelectedDate: LocalDate? = LocalDate.now(),
        @StringRes titleError: Int? = null,
        selectedCategory: TaskCategory = TaskCategory.PERSONAL,
        customCategoryName: String = "",
        selectedRecurrence: Recurrence = Recurrence.NONE,
        reminderOffsetMinutes: Long? = 0L,
        isAllDay: Boolean = false,
        taskType: TaskFormType = TaskFormType.ONE_TIME,
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
        dialogSelectedDate = dialogSelectedDate,
        titleError = titleError,
        selectedCategory = selectedCategory,
        customCategoryName = customCategoryName,
        selectedRecurrence = selectedRecurrence,
        reminderOffsetMinutes = reminderOffsetMinutes,
        isAllDay = isAllDay,
        taskType = taskType,
        subtaskDrafts = subtaskDrafts,
        isCompleted = isCompleted,
    )
}
