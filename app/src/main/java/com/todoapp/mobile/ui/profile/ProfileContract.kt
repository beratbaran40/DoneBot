package com.todoapp.mobile.ui.profile

import androidx.compose.runtime.Immutable
import com.todoapp.mobile.domain.repository.MAX_HALF_HEARTS

object ProfileContract {
    @Immutable
    data class UiState(
        val isLoading: Boolean = true,
        val userId: Long = 0L,
        val email: String = "",
        val displayName: String = "",
        val editedDisplayName: String = "",
        val avatarUrl: String? = null,
        val avatarVersion: Long = 0L,
        val isSaving: Boolean = false,
        val isUploading: Boolean = false,
        /** Health-points streak, 0..MAX_HALF_HEARTS — the avatar badge mirrors the Activity hearts. */
        val healthHalfHearts: Int = MAX_HALF_HEARTS,
        val errorMessage: String? = null,
    )

    sealed interface UiAction {
        data class OnDisplayNameChange(
            val value: String,
        ) : UiAction

        data object OnSaveName : UiAction

        /** A photo was picked; routes to the crop screen with the picked [uri]. */
        data class OnAvatarPicked(
            val uri: String,
        ) : UiAction

        /** The crop screen returned a cropped JPEG at [path]; upload it. */
        data class OnAvatarCropped(
            val path: String,
        ) : UiAction

        data object OnBack : UiAction

        data object OnChangePasswordTap : UiAction
    }

    sealed interface UiEffect {
        data class ShowToast(
            val message: String,
        ) : UiEffect
    }
}
