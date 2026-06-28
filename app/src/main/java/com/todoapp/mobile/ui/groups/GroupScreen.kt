package com.todoapp.mobile.ui.groups

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.uikit.R
import com.todoapp.mobile.common.move
import com.todoapp.mobile.ui.groups.GroupsContract.UiAction
import com.todoapp.mobile.ui.groups.GroupsContract.UiState
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDButtonType
import com.todoapp.uikit.components.TDFamilyGroupCard
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.components.TDUndoSnackbar
import com.todoapp.uikit.theme.TDTheme
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun GroupScreen(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
    onGroupSelect: ((Long, String) -> Unit)? = null,
) {
    // In the two-pane layout the host passes onGroupSelect to drive the detail pane; otherwise a
    // group tap navigates full-screen via OnGroupTap (phone / single-pane behaviour).
    val openGroup: (Long, String) -> Unit =
        onGroupSelect ?: { id, name -> onAction(UiAction.OnGroupTap(id, name)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            onAction(UiAction.OnScreenResumed)
        }
    }

    when (uiState) {
        is UiState.Empty ->
            GroupEmptyContent(
                onCreateNewGroup = { onAction(UiAction.OnCreateNewGroupTap) },
                uiState = uiState,
            )

        is UiState.Error -> {}
        UiState.Loading -> {}
        is UiState.Success -> GroupsContent(uiState, onAction, openGroup)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupsContent(
    uiState: UiState.Success,
    onAction: (UiAction) -> Unit,
    openGroup: (Long, String) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()

    var localGroups by remember { mutableStateOf(uiState.groups) }
    var dragOriginalIndex by remember { mutableIntStateOf(-1) }
    var dragFinalIndex by remember { mutableIntStateOf(-1) }

    val reorderableLazyListState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            if (dragOriginalIndex == -1) dragOriginalIndex = from.index
            dragFinalIndex = to.index
            val list = localGroups.toMutableList()
            list.move(from.index, to.index)
            localGroups = list
            hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }

    LaunchedEffect(uiState.groups) {
        if (!reorderableLazyListState.isAnyItemDragging) {
            localGroups = uiState.groups
        }
    }

    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .background(TDTheme.colors.background),
    ) groupsColumn@{
        TDText(
            text = stringResource(com.todoapp.mobile.R.string.my_groups),
            style = TDTheme.typography.heading1,
            color = TDTheme.colors.onBackground,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
        )

        val isAnyDragging = reorderableLazyListState.isAnyItemDragging

        Box(modifier = Modifier.weight(1f)) {
            PullToRefreshBox(
                modifier = Modifier.fillMaxSize(),
                isRefreshing = uiState.isRefreshing,
                onRefresh = { onAction(UiAction.OnPullToRefresh) },
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = lazyListState,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(
                        items = localGroups.filter { it.id != uiState.pendingDeleteGroup?.id },
                        key = { _, group -> group.id },
                    ) { index, group ->
                        ReorderableItem(
                            state = reorderableLazyListState,
                            key = group.id,
                        ) { isDragging ->
                            TDFamilyGroupCard(
                                name = group.name,
                                role = group.role,
                                description = group.description,
                                memberCount = group.memberCount,
                                pendingTaskCount = group.pendingTaskCount,
                                createdDate = group.createdAt,
                                membersIcon = R.drawable.ic_members,
                                tasksIcon = R.drawable.ic_sand_clock,
                                avatarUrl = group.avatarUrl,
                                onViewDetailsClick = {
                                    group.remoteId?.let { openGroup(it, group.name) }
                                },
                                onDeleteClick = { onAction(UiAction.OnDeleteGroupTap(group.id)) },
                                isDragging = isDragging,
                                isAnyDragging = isAnyDragging,
                                modifier =
                                Modifier
                                    .semantics {
                                        customActions =
                                            listOf(
                                                CustomAccessibilityAction(
                                                    label = "Move Up",
                                                    action = {
                                                        if (index > 0) {
                                                            onAction(UiAction.OnMoveGroup(index, index - 1))
                                                            true
                                                        } else {
                                                            false
                                                        }
                                                    },
                                                ),
                                                CustomAccessibilityAction(
                                                    label = "Move Down",
                                                    action = {
                                                        if (index < localGroups.lastIndex) {
                                                            onAction(UiAction.OnMoveGroup(index, index + 1))
                                                            true
                                                        } else {
                                                            false
                                                        }
                                                    },
                                                ),
                                            )
                                    }.longPressDraggableHandle(
                                        onDragStarted = {
                                            hapticFeedback.performHapticFeedback(
                                                HapticFeedbackType.GestureThresholdActivate,
                                            )
                                        },
                                        onDragStopped = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                            val orig = dragOriginalIndex
                                            val final = dragFinalIndex
                                            dragOriginalIndex = -1
                                            dragFinalIndex = -1
                                            if (orig != -1 && final != -1 && orig != final) {
                                                onAction(UiAction.OnMoveGroup(orig, final))
                                            }
                                        },
                                    ).clickable(
                                        onClick = {
                                            group.remoteId?.let { openGroup(it, group.name) }
                                        },
                                    ),
                            )
                        }
                    }
                }
            }

            TDButton(
                modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                text = stringResource(com.todoapp.mobile.R.string.create_new_group),
                type = TDButtonType.PRIMARY,
                icon = painterResource(R.drawable.ic_plus),
                onClick = { onAction(UiAction.OnCreateNewGroupTap) },
            )

            Box(
                modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            ) {
                this@groupsColumn.AnimatedVisibility(
                    visible = uiState.pendingDeleteGroup != null,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                ) {
                    TDUndoSnackbar(
                        message = stringResource(com.todoapp.mobile.R.string.group_deleted),
                        onUndo = { onAction(UiAction.OnUndoDeleteGroup) },
                    )
                }
            }
        }
    }

    if (uiState.isDeleteDialogOpen) {
        AlertDialog(
            onDismissRequest = { onAction(UiAction.OnDeleteGroupDialogDismiss) },
            title = { Text(stringResource(com.todoapp.mobile.R.string.delete_group_title)) },
            titleContentColor = TDTheme.colors.onBackground,
            containerColor = TDTheme.colors.background,
            textContentColor = TDTheme.colors.gray,
            text = { Text(stringResource(com.todoapp.mobile.R.string.delete_group_warning)) },
            confirmButton = {
                TextButton(onClick = { onAction(UiAction.OnDeleteGroupDialogConfirm) }) {
                    Text(
                        text = stringResource(com.todoapp.mobile.R.string.delete_group_button),
                        color = TDTheme.colors.crossRed,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(UiAction.OnDeleteGroupDialogDismiss) }) {
                    Text(
                        text = stringResource(com.todoapp.mobile.R.string.cancel),
                        color = TDTheme.colors.gray,
                    )
                }
            },
        )
    }
}

