package com.todoapp.mobile.ui.update

import androidx.compose.runtime.Immutable

object AppUpdateContract {
    @Immutable
    data class UiState(
        val isDialogVisible: Boolean = false,
    )

    sealed interface UiAction {
        data object OnUpdateClick : UiAction

        data object OnDismiss : UiAction

        /** The app came back to the foreground: worth another look at Play, and at any stalled update. */
        data object OnAppResumed : UiAction
    }

    sealed interface UiEffect {
        /** Hand off to Play. The flow itself is Activity-scoped, so the screen runs it, not the VM. */
        data object LaunchUpdateFlow : UiEffect

        /** Re-enter an immediate update Play already started but never finished. */
        data object ResumeUpdateFlow : UiEffect
    }
}
