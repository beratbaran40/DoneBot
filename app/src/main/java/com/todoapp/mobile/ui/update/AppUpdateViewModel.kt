package com.todoapp.mobile.ui.update

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoapp.mobile.domain.update.AppUpdateChecker
import com.todoapp.mobile.domain.update.AppUpdateFlowStarter
import com.todoapp.mobile.domain.update.AppUpdateStatus
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
 * **A dismissal is deliberately not persisted.** It lives in [answeredThisLaunch] and nowhere else,
 * which means it survives a rotation and a foreground return but dies with the process — exactly the
 * intended behaviour: not again this launch, but again the next one. Writing it to DataStore would
 * turn one "not now" into silence forever, and an app the user never updates is the thing this dialog
 * exists to prevent.
 */
@HiltViewModel
class AppUpdateViewModel
@Inject
constructor(
    private val appUpdateChecker: AppUpdateChecker,
    private val appUpdateFlowStarter: AppUpdateFlowStarter,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppUpdateContract.UiState())
    val uiState = _uiState.asStateFlow()

    // Buffered, not rendezvous: this fires from a tap, and a hand-off to Play that silently
    // evaporates because the collector happened to be between lifecycle states is a button that
    // does nothing.
    private val _uiEffect by lazy { Channel<AppUpdateContract.UiEffect>(Channel.BUFFERED) }
    val uiEffect: Flow<AppUpdateContract.UiEffect> by lazy { _uiEffect.receiveAsFlow() }

    /** The user said "not now" or "update" — either way, stop putting the dialog in front of them. */
    private var answeredThisLaunch = false

    /** Set once the user hands off to Play, so a resume still probes for a stalled update. */
    private var updateStartedThisLaunch = false

    private var lastStatus: AppUpdateStatus = AppUpdateStatus.Unknown
    private var checkInFlight = false

    init {
        checkForUpdate()
    }

    fun onAction(action: AppUpdateContract.UiAction) {
        when (action) {
            AppUpdateContract.UiAction.OnUpdateClick -> onUpdateClick()
            AppUpdateContract.UiAction.OnDismiss -> onDismiss()
            AppUpdateContract.UiAction.OnAppResumed -> onAppResumed()
        }
    }

    /**
     * Runs Play's flow. The launcher is a parameter and is never stored: it belongs to the composition
     * that created it, and the coroutine calling this is cancelled along with that composition.
     */
    suspend fun startUpdateFlow(launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean = appUpdateFlowStarter.startImmediateUpdate(launcher)

    suspend fun resumeUpdateFlow(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        appUpdateFlowStarter.resumeInProgressUpdate(launcher)
    }

    private fun onUpdateClick() {
        // Close before handing off: Play draws over the app, and a dialog still sitting underneath
        // would be waiting for the user if they backed out of the store.
        hide()
        answeredThisLaunch = true
        updateStartedThisLaunch = true
        _uiEffect.trySend(AppUpdateContract.UiEffect.LaunchUpdateFlow)
    }

    private fun onDismiss() {
        hide()
        answeredThisLaunch = true
    }

    private fun onAppResumed() {
        // A definitive "nothing to install" does not become truer by asking again, and there is no
        // stalled update to find unless this launch actually started one.
        if (lastStatus == AppUpdateStatus.NotAvailable && !updateStartedThisLaunch) return
        checkForUpdate()
    }

    private fun hide() {
        _uiState.update { it.copy(isDialogVisible = false) }
    }

    private fun checkForUpdate() {
        // ON_RESUME fires for the first time moments after init{} launched its own check; without this
        // every cold start would ask Play twice.
        if (checkInFlight) return
        checkInFlight = true
        viewModelScope.launch {
            try {
                val status = appUpdateChecker.check()
                lastStatus = status
                when (status) {
                    AppUpdateStatus.Available ->
                        if (!answeredThisLaunch) _uiState.update { it.copy(isDialogVisible = true) }

                    AppUpdateStatus.InProgress ->
                        _uiEffect.trySend(AppUpdateContract.UiEffect.ResumeUpdateFlow)

                    // Nothing to do. NotAvailable is final; Unknown gets another chance on the next
                    // foreground, which is how a launch with no network still ends up telling the user.
                    AppUpdateStatus.NotAvailable, AppUpdateStatus.Unknown -> Unit
                }
            } finally {
                checkInFlight = false
            }
        }
    }
}
