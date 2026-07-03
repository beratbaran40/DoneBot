package com.todoapp.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

@Composable
fun TDTaskCard(
    taskTitle: String,
    deadlinePrimary: String,
    deadlineSecondary: String?,
    deadlineColor: Color,
    statusTone: TDStatusChipTone,
    statusLabel: String,
    modifier: Modifier = Modifier,
    isCompleted: Boolean = false,
    description: String? = null,
    photoUrl: String? = null,
    onClick: () -> Unit = {},
    onPhotoClick: (() -> Unit)? = null,
    locationLabel: String? = null,
    onLocationClick: (() -> Unit)? = null,
    subtaskTotal: Int = 0,
    subtaskDone: Int = 0,
    isPendingSync: Boolean = false,
) {
    val contentAlpha = if (isCompleted) 0.5f else 1f
    val titleDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None

    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = TDTheme.colors.purple.copy(alpha = 0.18f),
                spotColor = TDTheme.colors.purple.copy(alpha = 0.18f),
            )
            .clip(RoundedCornerShape(20.dp))
            .background(TDTheme.colors.lightPending)
            .border(
                width = 1.dp,
                color = TDTheme.colors.lightPurple.copy(alpha = 0.3f),
                shape = RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick),
    ) {
        if (!photoUrl.isNullOrBlank()) {
            TaskPhotoBanner(
                photoUrl = photoUrl,
                contentAlpha = contentAlpha,
                onPhotoClick = onPhotoClick,
            )
        }
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InnerContent(
                taskTitle = taskTitle,
                titleDecoration = titleDecoration,
                contentAlpha = contentAlpha,
                statusTone = statusTone,
                statusLabel = statusLabel,
                description = description,
                deadlinePrimary = deadlinePrimary,
                deadlineSecondary = deadlineSecondary,
                deadlineColor = deadlineColor,
                locationLabel = locationLabel,
                onLocationClick = onLocationClick,
                isPendingSync = isPendingSync,
            )
            if (subtaskTotal > 0) {
                TDSubtaskProgress(
                    completed = subtaskDone,
                    total = subtaskTotal,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun InnerContent(
    taskTitle: String,
    titleDecoration: TextDecoration,
    contentAlpha: Float,
    statusTone: TDStatusChipTone,
    statusLabel: String,
    description: String?,
    deadlinePrimary: String,
    deadlineSecondary: String?,
    deadlineColor: Color,
    locationLabel: String?,
    onLocationClick: (() -> Unit)?,
    isPendingSync: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TDText(
                modifier = Modifier.weight(1f),
                text = taskTitle,
                style =
                TDTheme.typography.heading4.copy(
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = titleDecoration,
                ),
                color = TDTheme.colors.onBackground.copy(alpha = contentAlpha),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            TDStatusChip(tone = statusTone, label = statusLabel)
            if (isPendingSync) {
                Icon(
                    painter = painterResource(R.drawable.ic_refresh),
                    contentDescription = stringResource(R.string.cd_sync_pending),
                    tint = TDTheme.colors.pendingGray,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        if (!description.isNullOrBlank()) {
            TDText(
                text = description,
                style = TDTheme.typography.subheading1,
                color = TDTheme.colors.onBackground.copy(alpha = 0.55f * contentAlpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            TDText(
                text = deadlinePrimary,
                style = TDTheme.typography.heading6.copy(fontWeight = FontWeight.SemiBold),
                color = deadlineColor.copy(alpha = contentAlpha),
            )
            if (!deadlineSecondary.isNullOrBlank()) {
                TDText(
                    text = deadlineSecondary,
                    style = TDTheme.typography.subheading1,
                    color = TDTheme.colors.onBackground.copy(alpha = 0.55f * contentAlpha),
                )
            }
        }

        if (!locationLabel.isNullOrBlank()) {
            val pillModifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(TDTheme.colors.bgColorPurple)
                .let { base -> if (onLocationClick != null) base.clickable(onClick = onLocationClick) else base }
                .padding(horizontal = 8.dp, vertical = 4.dp)
            Row(modifier = pillModifier, verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_pin),
                    contentDescription = null,
                    tint = TDTheme.colors.darkPending.copy(alpha = contentAlpha),
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(4.dp))
                TDText(
                    text = locationLabel,
                    color = TDTheme.colors.darkPending.copy(alpha = contentAlpha),
                    style = TDTheme.typography.subheading2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@TDPreview
@Composable
fun TDTaskCardPreview() {
    TDTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TDTaskCard(
                taskTitle = "Read Book",
                description = "Chapter 5 of Clean Code",
                deadlinePrimary = "in 2h 15m",
                deadlineSecondary = "Apr 26 · 18:00",
                deadlineColor = TDTheme.colors.onBackground,
                statusTone = TDStatusChipTone.Neutral,
                statusLabel = "Pending",
            )
            TDTaskCard(
                taskTitle = "Morning Run",
                description = "5km around the park",
                deadlinePrimary = "1 hour ago",
                deadlineSecondary = "Apr 23 · 09:00",
                deadlineColor = TDTheme.colors.darkGreen,
                statusTone = TDStatusChipTone.Success,
                statusLabel = "Done",
                isCompleted = true,
            )
            TDTaskCard(
                taskTitle = "Submit report",
                deadlinePrimary = "3 hours ago · still pending",
                deadlineSecondary = "Apr 24 · 14:00",
                deadlineColor = TDTheme.colors.crossRed,
                statusTone = TDStatusChipTone.Danger,
                statusLabel = "Overdue",
                isPendingSync = true,
            )
        }
    }
}

@TDPreview
@Composable
private fun TDTaskCardRichPreview() {
    TDTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Staged task with subtask progress
            TDTaskCard(
                taskTitle = "Plan the trip",
                description = "Lock the itinerary",
                deadlinePrimary = "Tomorrow",
                deadlineSecondary = "Apr 27 · 10:00",
                deadlineColor = TDTheme.colors.onBackground,
                statusTone = TDStatusChipTone.Neutral,
                statusLabel = "Pending",
                subtaskTotal = 5,
                subtaskDone = 2,
            )
            // Task with a tappable location pill
            TDTaskCard(
                taskTitle = "Dentist appointment",
                deadlinePrimary = "in 30m",
                deadlineSecondary = "Apr 26 · 15:00",
                deadlineColor = TDTheme.colors.onBackground,
                statusTone = TDStatusChipTone.Neutral,
                statusLabel = "Pending",
                locationLabel = "Acıbadem Hospital",
                onLocationClick = {},
            )
            // Completed staged task with a photo banner (the image is absent in previews)
            TDTaskCard(
                taskTitle = "Birthday cake",
                description = "Pick up from the bakery",
                deadlinePrimary = "Done",
                deadlineSecondary = "Apr 26 · 17:00",
                deadlineColor = TDTheme.colors.darkGreen,
                statusTone = TDStatusChipTone.Success,
                statusLabel = "Done",
                isCompleted = true,
                photoUrl = "https://example.com/cake.jpg",
                subtaskTotal = 3,
                subtaskDone = 3,
            )
        }
    }
}
