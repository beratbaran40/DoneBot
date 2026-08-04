package com.todoapp.mobile.ui.notifications

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.model.Notification
import com.todoapp.mobile.domain.model.NotificationType
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.theme.TDTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
internal fun NotificationsList(
    items: List<Notification>,
    onItemTap: (Notification) -> Unit,
    onDelete: (Notification) -> Unit,
    onAcceptInvitation: (Notification) -> Unit,
) {
    val buckets = bucketize(items)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        notificationSection(
            keyPrefix = "today",
            titleRes = R.string.notifications_section_today,
            section = NotificationSection.TODAY,
            sectionItems = buckets.today,
            onItemTap = onItemTap,
            onDelete = onDelete,
            onAcceptInvitation = onAcceptInvitation,
        )
        notificationSection(
            keyPrefix = "yesterday",
            titleRes = R.string.notifications_section_yesterday,
            section = NotificationSection.YESTERDAY,
            sectionItems = buckets.yesterday,
            onItemTap = onItemTap,
            onDelete = onDelete,
            onAcceptInvitation = onAcceptInvitation,
        )
        notificationSection(
            keyPrefix = "earlier",
            titleRes = R.string.notifications_section_earlier,
            section = NotificationSection.EARLIER,
            sectionItems = buckets.earlier,
            onItemTap = onItemTap,
            onDelete = onDelete,
            onAcceptInvitation = onAcceptInvitation,
        )
    }
}

private fun LazyListScope.notificationSection(
    keyPrefix: String,
    @StringRes titleRes: Int,
    section: NotificationSection,
    sectionItems: List<Notification>,
    onItemTap: (Notification) -> Unit,
    onDelete: (Notification) -> Unit,
    onAcceptInvitation: (Notification) -> Unit,
) {
    if (sectionItems.isEmpty()) return
    item(key = "section-$keyPrefix") {
        NotificationsSectionHeader(text = stringResource(titleRes))
    }
    items(items = sectionItems, key = { "$keyPrefix-${it.id}" }) { item ->
        SwipeableNotificationCard(
            notification = item,
            section = section,
            onTap = { onItemTap(item) },
            onDelete = { onDelete(item) },
            onAcceptInvitation = { onAcceptInvitation(item) },
        )
    }
}

@Composable
private fun SwipeableNotificationCard(
    notification: Notification,
    section: NotificationSection,
    onTap: () -> Unit,
    onDelete: () -> Unit,
    onAcceptInvitation: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.EndToStart -> {
                onDelete()
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }
            SwipeToDismissBoxValue.StartToEnd -> {
                onAcceptInvitation()
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }
            SwipeToDismissBoxValue.Settled -> {}
        }
    }
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = notification.type == NotificationType.INVITATION_RECEIVED,
        enableDismissFromEndToStart = true,
        backgroundContent = { NotificationSwipeBackground(direction = dismissState.dismissDirection) },
    ) {
        NotificationCard(
            notification = notification,
            section = section,
            onClick = onTap,
        )
    }
}

@Composable
private fun NotificationSwipeBackground(direction: SwipeToDismissBoxValue) {
    val shape = TDTheme.shapes.large
    when (direction) {
        SwipeToDismissBoxValue.EndToStart -> Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(TDTheme.colors.crossRed),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Icon(
                painter = tdPainter(com.example.uikit.R.drawable.ic_delete),
                contentDescription = null,
                tint = TDTheme.colors.white,
                modifier = Modifier
                    .padding(end = 20.dp)
                    .size(22.dp),
            )
        }
        SwipeToDismissBoxValue.StartToEnd -> Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(if (TDTheme.isDark) TDTheme.colors.mediumGreen else TDTheme.colors.darkGreen),
            contentAlignment = Alignment.CenterStart,
        ) {
            Icon(
                painter = tdPainter(com.example.uikit.R.drawable.ic_check),
                contentDescription = null,
                tint = TDTheme.colors.white,
                modifier = Modifier
                    .padding(start = 20.dp)
                    .size(22.dp),
            )
        }
        SwipeToDismissBoxValue.Settled -> {}
    }
}

@Composable
private fun NotificationsSectionHeader(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TDText(
            text = text,
            style = TDTheme.typography.heading7,
            color = TDTheme.colors.onBackground,
            isHeading = true,
        )
        HorizontalDivider(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            thickness = 1.dp,
            color = TDTheme.colors.lightGray.copy(alpha = 0.5f),
        )
    }
}

private data class NotificationBuckets(
    val today: List<Notification>,
    val yesterday: List<Notification>,
    val earlier: List<Notification>,
)

private fun bucketize(items: List<Notification>): NotificationBuckets {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    val grouped = items.groupBy { item ->
        when (Instant.ofEpochMilli(item.createdAt).atZone(zone).toLocalDate()) {
            today -> NotificationSection.TODAY
            yesterday -> NotificationSection.YESTERDAY
            else -> NotificationSection.EARLIER
        }
    }
    return NotificationBuckets(
        today = grouped[NotificationSection.TODAY].orEmpty(),
        yesterday = grouped[NotificationSection.YESTERDAY].orEmpty(),
        earlier = grouped[NotificationSection.EARLIER].orEmpty(),
    )
}
