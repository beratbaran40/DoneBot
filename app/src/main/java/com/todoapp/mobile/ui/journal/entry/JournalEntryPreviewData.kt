package com.todoapp.mobile.ui.journal.entry

import com.todoapp.mobile.domain.model.JournalMood
import com.todoapp.mobile.ui.journal.entry.JournalEntryContract.UiState

internal object JournalEntryPreviewData {
    fun new(): UiState.Editing = UiState.Editing(
        entryId = 0L,
        content = "",
        mood = null,
        photoPaths = emptyList(),
        createdAt = null,
        isDirty = false,
    )

    fun existing(): UiState.Editing = UiState.Editing(
        entryId = 12L,
        content = "A productive morning\n\nStarted the day with a brisk walk and a fresh coffee.\nFelt energised through the entire focus session.",
        mood = JournalMood.HAPPY,
        photoPaths = listOf("/sample/a.jpg", "/sample/b.jpg"),
        createdAt = 1747000000000L,
        isDirty = false,
    )

    fun infoDialog(): UiState.Editing = existing().copy(showInfoDialog = true)
}
