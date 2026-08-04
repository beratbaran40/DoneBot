package com.todoapp.mobile.ui.groups.groupdetail

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.todoapp.mobile.BuildConfig
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.groups.groupdetail.GroupDetailContract.GroupMemberUiItem
import com.todoapp.mobile.ui.groups.groupdetail.GroupDetailContract.UiAction
import com.todoapp.mobile.ui.groups.groupdetail.GroupDetailContract.UiState
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDButtonSize
import com.todoapp.uikit.components.TDButtonType
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.image.rememberPixelImageModel
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.image.tdPixelFilterQuality
import com.todoapp.uikit.theme.TDTheme
import com.example.uikit.R as UiKitR

@Composable
fun GroupDetailMembersTab(
    uiState: UiState.Success,
    onAction: (UiAction) -> Unit,
) {
    LazyColumn(
        modifier =
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TDText(
                    text = "${uiState.memberCount} ${stringResource(R.string.members).uppercase()}",
                    style = TDTheme.typography.subheading1,
                    color = TDTheme.colors.gray,
                )
                if (uiState.currentUserRole == "ADMIN") {
                    TDButton(
                        text = stringResource(R.string.invite),
                        type = TDButtonType.PRIMARY,
                        size = TDButtonSize.SMALL,
                        onClick = { onAction(UiAction.OnInviteTap) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        items(uiState.members, key = { it.userId }) { member ->
            MemberCard(
                member = member,
                isAdmin = uiState.currentUserRole == "ADMIN",
                onRemove = { onAction(UiAction.OnRemoveMemberTap(member.userId)) },
                onClick = { onAction(UiAction.OnMemberTap(member.userId)) },
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (uiState.pendingInvites.isNotEmpty()) {
            item { GroupDetailPendingInvitesSection(invites = uiState.pendingInvites) }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun MemberCard(
    member: GroupMemberUiItem,
    isAdmin: Boolean,
    onRemove: () -> Unit,
    onClick: () -> Unit,
) {
    Column(
        modifier =
        Modifier
            .border(
                width = 2.dp,
                shape = TDTheme.shapes.medium,
                color = TDTheme.colors.pendingGray,
            ).fillMaxWidth()
            .clip(TDTheme.shapes.medium)
            .background(TDTheme.colors.background)
            .then(if (!member.isCurrentUser) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            MemberAvatar(
                initials = member.initials,
                avatarUrl = member.avatarUrl,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TDText(
                        text =
                        if (member.isCurrentUser) {
                            "${member.displayName} (${stringResource(
                                R.string.you,
                            )})"
                        } else {
                            member.displayName
                        },
                        style = TDTheme.typography.subheading1,
                        color = TDTheme.colors.onBackground,
                    )
                }
                TDText(
                    text = member.email,
                    style = TDTheme.typography.subheading1,
                    color = TDTheme.colors.gray,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RoleBadge(role = member.role)
                    TDText(
                        text = "• ${member.joinedAt}",
                        style = TDTheme.typography.subheading1,
                        color = TDTheme.colors.gray,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (member.pendingTaskCount == 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        painter = tdPainter(UiKitR.drawable.ic_tasks_done),
                        contentDescription = null,
                        tint = TDTheme.colors.darkGreen,
                        modifier = Modifier.size(16.dp),
                    )
                    TDText(
                        text = stringResource(R.string.all_caught_up),
                        style = TDTheme.typography.subheading1,
                        color = TDTheme.colors.darkGreen,
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        painter = tdPainter(UiKitR.drawable.ic_sand_clock),
                        contentDescription = null,
                        tint = TDTheme.colors.pendingGray,
                        modifier = Modifier.size(16.dp),
                    )
                    TDText(
                        text = "${member.pendingTaskCount} ${stringResource(R.string.pending_tasks)}",
                        style = TDTheme.typography.subheading1,
                        color = TDTheme.colors.pendingGray,
                    )
                }
            }
        }
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun GroupDetailMembersTabAdminPreview() {
    TDTheme {
        GroupDetailMembersTab(
            uiState =
            UiState.Success(
                groupId = 1L,
                groupName = "The Smith Family",
                description = "Daily chores, grocery lists, and vacation planning for 2024.",
                memberCount = 3,
                completedCount = 5,
                pendingCount = 3,
                tasks = mockGroupTasks,
                members = mockGroupMembers,
                pendingInvites = mockPendingInvites,
                activities = mockGroupActivities,
                selectedTab = 1,
                currentUserRole = "ADMIN",
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun GroupDetailMembersTabMemberPreview() {
    TDTheme {
        GroupDetailMembersTab(
            uiState =
            UiState.Success(
                groupId = 1L,
                groupName = "The Smith Family",
                description = "",
                memberCount = 3,
                completedCount = 5,
                pendingCount = 3,
                tasks = mockGroupTasks,
                members = mockGroupMembers,
                activities = mockGroupActivities,
                selectedTab = 1,
                currentUserRole = "MEMBER",
            ),
            onAction = {},
        )
    }
}

@Composable
fun MemberAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    size: Int = 44,
    avatarUrl: String? = null,
    avatarVersion: Long = 0L,
) {
    val absoluteUrl =
        remember(avatarUrl, avatarVersion) {
            if (avatarUrl.isNullOrBlank()) {
                null
            } else {
                val base =
                    BuildConfig.BASE_URL
                        .trimEnd('/')
                val rel = avatarUrl.trimStart('/')
                // avatarVersion == 0L → no cache-buster (other-member avatars never change mid-session);
                // the current user's own avatar passes a bumped token to force a refetch after upload.
                if (avatarVersion != 0L) "$base/$rel?v=$avatarVersion" else "$base/$rel"
            }
        }
    Box(
        modifier =
        modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(TDTheme.colors.pendingGray),
        contentAlignment = Alignment.Center,
    ) {
        if (absoluteUrl != null) {
            AsyncImage(
                model = rememberPixelImageModel(absoluteUrl, size.dp),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                filterQuality = tdPixelFilterQuality(),
                modifier = Modifier.size(size.dp).clip(TDTheme.shapes.circle),
            )
        } else {
            TDText(
                text = initials.take(2),
                style = TDTheme.typography.heading3,
                color = TDTheme.colors.white,
            )
        }
    }
}

@Composable
fun RoleBadge(role: String) {
    val isAdmin = role.uppercase() == "ADMIN"
    Box(
        modifier =
        Modifier
            .clip(TDTheme.shapes.tiny)
            .background(if (isAdmin) TDTheme.colors.orange else TDTheme.colors.pendingGray.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        TDText(
            text = if (isAdmin) stringResource(R.string.admin) else stringResource(R.string.member_role),
            style = TDTheme.typography.subheading1,
            color = if (isAdmin) TDTheme.colors.black else TDTheme.colors.onBackground,
        )
    }
}
