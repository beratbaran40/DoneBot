package com.todoapp.mobile.ui.onboarding

import androidx.compose.runtime.Immutable

object OnboardingContract {
    @Immutable
    data class UiState(
        val bgIndex: Int = 0,
    )

    sealed interface UiAction {
        data object OnLoginClick : UiAction

        data object OnGetStartedClick : UiAction

        /**
         * Advances the background carousel by one step. Emitted by the screen's lifecycle-aware
         * ticker instead of a ViewModel `while (true)` loop, so the carousel — and the bitmap
         * decodes it drives — stops while the app is backgrounded.
         */
        data object OnBackgroundTick : UiAction
    }

    sealed interface UiEffect
}
