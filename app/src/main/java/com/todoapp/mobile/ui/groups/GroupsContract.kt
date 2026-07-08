package com.todoapp.mobile.ui.groups

import androidx.compose.runtime.Immutable

object GroupsContract {
    @Immutable
    data class GroupUiItem(
        val id: Long,
        val remoteId: Long?,
        val name: String,
        val role: String,
        val description: String,
        val memberCount: Int,
        val pendingTaskCount: Int,
        val createdAt: String,
        val avatarUrl: String? = null,
    )

    sealed interface UiState {
        data object Loading : UiState

        @Immutable
        data class Empty(
            val isUserAuthenticated: Boolean,
        ) : UiState

        @Immutable
        data class Success(
            val isUserAuthenticated: Boolean,
            val groups: List<GroupUiItem>,
            val isDeleteDialogOpen: Boolean = false,
            val pendingDeleteGroup: GroupUiItem? = null,
            val isRefreshing: Boolean = false,
        ) : UiState

        data class Error(
            val message: String,
        ) : UiState
    }

    sealed interface UiEffect {
        data class ShowToast(val message: String) : UiEffect
    }

    sealed interface UiAction {
        data object OnCreateNewGroupTap : UiAction

        data class OnGroupTap(
            val remoteId: Long,
            val groupName: String,
            // GroupDetailContract.TAB_* — lets the members stat box land directly on Members.
            val initialTab: Int = 0,
        ) : UiAction

        data class OnMoveGroup(
            val from: Int,
            val to: Int,
        ) : UiAction

        data class OnDeleteGroupTap(
            val id: Long,
        ) : UiAction

        data object OnDeleteGroupDialogConfirm : UiAction

        data object OnDeleteGroupDialogDismiss : UiAction

        data object OnUndoDeleteGroup : UiAction

        data object OnScreenResumed : UiAction

        data object OnPullToRefresh : UiAction
    }
}
