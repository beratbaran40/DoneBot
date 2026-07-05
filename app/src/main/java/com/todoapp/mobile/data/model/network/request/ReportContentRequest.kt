package com.todoapp.mobile.data.model.network.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Body for `POST /family-groups/{groupId}/reports` — flags offensive or inappropriate group
 * content (a member, a shared task photo, or a task) for human moderation review. Required by
 * Google Play's user-generated-content policy. Blocking a user is handled client-side.
 */
@Serializable
data class ReportContentRequest(
    @SerialName("targetType") val targetType: String,
    @SerialName("targetUserId") val targetUserId: Long? = null,
    @SerialName("targetRef") val targetRef: String? = null,
    @SerialName("reason") val reason: String? = null,
)

/** Report target kinds accepted by the backend `content_reports` endpoint. */
object ReportTargetType {
    const val MEMBER = "MEMBER"
    const val PHOTO = "PHOTO"
    const val TASK = "TASK"
}
