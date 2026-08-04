package com.todoapp.mobile.ui.onboarding

import androidx.lifecycle.ViewModel
import com.todoapp.mobile.navigation.NavigationEffect
import com.todoapp.mobile.navigation.Screen
import com.todoapp.mobile.ui.onboarding.OnboardingContract.UiAction
import com.todoapp.mobile.ui.onboarding.OnboardingContract.UiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

class OnboardingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _navEffect by lazy { Channel<NavigationEffect>() }
    val navEffect by lazy { _navEffect.receiveAsFlow() }

    fun onAction(uiAction: UiAction) {
        when (uiAction) {
            // The carousel used to run from a `while (true)` loop in this ViewModel's init{}, which kept
            // advancing bgIndex — and re-decoding a full-screen background per step — while the app was
            // backgrounded. The screen now drives it under repeatOnLifecycle(RESUMED) instead.
            // Modulo reads OnboardingBackgrounds.size (same package, OnboardingScreen.kt) rather than a
            // literal 4, so adding or removing a background can't silently desync the two.
            is UiAction.OnBackgroundTick ->
                _uiState.update { state ->
                    state.copy(bgIndex = (state.bgIndex + 1) % OnboardingBackgrounds.size)
                }

            is UiAction.OnLoginClick -> _navEffect.trySend(NavigationEffect.Navigate(Screen.Login()))
            is UiAction.OnGetStartedClick ->
                _navEffect.trySend(
                    NavigationEffect.Navigate(Screen.Home, popUpTo = Screen.Onboarding, isInclusive = true),
                )
        }
    }
}
