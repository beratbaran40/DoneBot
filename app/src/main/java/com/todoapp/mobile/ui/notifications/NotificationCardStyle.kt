package com.todoapp.mobile.ui.notifications

import android.content.Context
import android.text.format.DateFormat
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.todoapp.mobile.common.deviceTimeFormatter
import com.todoapp.mobile.domain.model.NotificationType
import com.todoapp.uikit.theme.TDTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Per-type visual recipe: accent drives the stripe + unread medallion, soft pair styles the read state. */
internal data class NotificationCardStyle(
    @DrawableRes val iconRes: Int,
    val accent: Color,
    val softFill: Color,
    val softGlyph: Color,
)

@Composable
internal fun notificationCardStyle(type: NotificationType): NotificationCardStyle {
    val colors = TDTheme.colors
    val isDark = TDTheme.isDark
    return when (type) {
        NotificationType.INVITATION_RECEIVED -> NotificationCardStyle(
            iconRes = com.example.uikit.R.drawable.ic_members,
            accent = colors.purple,
            softFill = colors.lightPending,
            softGlyph = colors.darkPending,
        )
        NotificationType.INVITATION_ACCEPTED -> NotificationCardStyle(
            iconRes = com.example.uikit.R.drawable.ic_tasks_done,
            accent = if (isDark) colors.mediumGreen else colors.darkGreen,
            softFill = colors.lightGreen,
            softGlyph = colors.darkGreen,
        )
        NotificationType.INVITATION_DECLINED -> NotificationCardStyle(
            iconRes = com.example.uikit.R.drawable.ic_error,
            accent = colors.crossRed,
            softFill = colors.lightRed,
            softGlyph = colors.crossRed,
        )
        NotificationType.TASK_ASSIGNED -> NotificationCardStyle(
            iconRes = com.example.uikit.R.drawable.ic_plus,
            accent = if (isDark) colors.mediumPending else colors.darkPending,
            softFill = colors.lightPending,
            softGlyph = colors.darkPending,
        )
        NotificationType.TASK_COMPLETED -> NotificationCardStyle(
            iconRes = com.example.uikit.R.drawable.ic_tasks_done,
            accent = if (isDark) colors.mediumGreen else colors.darkGreen,
            softFill = colors.lightGreen,
            softGlyph = colors.darkGreen,
        )
        NotificationType.TASK_DUE_SOON -> NotificationCardStyle(
            iconRes = com.example.uikit.R.drawable.ic_sand_clock,
            accent = colors.orange,
            softFill = colors.lightOrange,
            softGlyph = colors.orange,
        )
        NotificationType.UNKNOWN -> NotificationCardStyle(
            iconRes = com.example.uikit.R.drawable.ic_notification,
            accent = colors.pendingGray,
            softFill = colors.lightGray.copy(alpha = 0.5f),
            softGlyph = colors.pendingGray,
        )
    }
}

/** Today/Yesterday cards show a device-formatted time-of-day; Earlier cards show a locale-ordered short date. */
internal fun notificationTimestamp(createdAt: Long, section: NotificationSection, context: Context): String {
    val dateTime = Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault())
    return when (section) {
        NotificationSection.TODAY, NotificationSection.YESTERDAY -> dateTime.format(deviceTimeFormatter(context))
        NotificationSection.EARLIER -> {
            val locale = Locale.getDefault()
            val pattern = DateFormat.getBestDateTimePattern(locale, "MMMd")
            dateTime.format(DateTimeFormatter.ofPattern(pattern, locale))
        }
    }
}
