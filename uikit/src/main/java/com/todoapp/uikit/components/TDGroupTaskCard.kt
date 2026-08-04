package com.todoapp.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.todoapp.uikit.image.rememberPixelImageModel
import com.todoapp.uikit.image.tdPixelFilterQuality
import com.todoapp.uikit.modifier.tdDropShadow
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme
import com.todoapp.uikit.theme.tdOutlineColor

@Composable
fun TDGroupTaskCard(
    title: String,
    priority: String?,
    deadlinePrimary: String,
    deadlineSecondary: String?,
    deadlineColor: Color,
    statusTone: TDStatusChipTone,
    statusLabel: String,
    assigneeName: String?,
    assigneeAvatarUrl: String?,
    assigneeInitials: String,
    unassignedLabel: String,
    modifier: Modifier = Modifier,
    photoUrl: String? = null,
    isCompleted: Boolean = false,
    onClick: (() -> Unit)? = null,
    onPhotoClick: (() -> Unit)? = null,
) {
    val contentAlpha = if (isCompleted) 0.5f else 1f
    val titleDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None

    val cardShape = TDTheme.shapes.xLarge
    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .tdDropShadow(
                elevation = 12.dp,
                shape = cardShape,
                ambientColor = TDTheme.colors.purple.copy(alpha = 0.18f),
            )
            .clip(cardShape)
            .background(TDTheme.colors.lightPending)
            .border(
                width = TDTheme.style.borderWidth,
                color = tdOutlineColor(TDTheme.colors.lightPurple.copy(alpha = 0.3f)),
                shape = cardShape,
            )
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!priority.isNullOrBlank()) {
                    TDPriorityBadge(priority = priority)
                }
                Spacer(modifier = Modifier.weight(1f))
                TDStatusChip(tone = statusTone, label = statusLabel)
            }

            TDText(
                text = title,
                style =
                TDTheme.typography.heading4.copy(
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = titleDecoration,
                ),
                color = TDTheme.colors.onBackground.copy(alpha = contentAlpha),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

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

            AssigneeRow(
                assigneeName = assigneeName,
                assigneeAvatarUrl = assigneeAvatarUrl,
                assigneeInitials = assigneeInitials,
                unassignedLabel = unassignedLabel,
                contentAlpha = contentAlpha,
            )
        }
    }
}

@Composable
internal fun TaskPhotoBanner(
    photoUrl: String,
    contentAlpha: Float,
    onPhotoClick: (() -> Unit)?,
) {
    Box(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(TDTheme.colors.bgColorPurple)
            .then(if (onPhotoClick != null) Modifier.clickable { onPhotoClick() } else Modifier),
    ) {
        AsyncImage(
            model = photoUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = contentAlpha,
        )
    }
}

@Composable
private fun AssigneeRow(
    assigneeName: String?,
    assigneeAvatarUrl: String?,
    assigneeInitials: String,
    unassignedLabel: String,
    contentAlpha: Float,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AssigneeAvatar(
            avatarUrl = assigneeAvatarUrl,
            initials = assigneeInitials,
            contentAlpha = contentAlpha,
        )
        TDText(
            text = assigneeName?.takeIf { it.isNotBlank() } ?: unassignedLabel,
            style = TDTheme.typography.subheading1.copy(fontWeight = FontWeight.Medium),
            color = TDTheme.colors.onBackground.copy(alpha = 0.75f * contentAlpha),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AssigneeAvatar(
    avatarUrl: String?,
    initials: String,
    contentAlpha: Float,
) {
    Box(
        modifier =
        Modifier
            .size(32.dp)
            .clip(TDTheme.shapes.circle)
            .background(TDTheme.colors.lightPurple.copy(alpha = 0.35f * contentAlpha))
            .border(
                width = TDTheme.style.borderWidth,
                color = tdOutlineColor(TDTheme.colors.lightPurple.copy(alpha = 0.6f * contentAlpha)),
                shape = TDTheme.shapes.circle,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = rememberPixelImageModel(avatarUrl, 32.dp),
                filterQuality = tdPixelFilterQuality(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = contentAlpha,
            )
        } else {
            TDText(
                text = initials,
                style = TDTheme.typography.subheading1.copy(fontWeight = FontWeight.SemiBold),
                color = TDTheme.colors.darkPurple.copy(alpha = contentAlpha),
            )
        }
    }
}

@TDPreview
@Composable
private fun TDGroupTaskCardPendingPreview() {
    TDTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            TDGroupTaskCard(
                title = "Buy groceries for Sunday dinner",
                priority = "HIGH",
                deadlinePrimary = "Today, 7:00 PM",
                deadlineSecondary = "in 3 hours",
                deadlineColor = TDTheme.colors.crossRed,
                statusTone = TDStatusChipTone.Neutral,
                statusLabel = "Pending",
                assigneeName = "Berat Baran",
                assigneeAvatarUrl = null,
                assigneeInitials = "BB",
                unassignedLabel = "Unassigned",
            )
        }
    }
}