@Composable
private fun GroupEmptyContent(
    uiState: UiState.Empty,
    onCreateNewGroup: () -> Unit = {},
) {
    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .background(color = TDTheme.colors.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painterResource(R.drawable.ic_avatar_new_group),
            contentDescription = stringResource(com.todoapp.mobile.R.string.new_group),
            modifier = Modifier.size(192.dp),
            tint = TDTheme.colors.pendingGray.copy(0.81f),
        )
        Spacer(Modifier.height(32.dp))
        // Single TDText so the layout engine wraps the full sentence naturally per locale.
        // Splitting across two TDText components forced a hard line break that read awkwardly
        // when TR rephrased word order ("ailenizle görevler" landing on its own row).
        TDText(
            text = stringResource(com.todoapp.mobile.R.string.groups_empty_title),
            style = TDTheme.typography.heading2,
            color = TDTheme.colors.onBackground,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        TDText(
            text = stringResource(com.todoapp.mobile.R.string.groups_empty_subtitle),
            style = TDTheme.typography.subheading3,
            color = TDTheme.colors.lightGray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        TDButton(
            modifier = Modifier.clip(RoundedCornerShape(12.dp)),
            text =
            if (uiState.isUserAuthenticated) {
                stringResource(com.todoapp.mobile.R.string.create_new_group)
            } else {
                stringResource(
                    com.todoapp.mobile.R.string.login_to_create_a_group,
                )
            },
            fullWidth = true,
        ) { onCreateNewGroup() }
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun GroupsContentPreview(
    @PreviewParameter(GroupsPreviewProvider::class) uiState: UiState,
) {
    TDTheme {
        GroupScreen(
            uiState = uiState,
            onAction = {},
        )
    }
}
