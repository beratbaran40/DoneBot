package com.todoapp.mobile.ui.groups.groupdetail

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
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
import com.todoapp.uikit.extensions.collectWithLifecycle
import com.todoapp.uikit.theme.TDTheme

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
            text = { Text(stringResource(R.string.delete_task_message)) },
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
        TabRow(
            selectedTabIndex = uiState.selectedTab,
            modifier = Modifier.fillMaxWidth(),
            containerColor = TDTheme.colors.background,
            contentColor = TDTheme.colors.pendingGray,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab]),
                    color = TDTheme.colors.darkPending,
                )
            },
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = uiState.selectedTab == index,
                    onClick = { onAction(UiAction.OnTabSelected(index)) },
                    text = {
                        TDText(
                            text = title,
                            style = TDTheme.typography.subheading1,
                            color = if (uiState.selectedTab == index) TDTheme.colors.darkPending else TDTheme.colors.gray,
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
            when (uiState.selectedTab) {
                0 -> GroupDetailOverviewTab(uiState = uiState, onAction = onAction)
                1 -> GroupDetailMembersTab(uiState = uiState, onAction = onAction)
                2 -> GroupDetailActivityTab(uiState = uiState)
            }
        }
    }
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
