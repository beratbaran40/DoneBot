package com.todoapp.mobile.ui.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.todoapp.mobile.R
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

@Composable
internal fun NotificationsHeader(
    hasUnread: Boolean,
    showMarkAllUndo: Boolean,
    onMarkAllRead: () -> Unit,
    onUndoMarkAllRead: () -> Unit,
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
        Column(horizontalAlignment = Alignment.End) {
            NotificationsMarkAllReadPill(enabled = hasUnread, onClick = onMarkAllRead)
            AnimatedVisibility(
                visible = showMarkAllUndo,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                NotificationsUndoMarkAllText(onClick = onUndoMarkAllRead)
            }
        }
    }
}

@Composable
private fun NotificationsMarkAllReadPill(enabled: Boolean, onClick: () -> Unit) {
    val containerColor = if (enabled) TDTheme.colors.lightPending else TDTheme.colors.lightGray.copy(alpha = 0.25f)
    val contentColor = if (enabled) TDTheme.colors.darkPending else TDTheme.colors.gray
    Row(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(TDTheme.shapes.xLarge)
            .background(containerColor)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = tdPainter(com.example.uikit.R.drawable.ic_double_check),
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

@Composable
private fun NotificationsUndoMarkAllText(onClick: () -> Unit) {
    TDText(
        text = stringResource(R.string.notifications_undo_mark_all),
        style = TDTheme.typography.subheading3,
        color = TDTheme.colors.darkPending,
        maxLines = 1,
        modifier = Modifier
            .padding(top = 2.dp)
            .clip(TDTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

@TDPreview
@Composable
private fun NotificationsHeaderPreview() {
    TDTheme {
        Column(modifier = Modifier.background(TDTheme.colors.background)) {
            NotificationsHeader(
                hasUnread = true,
                showMarkAllUndo = false,
                onMarkAllRead = {},
                onUndoMarkAllRead = {},
            )
            NotificationsHeader(
                hasUnread = false,
                showMarkAllUndo = true,
                onMarkAllRead = {},
                onUndoMarkAllRead = {},
            )
            NotificationsHeader(
                hasUnread = false,
                showMarkAllUndo = false,
                onMarkAllRead = {},
                onUndoMarkAllRead = {},
            )
        }
    }
}
