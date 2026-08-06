package com.todoapp.mobile.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoapp.mobile.domain.update.AppUpdateChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Owns whether the "a newer version is out" dialog is on screen.
 *
 * **A dismissal is deliberately not persisted.** It lives here and nowhere else, which means it
 * survives a rotation and dies with the process — exactly the intended behaviour: not again this
 * launch, but again the next one. Writing it to DataStore would turn one "not now" into silence
 * forever, and an app the user never updates is the thing this dialog exists to prevent.
 */
@HiltViewModel
class AppUpdateViewModel
@Inject
constructor(
    private val appUpdateChecker: AppUpdateChecker,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppUpdateContract.UiState())
    val uiState = _uiState.asStateFlow()

    // Buffered, not rendezvous: this fires from a tap, and a hand-off to Play that silently
    // evaporates because the collector happened to be between lifecycle states is a button that
    // does nothing.
    private val _uiEffect by lazy { Channel<AppUpdateContract.UiEffect>(Channel.BUFFERED) }
    val uiEffect: Flow<AppUpdateContract.UiEffect> by lazy { _uiEffect.receiveAsFlow() }

    init {
        checkForUpdate()
    }

    fun onAction(action: AppUpdateContract.UiAction) {
        when (action) {
            AppUpdateContract.UiAction.OnUpdateClick -> onUpdateClick()
            AppUpdateContract.UiAction.OnDismiss -> hide()
        }
    }

    private fun onUpdateClick() {
        // Close before handing off: Play draws over the app, and a dialog still sitting underneath
        // would be waiting for the user if they backed out of the store.
        hide()
        _uiEffect.trySend(AppUpdateContract.UiEffect.LaunchUpdateFlow)
    }

    private fun hide() {
        _uiState.update { it.copy(isDialogVisible = false) }
    }

    private fun checkForUpdate() {
        viewModelScope.launch {
            val available = appUpdateChecker.isUpdateAvailable()
            _uiState.update { it.copy(isDialogVisible = available) }
        }
    }
}
