package com.todoapp.mobile.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "group_tasks",
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
data class GroupTaskEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "remote_id") val remoteId: Long? = null,
    @ColumnInfo(name = "local_group_id") val localGroupId: Long,
    @ColumnInfo(name = "remote_group_id") val remoteGroupId: Long,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "description") val description: String? = null,
    @ColumnInfo(name = "is_completed", defaultValue = "0") val isCompleted: Boolean = false,
    @ColumnInfo(name = "priority") val priority: String? = null,
    @ColumnInfo(name = "due_date") val dueDate: Long? = null,
    @ColumnInfo(name = "is_all_day", defaultValue = "0") val isAllDay: Boolean = false,
    @ColumnInfo(name = "time_start") val timeStart: Long? = null,
    @ColumnInfo(name = "time_end") val timeEnd: Long? = null,
    @ColumnInfo(name = "assignee_user_id") val assigneeUserId: Long? = null,
    @ColumnInfo(name = "assignee_display_name") val assigneeDisplayName: String? = null,
    @ColumnInfo(name = "assignee_avatar_url") val assigneeAvatarUrl: String? = null,
    @ColumnInfo(name = "photo_urls", defaultValue = "") val photoUrls: String = "",
    @ColumnInfo(name = "location_lat") val locationLat: Double? = null,
    @ColumnInfo(name = "location_lng") val locationLng: Double? = null,
    @ColumnInfo(name = "location_name") val locationName: String? = null,
    @ColumnInfo(name = "location_address") val locationAddress: String? = null,
)
