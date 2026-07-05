package com.todoapp.mobile.ui.blockedusers

import androidx.compose.runtime.Immutable

object BlockedUsersContract {
    @Immutable
    data class BlockedUserUiItem(
        val userId: Long,
        val displayName: String,
        val initials: String,
    )

    sealed interface UiState {
        data object Loading : UiState

        @Immutable
        data class Success(
            val users: List<BlockedUserUiItem>,
        ) : UiState
    }

    sealed interface UiAction {
        data class OnUnblock(
            val userId: Long,
        ) : UiAction
    }
}
