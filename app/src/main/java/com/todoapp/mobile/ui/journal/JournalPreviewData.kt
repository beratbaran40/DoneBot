package com.todoapp.mobile.ui.journal

import com.todoapp.mobile.domain.model.JournalEntry
import com.todoapp.mobile.ui.journal.JournalContract.DateGroup
import com.todoapp.mobile.ui.journal.JournalContract.GroupedSection
import com.todoapp.mobile.ui.journal.JournalContract.UiState

internal object JournalPreviewData {
    private const val DAY_MS = 24L * 60L * 60L * 1000L

    fun samples(now: Long = 1747000000000L): List<JournalEntry> = listOf(
        JournalEntry(
            id = 1,
            title = "A productive morning",
            content = "Started the day with a brisk walk and a fresh coffee. Felt energised through the entire focus session.",
            photoPaths = listOf("/sample/a.jpg", "/sample/b.jpg"),
            createdAt = now,
            updatedAt = now,
        ),
        JournalEntry(
            id = 2,
            title = null.toString(),
            content = "Quiet evening. Read a chapter and called mom.",
            photoPaths = emptyList(),
            createdAt = now - DAY_MS,
            updatedAt = now - DAY_MS,
        ),
        JournalEntry(
            id = 3,
            title = "Long week wrap",
            content = "Hectic days, but managed to ship the migration. Grateful for the team support.",
            photoPaths = listOf("/sample/c.jpg"),
            createdAt = now - 3 * DAY_MS,
            updatedAt = now - 3 * DAY_MS,
        ),
        JournalEntry(
            id = 4,
            title = "Doctor's visit",
            content = "Routine check, everything looked fine. Treated myself to ice cream after.",
            photoPaths = emptyList(),
            createdAt = now - 15 * DAY_MS,
            updatedAt = now - 15 * DAY_MS,
        ),
        JournalEntry(
            id = 5,
            title = "Old memory",
            content = "Found an old photo album. So many faces I had forgotten.",
            photoPaths = emptyList(),
            createdAt = now - 90 * DAY_MS,
            updatedAt = now - 90 * DAY_MS,
        ),
    )

    fun successState(now: Long = 1747000000000L): UiState.Success {
        val list = samples(now)
        return UiState.Success(
            sections = listOf(
                GroupedSection(DateGroup.TODAY, listOf(list[0])),
                GroupedSection(DateGroup.YESTERDAY, listOf(list[1])),
                GroupedSection(DateGroup.THIS_WEEK, listOf(list[2])),
                GroupedSection(DateGroup.THIS_MONTH, listOf(list[3])),
                GroupedSection(DateGroup.OLDER, listOf(list[4])),
            ),
            searchQuery = "",
            isRawListEmpty = false,
            isFilteredEmpty = false,
        )
    }

    fun emptyState(): UiState.Success = UiState.Success(
        sections = emptyList(),
        searchQuery = "",
        isRawListEmpty = true,
        isFilteredEmpty = true,
    )

    fun filteredEmptyState(): UiState.Success = UiState.Success(
        sections = emptyList(),
        searchQuery = "vacation",
        isRawListEmpty = false,
        isFilteredEmpty = true,
    )
}
