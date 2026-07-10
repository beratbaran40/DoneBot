package com.todoapp.mobile.ui.notifications

import androidx.compose.runtime.Immutable
import com.todoapp.mobile.domain.model.Notification

object NotificationsContract {
    sealed interface UiState {
        data object Loading : UiState

        @Immutable
        data class Success(
            val items: List<Notification>,
            val isRefreshing: Boolean = false,
        ) : UiState

        data class Error(val message: String) : UiState
    }

    sealed interface UiAction {
        data object OnRetry : UiAction
        data object OnPullToRefresh : UiAction
        data object OnMarkAllRead : UiAction
        data class OnItemTap(val notification: Notification) : UiAction
        data class OnDeleteNotification(val notification: Notification) : UiAction
        data class OnAcceptInvitation(val notification: Notification) : UiAction
    }

    sealed interface UiEffect {
        data class ShowToast(val resId: Int) : UiEffect
    }
}
