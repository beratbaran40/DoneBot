package com.todoapp.mobile.ui.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.todoapp.mobile.BuildConfig
import com.todoapp.mobile.R
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.image.rememberPixelImageModel
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.image.tdPixelFilterQuality
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme
import com.todoapp.uikit.theme.tdOutlineColor

/**
 * Horizontal avatar picker for assigning a group task to a member. The first chip is an explicit
 * "Unassigned" option (selected when [selectedAssigneeId] is null) so a group-wide task is an
 * intentional, visible choice. Tapping the selected member returns to unassigned.
 */
@Composable
fun GroupTaskAssigneeSelector(
    members: List<AssigneeUi>,
    selectedAssigneeId: Long?,
    onAssigneeSelected: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        TDText(
            text = stringResource(R.string.assign_to),
            style = TDTheme.typography.heading3,
            color = TDTheme.colors.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(vertical = 8.dp),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            item(key = "unassigned") {
                UnassignedChip(
                    selected = selectedAssigneeId == null,
                    onClick = { onAssigneeSelected(null) },
                )
            }
            items(members, key = { it.userId }) { member ->
                MemberChip(
                    member = member,
                    selected = member.userId == selectedAssigneeId,
                    onClick = {
                        onAssigneeSelected(if (member.userId == selectedAssigneeId) null else member.userId)
                    },
                )
            }
        }
    }
}

@Composable
private fun UnassignedChip(
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (selected) TDTheme.colors.pendingGray else TDTheme.colors.lightPending)
                .then(
                    if (selected) Modifier.border(2.dp, tdOutlineColor(TDTheme.colors.onBackground), CircleShape) else Modifier,
                ),
        ) {
            Icon(
                painter = tdPainter(R.drawable.ic_groups),
                contentDescription = null,
                tint = if (selected) TDTheme.colors.surface else TDTheme.colors.pendingGray,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        TDText(
            text = stringResource(R.string.creation_assignee_unassigned),
            style = TDTheme.typography.subheading4,
            color = if (selected) TDTheme.colors.pendingGray else TDTheme.colors.gray,
        )
    }
}

@Composable
private fun MemberChip(
    member: AssigneeUi,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        val absoluteAvatarUrl = member.avatarUrl?.takeIf { it.isNotBlank() }?.let {
            "${BuildConfig.BASE_URL.trimEnd('/')}/${it.trimStart('/')}"
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (selected) TDTheme.colors.pendingGray else TDTheme.colors.lightPending)
                .then(
                    if (selected) Modifier.border(2.dp, tdOutlineColor(TDTheme.colors.onBackground), CircleShape) else Modifier,
                ),
        ) {
            if (absoluteAvatarUrl != null) {
                AsyncImage(
                    model = rememberPixelImageModel(absoluteAvatarUrl, 40.dp),
                    filterQuality = tdPixelFilterQuality(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(40.dp).clip(CircleShape),
                )
            } else {
                TDText(
                    text = member.initials,
                    style = TDTheme.typography.subheading2,
                    color = if (selected) TDTheme.colors.surface else TDTheme.colors.pendingGray,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        TDText(
            text = member.displayName.split(" ").firstOrNull() ?: member.displayName,
            style = TDTheme.typography.subheading4,
            color = if (selected) TDTheme.colors.pendingGray else TDTheme.colors.gray,
        )
    }
}

@TDPreview
@Composable
private fun GroupTaskAssigneeSelectorPreview() {
    val members = listOf(
        AssigneeUi(userId = 1, displayName = "Ada Lovelace", avatarUrl = null, initials = "AL"),
        AssigneeUi(userId = 2, displayName = "Grace Hopper", avatarUrl = null, initials = "GH"),
        AssigneeUi(userId = 3, displayName = "Alan Turing", avatarUrl = null, initials = "AT"),
    )
    TDTheme {
        Column(
            modifier = Modifier
                .background(TDTheme.colors.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Unassigned (group-wide)
            GroupTaskAssigneeSelector(members = members, selectedAssigneeId = null, onAssigneeSelected = {})
            // A member selected
            GroupTaskAssigneeSelector(members = members, selectedAssigneeId = 2L, onAssigneeSelected = {})
        }
    }
}
