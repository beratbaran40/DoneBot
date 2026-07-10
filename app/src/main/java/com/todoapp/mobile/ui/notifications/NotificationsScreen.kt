package com.todoapp.mobile.ui.notifications

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.notifications.NotificationsContract.UiAction
import com.todoapp.mobile.ui.notifications.NotificationsContract.UiEffect
import com.todoapp.mobile.ui.notifications.NotificationsContract.UiState
import com.todoapp.uikit.components.TDEmptyState
import com.todoapp.uikit.components.TDErrorState
import com.todoapp.uikit.components.TDUndoSnackbar
import com.todoapp.uikit.extensions.collectWithLifecycle
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    uiState: UiState,
    uiEffect: Flow<UiEffect>,
    onAction: (UiAction) -> Unit,
) {
    val context = LocalContext.current
    uiEffect.collectWithLifecycle {
        when (it) {
            is UiEffect.ShowToast -> Toast.makeText(context, it.resId, Toast.LENGTH_SHORT).show()
        }
    }

    val visibleItems = (uiState as? UiState.Success)
        ?.let { state -> state.items.filterNot { it.id == state.undoDeleteNotificationId } }
        .orEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TDTheme.colors.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (uiState is UiState.Success && visibleItems.isNotEmpty()) {
                NotificationsHeader(
                    hasUnread = visibleItems.any { !it.isRead },
                    onMarkAllRead = { onAction(UiAction.OnMarkAllRead) },
                )
            }
            when (uiState) {
                is UiState.Loading -> NotificationsSkeletonList()
                is UiState.Error -> TDErrorState(
                    message = uiState.message,
                    actionText = stringResource(R.string.retry),
                    onActionClick = { onAction(UiAction.OnRetry) },
                )
                is UiState.Success -> PullToRefreshBox(
                    modifier = Modifier.fillMaxSize(),
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { onAction(UiAction.OnPullToRefresh) },
                ) {
                    if (visibleItems.isEmpty()) {
                        TDEmptyState(
                            title = stringResource(R.string.notifications_empty),
                            subtitle = stringResource(R.string.notifications_empty_subtitle),
                            iconRes = com.example.uikit.R.drawable.ic_notification,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                        )
                    } else {
                        NotificationsList(
                            items = visibleItems,
                            onItemTap = { onAction(UiAction.OnItemTap(it)) },
                            onDelete = { onAction(UiAction.OnDeleteNotification(it)) },
                            onAcceptInvitation = { onAction(UiAction.OnAcceptInvitation(it)) },
                        )
                    }
                }
            }
        }
        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            visible = uiState is UiState.Success && uiState.undoDeleteNotificationId != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            TDUndoSnackbar(
                message = stringResource(R.string.notifications_deleted),
                onUndo = { onAction(UiAction.OnUndoDelete) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@TDPreview
@Composable
private fun NotificationsScreenPreview(
    @PreviewParameter(NotificationsPreviewProvider::class) state: UiState,
) {
    TDTheme {
        NotificationsScreen(
            uiState = state,
            uiEffect = emptyFlow(),
            onAction = {},
        )
    }
}
