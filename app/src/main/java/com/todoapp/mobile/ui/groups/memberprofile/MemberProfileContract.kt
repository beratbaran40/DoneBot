package com.todoapp.mobile.ui.groups.memberprofile

import androidx.compose.runtime.Immutable

object MemberProfileContract {
    @Immutable
    data class MemberUiItem(
        val userId: Long,
        val displayName: String,
        val firstName: String,
        val lastName: String,
        val email: String,
        val avatarUrl: String?,
        val initials: String,
        val role: String,
        val joinedAt: String,
    )

    sealed interface UiState {
        data object Loading : UiState

        @Immutable
        data class Success(
            val member: MemberUiItem,
            val pendingRemoval: Boolean = false,
            val showConfirmDialog: Boolean = false,
            val isBlocked: Boolean = false,
            val showReportDialog: Boolean = false,
            val isCurrentUserAdmin: Boolean = false,
        ) : UiState

        data class Error(
            val message: String,
        ) : UiState
    }

    sealed interface UiAction {
        data object OnRemoveTap : UiAction

        data object OnConfirmRemove : UiAction

        data object OnDismissDialog : UiAction

        data object OnUndoRemove : UiAction

        data object OnReportTap : UiAction

        data object OnReportConfirm : UiAction

        data object OnReportDismiss : UiAction

        data object OnBlockToggle : UiAction
    }

    sealed interface UiEffect {
        data class ShowToast(
            val message: String,
        ) : UiEffect
    }
}
