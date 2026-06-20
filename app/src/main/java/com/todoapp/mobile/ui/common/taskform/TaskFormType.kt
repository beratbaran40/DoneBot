package com.todoapp.mobile.ui.common.taskform

import com.todoapp.mobile.domain.model.Recurrence

/** The three task shapes. Derived from a task's data, never a stored column. */
enum class TaskFormType { ONE_TIME, ROUTINE, STAGED }

/** Derive the form type the same way everywhere: subtasks ⇒ staged, recurrence ⇒ routine, else one-time. */
fun taskFormType(hasSubtasks: Boolean, recurrence: Recurrence): TaskFormType = when {
    hasSubtasks -> TaskFormType.STAGED
    recurrence != Recurrence.NONE -> TaskFormType.ROUTINE
    else -> TaskFormType.ONE_TIME
}
