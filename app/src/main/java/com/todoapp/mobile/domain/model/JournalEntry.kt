package com.todoapp.mobile.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class JournalEntry(
    val id: Long,
    val title: String,
    val content: String,
    val photoPaths: List<String>,
    val createdAt: Long,
    val updatedAt: Long,
) {
    companion object {
        val EMPTY: JournalEntry = JournalEntry(
            id = 0L,
            title = "",
            content = "",
            photoPaths = emptyList(),
            createdAt = 0L,
            updatedAt = 0L,
        )
    }
}
