package com.todoapp.mobile.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "group_activities",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["local_group_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("local_group_id")],
)
data class GroupActivityEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "remote_id") val remoteId: Long,
    @ColumnInfo(name = "local_group_id") val localGroupId: Long,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "actor_name") val actorName: String,
    @ColumnInfo(name = "actor_avatar_url") val actorAvatarUrl: String? = null,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "task_title") val taskTitle: String? = null,
    @ColumnInfo(name = "target_name") val targetName: String? = null,
)
