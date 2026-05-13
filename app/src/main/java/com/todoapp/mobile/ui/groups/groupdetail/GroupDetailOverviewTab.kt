package com.todoapp.mobile.ui.groups.groupdetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.BuildConfig
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.groups.groupdetail.GroupDetailContract.GroupTaskUiItem
import com.todoapp.mobile.ui.groups.groupdetail.GroupDetailContract.TaskFilter
import com.todoapp.mobile.ui.groups.groupdetail.GroupDetailContract.UiAction
import com.todoapp.mobile.ui.groups.groupdetail.GroupDetailContract.UiState
import com.todoapp.mobile.ui.home.SecretOrNormalPhotoBanner
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDButtonType
import com.todoapp.uikit.components.TDPriorityBadge
import com.todoapp.uikit.components.TDTaskCardWithCheckbox
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.components.TDUndoSnackbar
import com.todoapp.uikit.theme.TDTheme
import com.example.uikit.R as UiKitR

@Composable
fun GroupDetailOverviewTab(
    uiState: UiState.Success,
    onAction: (UiAction) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                if (uiState.description.isNotBlank()) {
                    TDText(
                        text = uiState.description,
                        style = TDTheme.typography.subheading2,
                        color = TDTheme.colors.gray,
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                GroupStatsRow(
                    memberCount = uiState.memberCount,
                    completedCount = uiState.completedCount,
                    pendingCount = uiState.pendingCount,
                )
                Spacer(modifier = Modifier.height(16.dp))
                TaskFilterRow(
                    selectedFilter = uiState.taskFilter,
                    onFilterSelected = { onAction(UiAction.OnTaskFilterSelected(it)) },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            val displayedTasks =
                when (uiState.taskFilter) {
                    TaskFilter.ALL -> uiState.tasks
                    TaskFilter.ASSIGNED_TO_ME -> uiState.tasks.filter { it.isAssignedToMe }
                }.filter { it.id != uiState.undoDeleteTaskId }

            if (displayedTasks.isEmpty()) {
                item {
                    GroupTasksEmptyState()
                }
            } else {
                items(displayedTasks, key = { it.id }) { task ->
                    SwipeableGroupTaskCard(
                        task = task,
                        onChecked = { checked -> onAction(UiAction.OnTaskChecked(task.id, checked)) },
                        onLongPress = { onAction(UiAction.OnTaskLongPress(task.id)) },
                        onDelete = { onAction(UiAction.OnDeleteTask(task.id)) },
                        onAssignToMe = { onAction(UiAction.OnAssignToMe(task.id)) },
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        TDButton(
            modifier =
            Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            text = stringResource(R.string.new_task),
            type = TDButtonType.PRIMARY,
            onClick = { onAction(UiAction.OnNewTaskTap) },
        )

        AnimatedVisibility(
            modifier =
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            visible = uiState.undoDeleteTaskId != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            TDUndoSnackbar(
                message = stringResource(R.string.group_task_deleted),
                onUndo = { onAction(UiAction.OnUndoDeleteTask) },
            )
        }
    }
}

@Composable
private fun GroupTasksEmptyState() {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(UiKitR.drawable.ic_tasks_done),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = TDTheme.colors.lightGray,
        )
        Spacer(modifier = Modifier.height(16.dp))
        TDText(
            text = stringResource(R.string.no_tasks_yet),
            style = TDTheme.typography.subheading2,
            color = TDTheme.colors.gray,
        )
    }
}

@Composable
private fun GroupStatsRow(
    memberCount: Int,
    completedCount: Int,
    pendingCount: Int,
) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        GroupStatCard(
            modifier = Modifier.weight(1f),
            iconRes = UiKitR.drawable.ic_members,
            count = memberCount,
            cardBg = TDTheme.colors.groupMemberCardBgColor,
            iconBg = TDTheme.colors.gray,
            iconTint = TDTheme.colors.background,
            countColor = TDTheme.colors.onBackground,
        )
        GroupStatCard(
            iconRes = UiKitR.drawable.ic_tasks_done,
            count = completedCount,
            cardBg = TDTheme.colors.lightGreen,
            iconBg = TDTheme.colors.mediumGreen,
            iconTint = TDTheme.colors.darkGreen,
            countColor = TDTheme.colors.darkGreen,
            modifier = Modifier.weight(1f),
        )
        GroupStatCard(
            modifier = Modifier.weight(1f),
            iconRes = UiKitR.drawable.ic_sand_clock,
            count = pendingCount,
            cardBg = TDTheme.colors.lightPending,
            iconBg = TDTheme.colors.mediumPending,
            iconTint = TDTheme.colors.white,
            countColor = TDTheme.colors.pendingGray,
        )
    }
}

@Composable
private fun GroupStatCard(
    iconRes: Int,
    count: Int,
    cardBg: Color,
    iconBg: Color,
    iconTint: Color,
    countColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier =
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp),
            )
        }
        Column {
            TDText(
                text = count.toString(),
                style = TDTheme.typography.heading3,
                color = countColor,
            )
        }
    }
}

