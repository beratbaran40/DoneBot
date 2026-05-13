package com.todoapp.mobile.ui.groups.groupdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.ui.groups.groupdetail.GroupDetailContract.GroupActivityUiItem
import com.todoapp.mobile.ui.groups.groupdetail.GroupDetailContract.UiState
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.theme.TDTheme

@Composable
fun GroupDetailActivityTab(uiState: UiState.Success) {
    LazyColumn(
        modifier =
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        items(uiState.activities, key = { it.id }) { activity ->
            ActivityItem(activity = activity)
            Spacer(modifier = Modifier.height(12.dp))
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun GroupDetailActivityTabPreview() {
    TDTheme {
        GroupDetailActivityTab(
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
                selectedTab = 2,
                currentUserRole = "MEMBER",
            ),
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun GroupDetailActivityTabEmptyPreview() {
    TDTheme {
        GroupDetailActivityTab(
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
                activities = emptyList(),
                selectedTab = 2,
                currentUserRole = "MEMBER",
            ),
        )
    }
}

@Composable
private fun ActivityItem(activity: GroupActivityUiItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        MemberAvatar(
            initials = activity.actorInitials,
            size = 40,
            avatarUrl = activity.actorAvatarUrl,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    TDText(
                        text = activity.actorName,
                        style = TDTheme.typography.subheading2,
                        color = TDTheme.colors.onBackground,
                    )
                    TDText(
                        text = activity.description,
                        style = TDTheme.typography.subheading1,
                        color = TDTheme.colors.gray,
                    )
                }
                TDText(
                    text = activity.relativeTime,
                    style = TDTheme.typography.subheading1,
                    color = TDTheme.colors.gray,
                )
            }

            activity.taskTitle?.let { taskTitle ->
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TDTheme.colors.lightPending)
                        .padding(12.dp),
                ) {
                    TDText(
                        text = taskTitle,
                        style = TDTheme.typography.subheading3,
                        color = TDTheme.colors.darkPending,
                    )
                }
            }
        }
    }
}
