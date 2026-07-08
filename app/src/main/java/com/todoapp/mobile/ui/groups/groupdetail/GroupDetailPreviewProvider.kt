package com.todoapp.mobile.ui.groups.groupdetail

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.todoapp.mobile.ui.home.TaskFormState

class GroupDetailPreviewProvider : PreviewParameterProvider<GroupDetailContract.UiState> {
    override val values: Sequence<GroupDetailContract.UiState> =
        sequenceOf(
            GroupDetailContract.UiState.Loading,
            GroupDetailContract.UiState.Error("Failed to load group details"),
            // Overview tab – with tasks, filter ALL, ADMIN role
            GroupDetailContract.UiState.Success(
                groupId = 1L,
                groupName = "The Smith Family",
                description = "Daily chores, grocery lists, and vacation planning for 2024.",
                memberCount = 3,
                completedCount = 5,
                pendingCount = 3,
                tasks = mockGroupTasks,
                members = mockGroupMembers,
                activities = mockGroupActivities,
                selectedTab = 0,
                taskFilter = GroupDetailContract.TaskFilter.ALL,
                currentUserRole = "ADMIN",
            ),
            // Overview tab – empty tasks, ASSIGNED_TO_ME filter, MEMBER role
            GroupDetailContract.UiState.Success(
                groupId = 1L,
                groupName = "The Smith Family",
                description = "Daily chores, grocery lists, and vacation planning for 2024.",
                memberCount = 3,
                completedCount = 5,
                pendingCount = 0,
                tasks = emptyList(),
                members = mockGroupMembers,
                activities = mockGroupActivities,
                selectedTab = 0,
                taskFilter = GroupDetailContract.TaskFilter.ASSIGNED_TO_ME,
                currentUserRole = "MEMBER",
            ),
            // Members tab – ADMIN (Invite + Remove buttons visible)
            GroupDetailContract.UiState.Success(
                groupId = 1L,
                groupName = "The Smith Family",
                description = "Daily chores, grocery lists, and vacation planning for 2024.",
                memberCount = 3,
                completedCount = 5,
                pendingCount = 3,
                tasks = mockGroupTasks,
                members = mockGroupMembers,
                activities = mockGroupActivities,
                selectedTab = 1,
                currentUserRole = "ADMIN",
            ),
            // Members tab – MEMBER role (no invite/remove actions)
            GroupDetailContract.UiState.Success(
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
            // Activity tab
            GroupDetailContract.UiState.Success(
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
            // Task sheet open
            GroupDetailContract.UiState.Success(
                groupId = 1L,
                groupName = "The Smith Family",
                description = "Daily chores.",
                memberCount = 3,
                completedCount = 5,
                pendingCount = 3,
                tasks = mockGroupTasks,
                members = mockGroupMembers,
                activities = mockGroupActivities,
                selectedTab = 0,
                currentUserRole = "ADMIN",
                isTaskSheetOpen = true,
                taskFormState = TaskFormState(taskTitle = "Buy groceries"),
            ),
        )
}

val mockGroupTasks =
    listOf(
        GroupDetailContract.GroupTaskUiItem(
            id = 1L,
            title = "Buy groceries for the week",
            description = "Milk, eggs, bread, vegetables",
            assigneeId = 1L,
            assigneeAvatarUrl = null,
            assigneeName = "Alice Smith",
            assigneeInitials = "AS",
            dueTime = "Today, 6:00 PM",
            rawDueDate = null,
            priority = "HIGH",
            isCompleted = false,
            isAssignedToMe = true,
            canDelete = true,
        ),
        GroupDetailContract.GroupTaskUiItem(
            id = 2L,
            title = "Schedule dentist appointment",
            description = null,
            assigneeId = 2L,
            assigneeAvatarUrl = null,
            assigneeName = "Bob Smith",
            assigneeInitials = "BS",
            dueTime = "Tomorrow, 9:00 AM",
            rawDueDate = null,
            priority = "MEDIUM",
            isCompleted = true,
            isAssignedToMe = false,
            canDelete = false,
        ),
        GroupDetailContract.GroupTaskUiItem(
            id = 3L,
            title = "Fix the leaky faucet",
            description = "Kitchen sink, needs new washer",
            assigneeId = null,
            assigneeAvatarUrl = null,
            assigneeName = null,
            assigneeInitials = null,
            dueTime = null,
            rawDueDate = null,
            priority = "DAILY",
            isCompleted = false,
            isAssignedToMe = false,
            canDelete = false,
        ),
    )

val mockGroupMembers =
    listOf(
        GroupDetailContract.GroupMemberUiItem(
            userId = 1L,
            displayName = "Alice Smith",
            email = "alice@example.com",
            avatarUrl = null,
            initials = "AS",
            role = "ADMIN",
            joinedAt = "Jan 12, 2023",
            pendingTaskCount = 3,
            isCurrentUser = true,
        ),
        GroupDetailContract.GroupMemberUiItem(
            userId = 2L,
            displayName = "Bob Smith",
            email = "bob@example.com",
            avatarUrl = null,
            initials = "BS",
            role = "MEMBER",
            joinedAt = "Feb 05, 2023",
            pendingTaskCount = 0,
            isCurrentUser = false,
        ),
        GroupDetailContract.GroupMemberUiItem(
            userId = 3L,
            displayName = "Carol Smith",
            email = "carol@example.com",
            avatarUrl = null,
            initials = "CS",
            role = "MEMBER",
            joinedAt = "Mar 15, 2023",
            pendingTaskCount = 5,
            isCurrentUser = false,
        ),
    )

val mockPendingInvites =
    listOf(
        GroupDetailContract.PendingInviteUiItem(
            id = 1L,
            email = "dave@example.com",
            invitedAt = "6 Jul 2026",
        ),
        GroupDetailContract.PendingInviteUiItem(
            id = 2L,
            email = "esra@example.com",
            invitedAt = "8 Jul 2026",
        ),
    )

val mockGroupActivities =
    listOf(
        GroupDetailContract.GroupActivityUiItem(
            id = 1L,
            type = "TASK_CREATED",
            actorName = "Alice Smith",
            actorAvatarUrl = null,
            actorInitials = "AS",
            description = "created a new task",
            relativeTime = "2h ago",
            taskTitle = "Buy groceries for the week",
        ),
        GroupDetailContract.GroupActivityUiItem(
            id = 2L,
            type = "TASK_COMPLETED",
            actorName = "Bob Smith",
            actorAvatarUrl = null,
            actorInitials = "BS",
            description = "completed a task",
            relativeTime = "1d ago",
            taskTitle = "Schedule dentist appointment",
        ),
        GroupDetailContract.GroupActivityUiItem(
            id = 3L,
            type = "MEMBER_JOINED",
            actorName = "Carol Smith",
            actorAvatarUrl = null,
            actorInitials = "CS",
            description = "joined the group",
            relativeTime = "3d ago",
            taskTitle = null,
        ),
    )
