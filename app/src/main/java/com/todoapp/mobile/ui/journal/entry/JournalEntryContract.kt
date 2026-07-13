package com.todoapp.mobile.ui.journal.entry

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

object JournalEntryContract {
    sealed interface UiState {
        data object Loading : UiState

        @Immutable
        data class Editing(
            val entryId: Long,
            val content: String,
            val photoPaths: List<String>,
            val createdAt: Long?,
            val isDirty: Boolean,
            val fullscreenPath: String? = null,
        ) : UiState {
            val isNew: Boolean get() = entryId == 0L

            fun isMeaningful(): Boolean = content.isNotBlank() || photoPaths.isNotEmpty()
        }

        data class Error(
            @StringRes val messageRes: Int,
        ) : UiState
    }

    sealed interface UiAction {
        data class OnContentChange(val value: String) : UiAction

        data class OnPhotoPicked(val uri: Uri) : UiAction

        data object OnPolaroidCameraClicked : UiAction

        data class OnPhotoCapturedFromCamera(val path: String) : UiAction

        data class OnPhotoRemove(val path: String) : UiAction

        data class OnPhotoTap(val path: String) : UiAction

        data object OnDismissFullscreen : UiAction

        data object OnBackPress : UiAction
    }

    sealed interface UiEffect {
        data class ShowToast(@StringRes val messageRes: Int) : UiEffect
    }
}
