package com.todoapp.mobile.ui.groups.grouptaskdetail

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

class GroupTaskDetailPreviewProvider : PreviewParameterProvider<GroupTaskDetailContract.UiState> {
    override val values: Sequence<GroupTaskDetailContract.UiState> =
        sequenceOf(
            GroupTaskDetailContract.UiState.Loading,
            GroupTaskDetailContract.UiState.Error("Task not found"),
            // Incomplete task – all metadata
            GroupTaskDetailContract.UiState.Success(
                task =
                GroupTaskDetailContract.TaskUiModel(
                    id = 1L,
                    title = "Buy groceries for the week",
                    description = "Milk, eggs, bread, fresh vegetables from the farmers market",
                    priority = "HIGH",
                    dueTime = "Today, 6:00 PM",
                    rawDueDate = null,
                    isCompleted = false,
                    assigneeName = "Alice Smith",
                    assigneeInitials = "AS",
                    assigneeAvatarUrl = null,
                    assigneeUserId = 1L,
                    isAssignedToMe = true,
                    canDelete = true,
                    canComplete = true,
                ),
                groupName = "The Smith Family",
            ),
            // Completed task – all metadata
            GroupTaskDetailContract.UiState.Success(
                task =
                GroupTaskDetailContract.TaskUiModel(
                    id = 2L,
                    title = "Schedule dentist appointment",
                    description = "Annual check-up for the whole family",
                    priority = "MEDIUM",
                    dueTime = "Apr 20, 2025, 9:00 AM",
                    rawDueDate = null,
                    isCompleted = true,
                    assigneeName = "Bob Smith",
                    assigneeInitials = "BS",
                    assigneeAvatarUrl = null,
                    assigneeUserId = 2L,
                    isAssignedToMe = false,
                    canDelete = false,
                    canComplete = false,
                ),
                groupName = "The Smith Family",
            ),
            // Minimal task – no description, no assignee, no priority, no due time
            GroupTaskDetailContract.UiState.Success(
                task =
                GroupTaskDetailContract.TaskUiModel(
                    id = 3L,
                    title = "Fix the leaky faucet",
                    description = null,
                    priority = null,
                    dueTime = null,
                    rawDueDate = null,
                    isCompleted = false,
                    assigneeName = null,
                    assigneeInitials = null,
                    assigneeAvatarUrl = null,
                    assigneeUserId = null,
                    isAssignedToMe = false,
                    canDelete = false,
                    canComplete = false,
                ),
                groupName = "Work Project",
            ),
        )
}
