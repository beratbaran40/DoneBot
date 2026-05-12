package com.todoapp.mobile.data.repository

import com.todoapp.mobile.data.mapper.toDomain
import com.todoapp.mobile.data.mapper.toEntity
import com.todoapp.mobile.data.source.local.JournalEntryDao
import com.todoapp.mobile.data.storage.JournalPhotoStorage
import com.todoapp.mobile.domain.model.JournalEntry
import com.todoapp.mobile.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class JournalRepositoryImpl
@Inject
constructor(
    private val dao: JournalEntryDao,
    private val photoStorage: JournalPhotoStorage,
) : JournalRepository {
    override fun observeEntries(): Flow<List<JournalEntry>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getEntry(id: Long): JournalEntry? = dao.getById(id)?.toDomain()

    override suspend fun upsertEntry(entry: JournalEntry): Long {
        val now = System.currentTimeMillis()
        val previous = if (entry.id != 0L) dao.getById(entry.id) else null
        val createdAt = previous?.createdAt ?: now
        val toSave = entry.copy(createdAt = createdAt, updatedAt = now).toEntity()
        val newId = dao.upsert(toSave)
        val resolvedId = if (entry.id == 0L) newId else entry.id

        if (previous != null) {
            val previousPaths = previous.toDomain().photoPaths.toSet()
            val currentPaths = entry.photoPaths.toSet()
            val removed = previousPaths - currentPaths
            if (removed.isNotEmpty()) photoStorage.deletePhotos(removed)
        }

        return resolvedId
    }

    override suspend fun deleteEntry(id: Long) {
        val existing = dao.getById(id) ?: return
        photoStorage.deletePhotos(existing.toDomain().photoPaths)
        dao.deleteById(id)
    }
}
