package com.todoapp.mobile.ui.groups.groupdetail

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabIndicatorScope
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.lerp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.groups.groupdetail.GroupDetailContract.UiAction
import com.todoapp.mobile.ui.home.TaskFormUiAction
import com.todoapp.uikit.components.TDScreenWithSheet
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.extensions.ObscuredTouchGuard
import com.todoapp.uikit.extensions.collectWithLifecycle
import com.todoapp.uikit.theme.TDTheme
import kotlin.math.ceil
import kotlin.math.floor

@Composable
fun GroupDetailScreen(viewModel: GroupDetailViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.onAction(UiAction.OnScreenResumed)
        }
    }

    viewModel.uiEffect.collectWithLifecycle { effect ->
        when (effect) {
            is GroupDetailContract.UiEffect.ShowToast ->
                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
        }
    }

    GroupDetailContent(
        uiState = uiState,
        onAction = viewModel::onAction,
    )

    val successState = uiState as? GroupDetailContract.UiState.Success
    if (successState?.pendingDeleteTaskId != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onAction(UiAction.OnDeleteTaskDismiss) },
            title = { Text(stringResource(R.string.delete_task_title)) },
            text = {
                ObscuredTouchGuard()
                Text(stringResource(R.string.delete_task_message))
            },
            titleContentColor = TDTheme.colors.onBackground,
            containerColor = TDTheme.colors.background,
            textContentColor = TDTheme.colors.gray,
            confirmButton = {
                TextButton(onClick = { viewModel.onAction(UiAction.OnDeleteTaskConfirm) }) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = TDTheme.colors.crossRed,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onAction(UiAction.OnDeleteTaskDismiss) }) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = TDTheme.colors.gray,
                    )
                }
            },
        )
    }

    if (successState?.isFirstInviteDialogOpen == true) {
        GroupDetailFirstInviteDialog(
            groupName = successState.groupName,
            email = successState.firstInviteEmail,
            errorRes = successState.firstInviteErrorRes,
            isSending = successState.isFirstInviteSending,
            onEmailChange = { viewModel.onAction(UiAction.OnFirstInviteEmailChange(it)) },
            onSend = { viewModel.onAction(UiAction.OnFirstInviteSend) },
            onDismiss = { viewModel.onAction(UiAction.OnFirstInviteDismiss) },
        )
    }

    if (successState?.pendingAssignTaskId != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onAction(UiAction.OnAssignToMeDismiss) },
            title = { Text(stringResource(R.string.assign_to_me_dialog_title)) },
            text = { Text(stringResource(R.string.assign_to_me_dialog_message)) },
            titleContentColor = TDTheme.colors.onBackground,
            containerColor = TDTheme.colors.background,
            textContentColor = TDTheme.colors.gray,
            confirmButton = {
                TextButton(onClick = { viewModel.onAction(UiAction.OnAssignToMeConfirm) }) {
                    Text(
                        text = stringResource(R.string.assign),
                        color = TDTheme.colors.pendingGray,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onAction(UiAction.OnAssignToMeDismiss) }) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = TDTheme.colors.gray,
                    )
                }
            },
        )
    }
}

