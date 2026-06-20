package com.todoapp.mobile.domain.model

import androidx.compose.runtime.Immutable

/**
 * A single step of a staged task. A task is "staged" iff it has ≥1 subtask (the type is derived, not a
 * stored enum). Personal tasks only.
 */
@Immutable
data class Subtask(
    val id: Long = 0L,
    val parentTaskId: Long = 0L,
    val title: String,
    val isCompleted: Boolean = false,
    val orderIndex: Int = 0,
)

/**
 * Completion invariant for staged tasks: the parent is complete **iff** it has at least one step and
 * every step is done. An empty list is never "done" (a task with no steps is not a staged task).
 */
fun List<Subtask>.allSubtasksDone(): Boolean = isNotEmpty() && all { it.isCompleted }