@Composable
private fun TaskFilterRow(
    selectedFilter: TaskFilter,
    onFilterSelected: (TaskFilter) -> Unit,
) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            text = stringResource(R.string.all),
            selected = selectedFilter == TaskFilter.ALL,
            onClick = { onFilterSelected(TaskFilter.ALL) },
        )
        FilterChip(
            text = stringResource(R.string.assigned_to_me),
            selected = selectedFilter == TaskFilter.ASSIGNED_TO_ME,
            onClick = { onFilterSelected(TaskFilter.ASSIGNED_TO_ME) },
        )
    }
}

@Composable
private fun FilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) TDTheme.colors.darkPending else TDTheme.colors.lightPending)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        TDText(
            text = text,
            style = TDTheme.typography.subheading3,
            color = if (selected) TDTheme.colors.background else TDTheme.colors.onBackground,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableGroupTaskCard(
    task: GroupTaskUiItem,
    onChecked: (Boolean) -> Unit,
    onLongPress: () -> Unit,
    onDelete: () -> Unit,
    onAssignToMe: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState()
    val hasAssignee = task.assigneeId != null
    val canUnassignOther = hasAssignee && !task.isAssignedToMe && task.canDelete
    val canStartToEndSwipe = !hasAssignee || task.isAssignedToMe || canUnassignOther

    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.EndToStart -> {
                if (task.canDelete) {
                    onDelete()
                }
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }

            SwipeToDismissBoxValue.StartToEnd -> {
                onAssignToMe()
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }

            SwipeToDismissBoxValue.Settled -> {}
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = canStartToEndSwipe,
        enableDismissFromEndToStart = task.canDelete,
        backgroundContent = {
            GroupTaskSwipeBackground(direction = dismissState.dismissDirection, hasAssignee = hasAssignee)
        },
    ) {
        FlippableGroupTaskCard(
            task = task,
            onChecked = onChecked,
            onLongPress = onLongPress,
        )
    }
}

@Composable
private fun GroupTaskSwipeBackground(
    direction: SwipeToDismissBoxValue,
    hasAssignee: Boolean,
) {
    val color by animateColorAsState(
        targetValue =
        when (direction) {
            SwipeToDismissBoxValue.EndToStart -> TDTheme.colors.crossRed
            SwipeToDismissBoxValue.StartToEnd -> if (hasAssignee) TDTheme.colors.lightOrange else TDTheme.colors.pendingGray
            else -> Color.Transparent
        },
        label = "group_swipe_bg",
    )
    val alignment =
        when (direction) {
            SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
            else -> Alignment.CenterStart
        }
    Box(
        modifier =
        Modifier
            .fillMaxSize()
            .background(color, RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp),
        contentAlignment = alignment,
    ) {
        when (direction) {
            SwipeToDismissBoxValue.EndToStart ->
                Icon(
                    painter = painterResource(UiKitR.drawable.ic_delete),
                    contentDescription = null,
                    tint = Color.White,
                )

            SwipeToDismissBoxValue.StartToEnd ->
                Icon(
                    painter = painterResource(UiKitR.drawable.ic_members),
                    contentDescription =
                    stringResource(
                        if (hasAssignee) R.string.unassign_task else R.string.assign_to_me,
                    ),
                    tint = if (hasAssignee) TDTheme.colors.orange else Color.White,
                )

            else -> {}
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FlippableGroupTaskCard(
    task: GroupTaskUiItem,
    onChecked: (Boolean) -> Unit,
    onLongPress: () -> Unit,
) {
    var isFlipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "cardFlip",
    )

    Box(
        modifier =
        Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { isFlipped = !isFlipped },
                onLongClick = { onLongPress() },
            ),
    ) {
        // Front face
        Box(
            modifier =
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                    alpha = if (rotation <= 90f) 1f else 0f
                },
        ) {
            // Pass no-op onChecked when flipped so hidden checkbox can't be triggered
            GroupTaskCard(task = task, onChecked = if (!isFlipped) onChecked else { _ -> })
        }

        // Back face — matchParentSize() ensures it never exceeds front face dimensions
        Box(
            modifier =
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    rotationY = rotation - 180f
                    cameraDistance = 12f * density
                    alpha = if (rotation > 90f) 1f else 0f
                },
        ) {
            AssigneeBackFace(task = task)
        }
    }
}

