package com.todoapp.mobile.ui.journal.entry

import com.todoapp.mobile.ui.journal.entry.JournalEntryContract.UiState

internal object JournalEntryPreviewData {
    fun new(): UiState.Editing = UiState.Editing(
        entryId = 0L,
        content = "",
        photoPaths = emptyList(),
        createdAt = null,
        isDirty = false,
    )

    fun existing(): UiState.Editing = UiState.Editing(
        entryId = 12L,
        content = "A productive morning\n\nStarted the day with a brisk walk and a fresh coffee.\nFelt energised through the entire focus session.",
        photoPaths = listOf("/sample/a.jpg", "/sample/b.jpg"),
        createdAt = 1747000000000L,
        isDirty = false,
    )
}