@Composable
private fun GroupDetailContent(
    uiState: GroupDetailContract.UiState,
    onAction: (UiAction) -> Unit,
) {
    val successState = uiState as? GroupDetailContract.UiState.Success
    TDScreenWithSheet(
        isSheetOpen = successState?.isTaskSheetOpen ?: false,
        sheetContent = {
            if (successState != null) {
                GroupAddTaskSheet(
                    groupName = successState.groupName,
                    formState = successState.taskFormState,
                    members = successState.members,
                    submitLabel =
                    stringResource(
                        if (successState.editingTaskId != null) R.string.update_task else R.string.create_task,
                    ),
                    onAction = { action ->
                        when (action) {
                            TaskFormUiAction.Dismiss -> onAction(UiAction.OnDismissGroupTaskSheet)
                            TaskFormUiAction.Create -> onAction(UiAction.OnGroupTaskCreate)
                            else -> onAction(UiAction.OnGroupTaskFormAction(action))
                        }
                    },
                )
            }
        },
        onDismissSheet = { onAction(UiAction.OnDismissGroupTaskSheet) },
    ) {
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .background(TDTheme.colors.background),
        ) {
            when (uiState) {
                is GroupDetailContract.UiState.Loading -> GroupDetailLoadingContent()
                is GroupDetailContract.UiState.Error -> GroupDetailErrorContent(uiState.message)
                is GroupDetailContract.UiState.Success -> GroupDetailSuccessContent(uiState, onAction)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupDetailSuccessContent(
    uiState: GroupDetailContract.UiState.Success,
    onAction: (UiAction) -> Unit,
) {
    val tabs =
        listOf(
            stringResource(R.string.overview),
            stringResource(R.string.members),
            stringResource(R.string.activity),
        )

    Column(modifier = Modifier.fillMaxSize()) {
        // Tabs are also horizontally swipeable (tester feedback: the tab row reads as a pager).
        // Two-way sync with the VM-owned selectedTab: tab tap → state → animateScrollToPage;
        // swipe settles → OnTabSelected (a same-value copy dedupes in the StateFlow, so no loop).
        // The indicator + tab tint read the pager DIRECTLY (not selectedTab) so both ride the
        // finger mid-swipe instead of springing after settle.
        val pagerState = rememberPagerState(initialPage = uiState.selectedTab) { GroupDetailContract.TAB_COUNT }
        LaunchedEffect(uiState.selectedTab) {
            if (pagerState.currentPage != uiState.selectedTab) {
                pagerState.animateScrollToPage(uiState.selectedTab)
            }
        }
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.settledPage }.collect { page ->
                onAction(UiAction.OnTabSelected(page))
            }
        }
        // Tint flips at the pager's direction-commit threshold — a few recompositions per swipe,
        // not per-frame (the per-frame tracking lives in the indicator's measure lambda).
        val visualTab = pagerState.targetPage

        SecondaryTabRow(
            selectedTabIndex = uiState.selectedTab,
            modifier = Modifier.fillMaxWidth(),
            containerColor = TDTheme.colors.background,
            contentColor = TDTheme.colors.pendingGray,
            indicator = { PagerTrackingIndicator(pagerState) },
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = uiState.selectedTab == index,
                    onClick = { onAction(UiAction.OnTabSelected(index)) },
                    text = {
                        TDText(
                            text = title,
                            style = TDTheme.typography.subheading1,
                            color = if (visualTab == index) TDTheme.colors.darkPending else TDTheme.colors.gray,
                        )
                    },
                )
            }
        }

        PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
            isRefreshing = uiState.isRefreshing,
            onRefresh = { onAction(UiAction.OnPullToRefresh) },
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when (page) {
                    GroupDetailContract.TAB_OVERVIEW -> GroupDetailOverviewTab(uiState = uiState, onAction = onAction)
                    GroupDetailContract.TAB_MEMBERS -> GroupDetailMembersTab(uiState = uiState, onAction = onAction)
                    GroupDetailContract.TAB_ACTIVITY -> GroupDetailActivityTab(uiState = uiState)
                }
            }
        }
    }
}

// Positions the indicator from the pager's LIVE scroll position (currentPage + offset fraction)
// instead of the settled tab index, so the line rides the finger during a swipe and glides through
// intermediate tabs on a tap-driven animateScrollToPage. Reading pagerState inside the measure
// lambda is snapshot-aware → Compose relayouts it every scroll frame without recomposing the row.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabIndicatorScope.PagerTrackingIndicator(pagerState: PagerState) {
    TabRowDefaults.SecondaryIndicator(
        color = TDTheme.colors.darkPending,
        modifier = Modifier.tabIndicatorLayout { measurable, constraints, tabPositions ->
            val position = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                .coerceIn(0f, (tabPositions.size - 1).toFloat())
            val fromTab = tabPositions[floor(position).toInt()]
            val toTab = tabPositions[ceil(position).toInt()]
            val fraction = position - floor(position)
            val width = lerp(fromTab.width, toTab.width, fraction).roundToPx()
            val left = lerp(fromTab.left, toTab.left, fraction).roundToPx()
            val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
            layout(constraints.maxWidth, constraints.maxHeight) {
                placeable.place(left, maxOf(constraints.maxHeight - placeable.height, 0))
            }
        },
    )
}

@Composable
private fun GroupDetailLoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = TDTheme.colors.pendingGray)
    }
}

@Composable
private fun GroupDetailErrorContent(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        TDText(
            text = message,
            color = TDTheme.colors.crossRed,
            style = TDTheme.typography.subheading2,
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun GroupDetailContentPreview(
    @PreviewParameter(GroupDetailPreviewProvider::class) uiState: GroupDetailContract.UiState,
) {
    TDTheme {
        GroupDetailContent(
            uiState = uiState,
            onAction = {},
        )
    }
}
