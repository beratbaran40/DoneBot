package com.todoapp.mobile.ui.groups.groupsettings

import androidx.compose.runtime.Immutable

object GroupSettingsContract {
    @Immutable
    data class UiState(
        val groupId: Long = 0L,
        val name: String = "",
        val description: String = "",
        val currentUserRole: String = "",
        val avatarUrl: String? = null,
        val avatarVersion: Long = 0L,
        val isSaving: Boolean = false,
        val isLeaving: Boolean = false,
        val isLeaveDialogOpen: Boolean = false,
        val isTransferPromptOpen: Boolean = false,
        val isLoading: Boolean = true,
        val errorMessage: String? = null,
    )

    sealed interface UiAction {
        data class OnNameChange(
            val name: String,
        ) : UiAction

        data class OnDescriptionChange(
            val description: String,
        ) : UiAction

        data object OnSaveTap : UiAction

        data object OnManageMembersTap : UiAction

        data object OnTransferOwnershipTap : UiAction

        data object OnLeaveTap : UiAction

        data object OnLeaveConfirm : UiAction

        data object OnLeaveDialogDismiss : UiAction

        data object OnTransferPromptConfirm : UiAction

        data object OnTransferPromptDismiss : UiAction

        /** A photo was picked; routes to the crop screen with the picked [uri]. */
        data class OnAvatarPicked(
            val uri: String,
        ) : UiAction

        /** The crop screen returned a cropped JPEG at [path]; upload it. */
        data class OnAvatarCropped(
            val path: String,
        ) : UiAction
    }

    sealed interface UiEffect {
        data class ShowToast(
            val message: String,
        ) : UiEffect
    }
}
