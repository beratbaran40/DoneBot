package com.todoapp.mobile.ui.details

/**
 * One editable step on the detail screen. [id] is null for a not-yet-saved row (no checkbox until it
 * is persisted on save). Completion, rename, add and remove are all staged and committed by Save.
 */
data class SubtaskDraft(
    val id: Long?,
    val title: String,
    val isCompleted: Boolean,
)
