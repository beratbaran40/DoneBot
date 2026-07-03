package com.todoapp.mobile.data.source.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.todoapp.mobile.data.model.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    // Cap the observed list at the last 500 messages so an ever-growing chat history
    // doesn't bloat StateFlow emissions and slow ChatScreen scrolling. Older messages
    // remain in the DB; UI just stops showing them past the cap.
    @Query(
        """
        SELECT * FROM (
            SELECT * FROM chat_messages ORDER BY created_at DESC, id DESC LIMIT 500
        )
        ORDER BY created_at ASC, id ASC
        """,
    )
    fun observeAll(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY created_at ASC, id ASC")
    suspend fun getAll(): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity): Long

    // Retention: keep only the most-recent [keep] rows so chat_messages can't grow unbounded across
    // the app's lifetime. [keep] is chosen >= the observeAll() display cap, so pruning never removes
    // a message the UI can still show, and it stays far above the send-time last-10-turns window.
    @Query(
        """
        DELETE FROM chat_messages
        WHERE id NOT IN (
            SELECT id FROM chat_messages ORDER BY created_at DESC, id DESC LIMIT :keep
        )
        """,
    )
    suspend fun pruneToLast(keep: Int)

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAll()
}