@Composable
private fun AssigneeBackFace(task: GroupTaskUiItem) {
    Row(
        modifier =
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(TDTheme.colors.infoCardBgColor)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            if (task.assigneeInitials != null || task.assigneeName != null) {
                MemberAvatar(
                    initials = task.assigneeInitials ?: task.assigneeName?.take(2)?.uppercase() ?: "?",
                    size = 32,
                    avatarUrl = task.assigneeAvatarUrl,
                )
                TDText(
                    text = task.assigneeName.orEmpty(),
                    style = TDTheme.typography.subheading2,
                    color = TDTheme.colors.onBackground,
                    maxLines = 1,
                )
            } else {
                Icon(
                    painter = painterResource(UiKitR.drawable.ic_members),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = TDTheme.colors.lightGray,
                )
                TDText(
                    text = stringResource(R.string.no_assignee),
                    style = TDTheme.typography.subheading2,
                    color = TDTheme.colors.gray,
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_calendar),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = TDTheme.colors.gray,
            )
            TDText(
                text = task.dueTime ?: stringResource(R.string.no_due_date),
                style = TDTheme.typography.subheading1,
                color = TDTheme.colors.gray,
            )
        }
    }
}

@Composable
private fun GroupTaskCard(
    task: GroupTaskUiItem,
    onChecked: (Boolean) -> Unit,
) {
    val firstPhoto = task.photoUrls.firstOrNull()
    Box {
        if (firstPhoto != null) {
            Column(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(TDTheme.colors.lightPending),
            ) {
                SecretOrNormalPhotoBanner(
                    url =
                    run {
                        val base =
                            BuildConfig.BASE_URL
                                .trimEnd('/')
                        "$base/${firstPhoto.trimStart('/')}"
                    },
                    isSecret = false,
                )
                val openLocation = com.todoapp.mobile.ui.common.rememberOpenLocation(
                    task.locationName, task.locationAddress, task.locationLat, task.locationLng,
                )
                TDTaskCardWithCheckbox(
                    isChecked = task.isCompleted,
                    taskText = task.title,
                    taskDescription = task.description,
                    onCheckBoxClick = onChecked,
                    shape =
                    RoundedCornerShape(
                        topStart = 0.dp,
                        topEnd = 0.dp,
                        bottomStart = 12.dp,
                        bottomEnd = 12.dp,
                    ),
                    locationLabel = task.locationName,
                    onLocationClick = openLocation,
                )
            }
        } else {
            val openLocation = com.todoapp.mobile.ui.common.rememberOpenLocation(
                task.locationName, task.locationAddress, task.locationLat, task.locationLng,
            )
            TDTaskCardWithCheckbox(
                isChecked = task.isCompleted,
                taskText = task.title,
                taskDescription = task.description,
                onCheckBoxClick = onChecked,
                locationLabel = task.locationName,
                onLocationClick = openLocation,
            )
        }
        if (!task.priority.isNullOrBlank()) {
            TDPriorityBadge(
                priority = task.priority,
                modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 12.dp),
            )
        }
    }
}

@Composable
fun AssigneeAvatar(
    avatarUrl: String?,
    initials: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
        modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(TDTheme.colors.lightPending),
        contentAlignment = Alignment.Center,
    ) {
        TDText(
            text = initials.take(2),
            style = TDTheme.typography.subheading1,
            color = TDTheme.colors.onBackground,
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun GroupDetailOverviewTabWithTasksPreview() {
    TDTheme {
        GroupDetailOverviewTab(
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
                activities = mockGroupActivities,
                selectedTab = 0,
                taskFilter = TaskFilter.ALL,
                currentUserRole = "ADMIN",
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun GroupDetailOverviewTaskAssigneeBackFacePreview() {
    TDTheme {
        Box(
            modifier =
            Modifier
                .height(75.dp),
        ) {
            AssigneeBackFace(
                task = mockGroupTasks.first(),
            )
        }
    }
}
