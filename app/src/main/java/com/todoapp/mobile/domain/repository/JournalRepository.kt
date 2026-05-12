package com.todoapp.mobile.domain.repository

import com.todoapp.mobile.domain.model.JournalEntry
import kotlinx.coroutines.flow.Flow

interface JournalRepository {
    fun observeEntries(): Flow<List<JournalEntry>>

    suspend fun getEntry(id: Long): JournalEntry?

    /**
     * Inserts a new entry (when [JournalEntry.id] == 0) or replaces an existing one.
     * On update, any photo paths present in the previous entry but missing from [entry]
     * are removed from disk by the repository.
     *
     * @return the persisted entry's id.
     */
    suspend fun upsertEntry(entry: JournalEntry): Long

    /**
     * Deletes the entry and all photos attached to it from disk.
     */
    suspend fun deleteEntry(id: Long)
}
