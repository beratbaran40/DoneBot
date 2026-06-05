package com.todoapp.mobile.data.source.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.todoapp.mobile.data.model.entity.JournalEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalEntryDao {
    @Query("SELECT * FROM journal_entries WHERE owner_user_id = :ownerId ORDER BY created_at DESC")
    fun observeAllForOwner(ownerId: Long): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE id = :id AND owner_user_id = :ownerId")
    suspend fun getByIdForOwner(id: Long, ownerId: Long): JournalEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: JournalEntryEntity): Long

    @Query("DELETE FROM journal_entries WHERE id = :id AND owner_user_id = :ownerId")
    suspend fun deleteByIdForOwner(id: Long, ownerId: Long)

    /** One-time backfill: assigns all unclaimed (owner_user_id = 0) rows to the given user. */
    @Query("UPDATE journal_entries SET owner_user_id = :ownerId WHERE owner_user_id = 0")
    suspend fun claimOrphans(ownerId: Long)

    /** Account-deletion purge: removes every row belonging to the given user. */
    @Query("DELETE FROM journal_entries WHERE owner_user_id = :ownerId")
    suspend fun deleteAllForOwner(ownerId: Long)
}
