package com.todoapp.mobile.ui.journal.entry

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.todoapp.mobile.domain.model.JournalMood

object JournalEntryContract {
    sealed interface UiState {
        data object Loading : UiState

        @Immutable
        data class Editing(
            val entryId: Long,
            val content: String,
            val mood: JournalMood?,
            val photoPaths: List<String>,
            val createdAt: Long?,
            val isDirty: Boolean,
            val showInfoDialog: Boolean = false,
            val fullscreenPath: String? = null,
        ) : UiState {
            val isNew: Boolean get() = entryId == 0L

            fun isMeaningful(): Boolean = content.isNotBlank() || mood != null || photoPaths.isNotEmpty()
        }

        data class Error(
            @StringRes val messageRes: Int,
        ) : UiState
    }

    sealed interface UiAction {
        data class OnContentChange(val value: String) : UiAction

        data class OnMoodSelect(val mood: JournalMood?) : UiAction

        data class OnPhotoPicked(val uri: Uri) : UiAction

        data class OnPhotoRemove(val path: String) : UiAction

        data class OnPhotoTap(val path: String) : UiAction

        data object OnDismissFullscreen : UiAction

        data object OnBackPress : UiAction

        data object OnInfoClick : UiAction

        data object OnDismissInfoDialog : UiAction
    }

    sealed interface UiEffect {
        data class ShowToast(@StringRes val messageRes: Int) : UiEffect
    }
}
