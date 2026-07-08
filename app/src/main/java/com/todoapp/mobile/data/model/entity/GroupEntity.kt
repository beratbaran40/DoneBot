package com.todoapp.mobile.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// remote_id is unique so a re-synced backend group can never materialize as a second local row
// (SQLite treats NULLs as distinct, so unsynced rows are unaffected). Paired with
// GroupDao.insertIgnoring as the floor beneath the repository's sync mutex.
@Entity(
    tableName = "groups",
    indices = [Index(value = ["remote_id"], unique = true)],
)
data class GroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "role", defaultValue = "") val role: String = "",
    @ColumnInfo(name = "memberCount", defaultValue = "0") val memberCount: Int = 0,
    @ColumnInfo(name = "pendingTaskCount", defaultValue = "0") val pendingTaskCount: Int = 0,
    @ColumnInfo(name = "remote_id") val remoteId: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "order_index", defaultValue = "0") val orderIndex: Int = 0,
)
