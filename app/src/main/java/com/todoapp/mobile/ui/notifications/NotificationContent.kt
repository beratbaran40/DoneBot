package com.todoapp.mobile.ui.notifications

import android.content.Context
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.model.NotificationType

/**
 * Renders a localized title + body from a notification's type + payload, falling back to the
 * server-rendered English strings stored on the inbox row when required params are missing or
 * the type is unknown. Single source of truth for both the in-app inbox and FCM banners so
 * the user sees the same wording in their preferred locale across both surfaces.
 */
object NotificationContent {
    data class Rendered(val title: String, val body: String)

    @Suppress("CyclomaticComplexMethod")
    fun render(
        context: Context,
        type: NotificationType,
        payload: Map<String, String>,
        fallbackTitle: String,
        fallbackBody: String,
    ): Rendered {
        val groupName = payload["groupName"]?.takeIf { it.isNotBlank() }
        val taskTitle = payload["taskTitle"]?.takeIf { it.isNotBlank() }
        val inviter = payload["inviterName"]?.takeIf { it.isNotBlank() }
        val acceptor = payload["acceptorName"]?.takeIf { it.isNotBlank() }
        val decliner = payload["declinerName"]?.takeIf { it.isNotBlank() }
        val actor = payload["actorName"]?.takeIf { it.isNotBlank() }

        return when (type) {
            NotificationType.INVITATION_RECEIVED -> {
                val title = context.getString(R.string.notif_invitation_received_title)
                val body = if (inviter != null && groupName != null) {
                    context.getString(R.string.notif_invitation_received_body, inviter, groupName)
                } else if (groupName != null) {
                    context.getString(R.string.notif_invitation_received_body_anon, groupName)
                } else {
                    fallbackBody
                }
                Rendered(title, body)
            }
            NotificationType.INVITATION_ACCEPTED -> {
                val title = context.getString(R.string.notif_invitation_accepted_title)
                val body = if (acceptor != null && groupName != null) {
                    context.getString(R.string.notif_invitation_accepted_body, acceptor, groupName)
                } else {
                    fallbackBody
                }
                Rendered(title, body)
            }
            NotificationType.INVITATION_DECLINED -> {
                val title = context.getString(R.string.notif_invitation_declined_title)
                val body = if (decliner != null && groupName != null) {
                    context.getString(R.string.notif_invitation_declined_body, decliner, groupName)
                } else {
                    fallbackBody
                }
                Rendered(title, body)
            }
            NotificationType.TASK_ASSIGNED -> {
                val title = context.getString(R.string.notif_task_assigned_title)
                val body = if (taskTitle != null) {
                    context.getString(R.string.notif_task_assigned_body, taskTitle)
                } else {
                    fallbackBody
                }
                Rendered(title, body)
            }
            NotificationType.TASK_COMPLETED -> {
                val title = context.getString(R.string.notif_task_completed_title)
                val body = if (actor != null && taskTitle != null) {
                    context.getString(R.string.notif_task_completed_body, actor, taskTitle)
                } else {
                    fallbackBody
                }
                Rendered(title, body)
            }
            NotificationType.TASK_DUE_SOON -> {
                val title = context.getString(R.string.notif_task_due_soon_title)
                val body = if (taskTitle != null && groupName != null) {
                    context.getString(R.string.notif_task_due_soon_body, taskTitle, groupName)
                } else {
                    fallbackBody
                }
                Rendered(title, body)
            }
            NotificationType.GROUP_OWNERSHIP_TRANSFERRED -> {
                val title = if (groupName != null) {
                    context.getString(R.string.notification_group_ownership_title_format, groupName)
                } else {
                    fallbackTitle
                }
                Rendered(title, context.getString(R.string.notification_group_ownership_body))
            }
            NotificationType.UNKNOWN -> Rendered(fallbackTitle, fallbackBody)
        }
    }
}