@TDPreview
@Composable
private fun TDGroupTaskCardCompletedPreview() {
    TDTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            TDGroupTaskCard(
                title = "Submit weekly report",
                priority = "MEDIUM",
                deadlinePrimary = "Yesterday, 6:00 PM",
                deadlineSecondary = null,
                deadlineColor = TDTheme.colors.darkGreen,
                statusTone = TDStatusChipTone.Success,
                statusLabel = "Done",
                assigneeName = "Ayse Y.",
                assigneeAvatarUrl = null,
                assigneeInitials = "AY",
                unassignedLabel = "Unassigned",
                isCompleted = true,
            )
        }
    }
}

@TDPreview
@Composable
private fun TDGroupTaskCardOverduePreview() {
    TDTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            TDGroupTaskCard(
                title = "Pay electricity bill",
                priority = "HIGH",
                deadlinePrimary = "2 days ago",
                deadlineSecondary = "Overdue",
                deadlineColor = TDTheme.colors.crossRed,
                statusTone = TDStatusChipTone.Danger,
                statusLabel = "Overdue",
                assigneeName = null,
                assigneeAvatarUrl = null,
                assigneeInitials = "?",
                unassignedLabel = "Unassigned",
            )
        }
    }
}

@TDPreview
@Composable
private fun TDGroupTaskCardWithPhotoPreview() {
    TDTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            TDGroupTaskCard(
                title = "Take out the trash",
                priority = "LOW",
                deadlinePrimary = "Tomorrow, 8:00 AM",
                deadlineSecondary = "in 14 hours",
                deadlineColor = TDTheme.colors.darkPending,
                statusTone = TDStatusChipTone.Neutral,
                statusLabel = "Pending",
                assigneeName = "Mehmet K.",
                assigneeAvatarUrl = null,
                assigneeInitials = "MK",
                unassignedLabel = "Unassigned",
                photoUrl = "https://example.com/photo.jpg",
            )
        }
    }
}

@TDPreview
@Composable
private fun TDGroupTaskCardNoPriorityPreview() {
    TDTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            TDGroupTaskCard(
                title = "Walk the dog",
                priority = null,
                deadlinePrimary = "Today, 5:00 PM",
                deadlineSecondary = null,
                deadlineColor = TDTheme.colors.darkPending,
                statusTone = TDStatusChipTone.Neutral,
                statusLabel = "Pending",
                assigneeName = "Fatma D.",
                assigneeAvatarUrl = null,
                assigneeInitials = "FD",
                unassignedLabel = "Unassigned",
            )
        }
    }
}

@TDPreview
@Composable
private fun TDGroupTaskCardLongTitlePreview() {
    TDTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            TDGroupTaskCard(
                title = "Prepare comprehensive end-of-quarter financial review with all stakeholders",
                priority = "HIGH",
                deadlinePrimary = "Friday, 2:00 PM",
                deadlineSecondary = "in 3 days",
                deadlineColor = TDTheme.colors.orange,
                statusTone = TDStatusChipTone.Neutral,
                statusLabel = "Pending",
                assigneeName = "Ali V.",
                assigneeAvatarUrl = null,
                assigneeInitials = "AV",
                unassignedLabel = "Unassigned",
            )
        }
    }
}
