package com.todoapp.mobile.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.todoapp.mobile.R
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

@Composable
internal fun NotificationsHeader(
    hasUnread: Boolean,
    onMarkAllRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            TDText(
                text = stringResource(R.string.notifications_header_overline),
                style = TDTheme.typography.subheading2.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                ),
                color = TDTheme.colors.gray,
            )
            Spacer(Modifier.height(2.dp))
            TDText(
                text = stringResource(R.string.notifications_header_title),
                style = TDTheme.typography.heading1,
                color = TDTheme.colors.darkPending,
                isHeading = true,
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(12.dp))
        NotificationsMarkAllReadPill(enabled = hasUnread, onClick = onMarkAllRead)
    }
}

@Composable
private fun NotificationsMarkAllReadPill(enabled: Boolean, onClick: () -> Unit) {
    val containerColor = if (enabled) TDTheme.colors.lightPending else TDTheme.colors.lightGray.copy(alpha = 0.25f)
    val contentColor = if (enabled) TDTheme.colors.darkPending else TDTheme.colors.gray
    Row(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(containerColor)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(com.example.uikit.R.drawable.ic_double_check),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        TDText(
            text = stringResource(R.string.notifications_mark_all_read),
            style = TDTheme.typography.subheading4,
            color = contentColor,
            maxLines = 1,
        )
    }
}

@TDPreview
@Composable
private fun NotificationsHeaderPreview() {
    TDTheme {
        Column(modifier = Modifier.background(TDTheme.colors.background)) {
            NotificationsHeader(hasUnread = true, onMarkAllRead = {})
            NotificationsHeader(hasUnread = false, onMarkAllRead = {})
        }
    }
}
