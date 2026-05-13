package com.todoapp.mobile.ui.pomodoro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoapp.mobile.common.toUiMode
import com.todoapp.mobile.domain.engine.PomodoroEngine
import com.todoapp.mobile.domain.engine.PomodoroEvent
import com.todoapp.mobile.domain.engine.PomodoroMode
import com.todoapp.mobile.domain.engine.Session
import com.todoapp.mobile.domain.model.Pomodoro
import com.todoapp.mobile.domain.repository.PomodoroRepository
import com.todoapp.mobile.navigation.NavigationEffect
import com.todoapp.mobile.navigation.Screen
import com.todoapp.mobile.ui.pomodoro.PomodoroContract.UiAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PomodoroViewModel
@Inject
constructor(
    private val pomodoroRepository: PomodoroRepository,
    private val engine: PomodoroEngine,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PomodoroContract.UiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect by lazy { Channel<PomodoroContract.UiEffect>() }
    val uiEffect by lazy { _uiEffect.receiveAsFlow() }

    val engineState = engine.state
    private val engineEvents = engine.events

    private val _navEffect by lazy { Channel<NavigationEffect>() }
    val navEffect by lazy { _navEffect.receiveAsFlow() }

    // Full immutable snapshot of all sessions — used for index-based lookups and queue rebuilding
    private var sessionQueue: List<Session> = emptyList()

    // Tracks which session in [sessionQueue] the engine is currently on
    private var currentSessionIndex: Int = 0

    // Accumulates completed session stats for the summary screen
    private var totalFocusSeconds: Long = 0L
    private var totalBreakSeconds: Long = 0L

    fun onAction(action: UiAction) {
        when (action) {
            UiAction.SkipSession -> onSkipSession()
            UiAction.StartCountDown -> onStartCountdown()
            UiAction.StopCountDown -> onStopCountdown()
            is UiAction.ToggleBannerVisibility -> updateBannerVisibility(action.isVisible)
            UiAction.OnEndSessionTap -> _uiState.update { it.copy(showFinishEarlyDialog = true) }
            UiAction.DismissEndSessionDialog -> _uiState.update { it.copy(showFinishEarlyDialog = false) }
            UiAction.ConfirmEndSession -> onConfirmEndSession()
        }
    }

    init {
        // If the engine is already running with a valid queue (e.g. DoneBot
        // started a session via LocalIntentClassifier), trust its state and
        // mirror the snapshot into this VM. Resetting here would wipe the
        // bot's queue and rebuild from saved settings — surfacing the old
        // saved duration instead of what the bot actually started.
        // The hasStartedAnySession guard inside the engine still protects
        // the "leftover Summary leak" case for non-running stale state.
        val engineSnapshot = engine.state.value
        val originalSessions = engine.sessionsSnapshot
        if (engineSnapshot.isRunning && originalSessions.isNotEmpty()) {
            sessionQueue = originalSessions
            currentSessionIndex = engineSnapshot.currentSessionIndex
            _uiState.update {
                it.copy(
                    totalSessions = sessionQueue.size,
                    currentSessionIndex = currentSessionIndex,
                    totalSessionSeconds =
                    sessionQueue.getOrNull(currentSessionIndex)?.durationSeconds
                        ?: engineSnapshot.currentSessionTotalSeconds,
                )
            }
        } else {
            engine.resetState()
            initializeEngineIfNeeded()
        }
        observeEngineEvents()
        observeEngineState()
    }

    // ── Actions ────────────────────────────────────────────────────────────────

    private fun onSkipSession() {
        if (!uiState.value.isOvertime) {
            // SessionFinished won't fire naturally for a manual skip, so advance index here.
            currentSessionIndex++
        }
        engine.skip(autoStart = uiState.value.isRunning)
    }

    private fun onStartCountdown() {
        if (uiState.value.isOvertime) {
            // SessionFinished already fired when overtime started; index is already correct.
            engine.skip(autoStart = true)
        } else {
            engine.start()
        }
    }

    private fun onStopCountdown() {
        engine.pause()
    }

    private fun onConfirmEndSession() {
        _uiState.update { it.copy(showFinishEarlyDialog = false) }
        engine.pause()
        engine.setSessionQueue(ArrayDeque())
        _navEffect.trySend(NavigationEffect.Navigate(route = Screen.PomodoroLaunch, popUpTo = Screen.Home))
    }

    private fun updateBannerVisibility(isVisible: Boolean) {
        engine.updateBannerVisibility(isVisible)
    }

    // ── Engine lifecycle ───────────────────────────────────────────────────────

    private fun initializeEngineIfNeeded() {
        viewModelScope.launch {
            val pomodoro = requireNotNull(pomodoroRepository.getSavedPomodoroSettings()) {
                "Pomodoro settings should be seeded at first launch"
            }
            if (!engine.state.value.isRunning) {
                val queue = buildSessionQueue(pomodoro)
                sessionQueue = queue.toList()
                currentSessionIndex = 0
                engine.setSessionQueue(queue)
                engine.prepare()
                engine.start()
                _uiState.update {
                    it.copy(
                        totalSessions = sessionQueue.size,
                        totalSessionSeconds = sessionQueue.firstOrNull()?.durationSeconds ?: 0L,
                        currentSessionIndex = 0,
                    )
                }
            } else {
                // Engine is already running — user navigated back to this screen.
                // Rebuild the full queue from settings (for duration lookups by index)
                // and restore the current index from the engine's tracked state.
                sessionQueue = buildSessionQueue(pomodoro).toList()
                val engineState = engine.state.value
                currentSessionIndex = engineState.currentSessionIndex
                _uiState.update {
                    it.copy(
                        totalSessions = engineState.totalSessions,
                        currentSessionIndex = engineState.currentSessionIndex,
                        totalSessionSeconds =
                        sessionQueue
                            .getOrNull(currentSessionIndex)
                            ?.durationSeconds ?: it.totalSessionSeconds,
                    )
                }
            }
        }
    }

    private fun navigateToFinishScreen() {
        _navEffect.trySend(
            NavigationEffect.Navigate(
                route =
                Screen.PomodoroSummary(
                    focusSessions = sessionQueue.count { it.mode == PomodoroMode.Focus },
                    totalFocusMinutes = (totalFocusSeconds / SECONDS_PER_MINUTE).toInt(),
                    totalBreakMinutes = (totalBreakSeconds / SECONDS_PER_MINUTE).toInt(),
                ),
                popUpTo = Screen.Home,
            ),
        )
    }

    private fun observeEngineEvents() {
        viewModelScope.launch {
            engineEvents.collect { event ->
                when (event) {
                    PomodoroEvent.SessionFinished -> onSessionFinished()
                    PomodoroEvent.PomodoroFinished -> navigateToFinishScreen()
                }
            }
        }
    }

    /**
     * Called only when the countdown reaches 0 naturally (engine enters overtime).
     * Manual skips are handled in [onSkipSession] and do NOT reach here.
     */
    @Suppress("RedundantSuspendModifier") // Detekt false positive: invokes suspend Channel.send
    private suspend fun onSessionFinished() {
        sessionQueue.getOrNull(currentSessionIndex)?.let { finished ->
            when (finished.mode) {
                PomodoroMode.Focus -> totalFocusSeconds += finished.durationSeconds
                PomodoroMode.ShortBreak,
                PomodoroMode.LongBreak,
                -> totalBreakSeconds += finished.durationSeconds
                PomodoroMode.OverTime -> Unit
            }
        }
        currentSessionIndex++
        _uiEffect.send(PomodoroContract.UiEffect.SessionFinished)
    }

    private fun observeEngineState() {
        viewModelScope.launch {
            engineState.collect { engineSnapshot ->
                mapEngineStateToUiState(engineSnapshot)
            }
        }
    }

    private fun mapEngineStateToUiState(engineStateSnapshot: com.todoapp.mobile.domain.engine.PomodoroEngineState) {
        val totalSeconds = engineStateSnapshot.remainingSeconds
        val currentDuration =
            sessionQueue.getOrNull(currentSessionIndex)?.durationSeconds
                ?: (25L * SECONDS_PER_MINUTE)

        _uiState.update {
            it.copy(
                min = (totalSeconds / SECONDS_PER_MINUTE).toInt(),
                second = (totalSeconds % SECONDS_PER_MINUTE).toInt(),
                mode = engineStateSnapshot.mode.toUiMode(),
                isRunning = engineStateSnapshot.isRunning,
                isOvertime = engineStateSnapshot.isOvertime,
                totalSessionSeconds = currentDuration,
                currentSessionIndex = currentSessionIndex,
                totalSessions = sessionQueue.size,
            )
        }
    }

    // ── Session queue builder ──────────────────────────────────────────────────

    private fun buildSessionQueue(pomodoro: Pomodoro): ArrayDeque<Session> {
        val queue: ArrayDeque<Session> = ArrayDeque()

        val focusSeconds = pomodoro.focusTime * SECONDS_PER_MINUTE
        val shortSeconds = pomodoro.shortBreak * SECONDS_PER_MINUTE
        val longSeconds = pomodoro.longBreak * SECONDS_PER_MINUTE

        for (i in FIRST_SESSION_INDEX..pomodoro.sessionCount) {
            queue.addLast(Session(durationSeconds = focusSeconds, mode = PomodoroMode.Focus))

            val isLastFocus = i == pomodoro.sessionCount
            if (!isLastFocus) {
                val breakMode =
                    if (i % pomodoro.sectionCount == 0) {
                        PomodoroMode.LongBreak
                    } else {
                        PomodoroMode.ShortBreak
                    }
                val breakDuration = if (breakMode == PomodoroMode.LongBreak) longSeconds else shortSeconds
                queue.addLast(Session(durationSeconds = breakDuration, mode = breakMode))
            }
        }

        return queue
    }

    private companion object {
        private const val SECONDS_PER_MINUTE: Long = 60L
        private const val FIRST_SESSION_INDEX: Int = 1
    }
}
