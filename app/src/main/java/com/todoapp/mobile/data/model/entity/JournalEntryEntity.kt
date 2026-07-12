package com.todoapp.mobile.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "photo_paths") val photoPaths: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    // Backend user id of the entry's owner. Scopes the local-only journal per logged-in user so a
    // different account on the same device never sees it. 0 = unclaimed (pre-v20 rows / logged-out).
    @ColumnInfo(name = "owner_user_id", defaultValue = "0") val ownerUserId: Long = 0L,
)
