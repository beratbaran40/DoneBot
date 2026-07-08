package com.todoapp.mobile.data.model.network.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthResponseData(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val user: UserData,
)

@Serializable
data class UserData(
    val id: Long,
    val email: String,
    val displayName: String,
    val avatarUrl: String?,
    val emailVerified: Boolean,
    val providers: List<String>,
    val createdAt: String,
)

@Serializable
data class RefreshTokenData(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
)

@Serializable
data class GroupSummaryDataList(
    @SerialName("familyGroups") val groups: List<GroupSummaryData>,
    @SerialName("count") val count: Int,
)

@Serializable
data class GroupSummaryData(
    @SerialName("id")
    val id: Long,
    @SerialName("name")
    val name: String,
    @SerialName("description")
    val description: String,
    @SerialName("role")
    val role: String,
    @SerialName("memberCount")
    val memberCount: Int,
    @SerialName("pendingTaskCount")
    val pendingTaskCount: Int,
    @SerialName("avatarUrl")
    val avatarUrl: String? = null,
    @SerialName("createdAt")
    val createdAt: Long,
)

@Serializable
data class GroupData(
    @SerialName("id")
    val id: Long,
    @SerialName("name")
    val name: String,
    @SerialName("description")
    val description: String,
    @SerialName("avatarUrl")
    val avatarUrl: String? = null,
    @SerialName("createdAt")
    val createdAt: Long,
    @SerialName("updatedAt")
    val updatedAt: Long,
    @SerialName("members")
    val members: List<GroupMemberData>,
    // Outgoing PENDING invites embedded in the detail payload (backend ≥ the pending-invites
    // deploy). Defaulted so responses from an older backend still deserialize.
    @SerialName("pendingInvitations")
    val pendingInvitations: List<GroupInvitationData> = emptyList(),
)

@Serializable
data class GroupInvitationData(
    @SerialName("id")
    val id: Long,
    @SerialName("inviteeEmail")
    val inviteeEmail: String,
    @SerialName("status")
    val status: String = "PENDING",
    @SerialName("createdAt")
    val createdAt: Long = 0L,
)

@Serializable
data class GroupMemberData(
    @SerialName("userId")
    val userId: Long,
    @SerialName("displayName")
    val displayName: String,
    @SerialName("email")
    val email: String,
    @SerialName("avatarUrl")
    val avatarUrl: String?,
    val emailVerified: Boolean = false,
    val providers: List<String> = emptyList(),
    val createdAt: String = "",
    @SerialName("role")
    val role: String = "",
    @SerialName("joinedAt")
    val joinedAt: Long = 0L,
)

@Serializable
data class FCMTokenResponseData(
    val token: String,
    val deviceId: String,
    val deviceName: String,
)

@Serializable
data class GroupActivityData(
    @SerialName("id") val id: Long,
    @SerialName("type") val type: String,
    @SerialName("actorName") val actorName: String,
    @SerialName("actorAvatarUrl") val actorAvatarUrl: String? = null,
    @SerialName("description") val description: String,
    @SerialName("timestamp") val timestamp: Long,
    @SerialName("taskTitle") val taskTitle: String? = null,
    // Backend ≥ V18; defaulted so older backends still deserialize. Carries the person the
    // action was done TO (assignee / removed member / new owner) for client-side localization.
    @SerialName("targetName") val targetName: String? = null,
)

@Serializable
data class GroupActivityDataList(
    @SerialName("activities") val activities: List<GroupActivityData>,
)

@Serializable
data class GroupTaskData(
    @SerialName("id") val id: Long,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String? = null,
    @SerialName("isCompleted") val isCompleted: Boolean = false,
    @SerialName("priority") val priority: String? = null,
    @SerialName("dueDate") val dueDate: Long? = null,
    @SerialName("assignee") val assignee: GroupMemberData? = null,
    @SerialName("photoUrls") val photoUrls: List<String> = emptyList(),
    @SerialName("locationLat") val locationLat: Double? = null,
    @SerialName("locationLng") val locationLng: Double? = null,
    @SerialName("locationName") val locationName: String? = null,
    @SerialName("locationAddress") val locationAddress: String? = null,
)

@Serializable
data class GroupTaskListData(
    @SerialName("tasks") val tasks: List<GroupTaskData>,
)
