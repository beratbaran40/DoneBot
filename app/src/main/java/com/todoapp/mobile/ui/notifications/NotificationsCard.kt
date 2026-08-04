package com.todoapp.mobile.ui.notifications

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.model.Notification
import com.todoapp.mobile.domain.model.NotificationType
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

@Composable
internal fun NotificationCard(
    notification: Notification,
    section: NotificationSection,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val style = notificationCardStyle(notification.type)
    val rendered = NotificationContent.render(
        context = context,
        type = notification.type,
        payload = notification.payload,
        fallbackTitle = notification.title,
        fallbackBody = notification.body,
    )
    val shape = TDTheme.shapes.large
    val borderAlpha = if (TDTheme.isDark) 0.25f else 0.4f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(TDTheme.colors.settingsCard)
            .border(width = 1.dp, color = TDTheme.colors.lightGray.copy(alpha = borderAlpha), shape = shape)
            .clickable(onClick = onClick),
    ) {
        val stripeAlpha = if (notification.isRead) 0.35f else 1f
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(style.accent.copy(alpha = stripeAlpha)),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(14.dp),
        ) {
            Row {
                NotificationMedallion(style = style, isRead = notification.isRead)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    NotificationTitleRow(
                        title = rendered.title,
                        timestamp = notificationTimestamp(notification.createdAt, section, context),
                        isRead = notification.isRead,
                    )
                    if (rendered.body.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        TDText(
                            text = rendered.body,
                            style = TDTheme.typography.subheading1,
                            color = TDTheme.colors.gray,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (notification.type == NotificationType.INVITATION_RECEIVED) {
                NotificationInvitationSummary(payload = notification.payload)
            }
        }
    }
}

@Composable
private fun NotificationTitleRow(
    title: String,
    timestamp: String,
    isRead: Boolean,
) {
    Row(verticalAlignment = Alignment.Top) {
        val titleStyle = if (isRead) {
            TDTheme.typography.heading7
        } else {
            TDTheme.typography.heading7.copy(fontWeight = FontWeight.SemiBold)
        }
        TDText(
            text = title,
            style = titleStyle,
            color = if (isRead) TDTheme.colors.gray else TDTheme.colors.onBackground,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        TDText(
            text = timestamp,
            style = TDTheme.typography.subheading1,
            color = TDTheme.colors.pendingGray,
            maxLines = 1,
        )
    }
}

@Composable
private fun NotificationMedallion(style: NotificationCardStyle, isRead: Boolean) {
    val fillColor = if (isRead) style.softFill else style.accent
    val glyphTint = if (isRead) style.softGlyph.copy(alpha = 0.7f) else TDTheme.colors.white
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(fillColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = tdPainter(style.iconRes),
            contentDescription = null,
            tint = glyphTint,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun NotificationInvitationSummary(payload: Map<String, String>) {
    val description = payload["groupDescription"]?.takeIf { it.isNotBlank() }
    val memberCount = payload["memberCount"]?.toIntOrNull()
    if (description == null && memberCount == null) return
    Spacer(Modifier.height(12.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TDTheme.shapes.medium)
            .background(TDTheme.colors.lightPending)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        if (description != null) {
            TDText(
                text = description,
                style = TDTheme.typography.regularTextStyle,
                color = TDTheme.colors.onBackground,
                maxLines = 3,
            )
        }
        if (memberCount != null) {
            if (description != null) Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = tdPainter(com.example.uikit.R.drawable.ic_members),
                    contentDescription = null,
                    tint = TDTheme.colors.darkPending,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                TDText(
                    text = pluralMembers(memberCount, LocalContext.current),
                    style = TDTheme.typography.subheading4,
                    color = TDTheme.colors.darkPending,
                )
            }
        }
    }
}

private fun pluralMembers(count: Int, context: Context): String = context.resources.getQuantityString(R.plurals.member_count, count, count)

private fun previewNotification(id: Long, type: NotificationType, isRead: Boolean) = Notification(
    id = id,
    type = type,
    title = "DoneBot update",
    body = "Something new is waiting in the app",
    payload = mapOf(
        "groupName" to "Smith Family",
        "taskTitle" to "Buy groceries",
        "inviterName" to "Berat",
        "acceptorName" to "Ayse",
        "declinerName" to "Mehmet",
        "actorName" to "Ayse",
    ),
    isRead = isRead,
    createdAt = System.currentTimeMillis(),
)

@TDPreview
@Composable
private fun NotificationCardTypesPreview() {
    TDTheme {
        Column(
            modifier = Modifier
                .background(TDTheme.colors.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            NotificationType.entries.forEachIndexed { index, type ->
                NotificationCard(
                    notification = previewNotification(id = index.toLong(), type = type, isRead = false),
                    section = NotificationSection.TODAY,
                    onClick = {},
                )
            }
            NotificationCard(
                notification = previewNotification(id = 99, type = NotificationType.TASK_COMPLETED, isRead = true),
                section = NotificationSection.TODAY,
                onClick = {},
            )
        }
    }
}
