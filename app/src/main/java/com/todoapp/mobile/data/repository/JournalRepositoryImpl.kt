package com.todoapp.mobile.data.repository

import com.todoapp.mobile.data.mapper.toDomain
import com.todoapp.mobile.data.mapper.toEntity
import com.todoapp.mobile.data.source.local.JournalEntryDao
import com.todoapp.mobile.data.storage.JournalPhotoStorage
import com.todoapp.mobile.domain.model.JournalEntry
import com.todoapp.mobile.domain.repository.JournalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class JournalRepositoryImpl
@Inject
constructor(
    private val dao: JournalEntryDao,
    private val photoStorage: JournalPhotoStorage,
    private val dataStoreHelper: DataStoreHelper,
) : JournalRepository {
    /** Stable backend id of the logged-in user, or 0 when signed out (cleared on logout). */
    private suspend fun currentOwnerId(): Long = dataStoreHelper.observeUser().first()?.id ?: 0L

    // Re-derives the owner from the cached user so the stream swaps buckets on login/logout/user-switch.
    @OptIn(ExperimentalCoroutinesApi::class)
    @Suppress("IgnoredReturnValue") // Detekt false positive: inner `rows.map { ... }` value is the lambda result.
    override fun observeEntries(): Flow<List<JournalEntry>> =
        dataStoreHelper.observeUser()
            .map { it?.id ?: 0L }
            .distinctUntilChanged()
            .flatMapLatest { ownerId -> dao.observeAllForOwner(ownerId) }
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun getEntry(id: Long): JournalEntry? = dao.getByIdForOwner(id, currentOwnerId())?.toDomain()

    override suspend fun upsertEntry(entry: JournalEntry): Long {
        val ownerId = currentOwnerId()
        val now = System.currentTimeMillis()
        // Scope the previous-row lookup to the owner so a guessed id can't read/overwrite another user's row.
        val previous = if (entry.id != 0L) dao.getByIdForOwner(entry.id, ownerId) else null
        val createdAt = previous?.createdAt ?: now
        val toSave = entry.copy(createdAt = createdAt, updatedAt = now)
            .toEntity()
            .copy(ownerUserId = ownerId)
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
        val ownerId = currentOwnerId()
        val existing = dao.getByIdForOwner(id, ownerId) ?: return
        photoStorage.deletePhotos(existing.toDomain().photoPaths)
        dao.deleteByIdForOwner(id, ownerId)
    }

    override suspend fun claimOrphansForCurrentUser() {
        if (dataStoreHelper.isJournalOrphansClaimed()) return
        val ownerId = currentOwnerId()
        if (ownerId == 0L) return // only claim once a real user is logged in
        dao.claimOrphans(ownerId)
        dataStoreHelper.setJournalOrphansClaimed(true)
    }

    override suspend fun deleteAllForCurrentUser() {
        val ownerId = currentOwnerId()
        if (ownerId == 0L) return
        val rows = dao.observeAllForOwner(ownerId).first()
        photoStorage.deletePhotos(rows.flatMap { it.toDomain().photoPaths })
        dao.deleteAllForOwner(ownerId)
    }
}
