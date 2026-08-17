package com.todoapp.mobile.ui.pomodorosummary

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.todoapp.mobile.common.deviceTimePattern
import com.todoapp.mobile.domain.engine.PomodoroEngine
import com.todoapp.mobile.domain.engine.PomodoroMode
import com.todoapp.mobile.domain.engine.Session
import com.todoapp.mobile.domain.model.Pomodoro
import com.todoapp.mobile.domain.repository.PomodoroRepository
import com.todoapp.mobile.navigation.NavigationEffect
import com.todoapp.mobile.navigation.Screen
import com.todoapp.mobile.ui.pomodorosummary.PomodoroSummaryContract.UiAction
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class PomodoroSummaryViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val pomodoroRepository: PomodoroRepository,
    private val pomodoroSessionRepository: com.todoapp.mobile.domain.repository.PomodoroSessionRepository,
    private val engine: PomodoroEngine,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {
    private val route: Screen.PomodoroSummary = savedStateHandle.toRoute()

    private val _uiState =
        MutableStateFlow(
            PomodoroSummaryContract.UiState(
                focusSessions = route.focusSessions,
                totalFocusMinutes = route.totalFocusMinutes,
                totalBreakMinutes = route.totalBreakMinutes,
                completedAt =
                LocalDateTime
                    .now()
                    .format(DateTimeFormatter.ofPattern("EEE, MMM d · " + deviceTimePattern(appContext))),
            ),
        )
    val uiState = _uiState.asStateFlow()

    private val _navEffect = Channel<NavigationEffect>(Channel.BUFFERED)
    val navEffect = _navEffect.receiveAsFlow()

    init {
        // Hybrid on purpose. The state above is seeded from the navigation arguments so the screen
        // renders instantly with no flicker, then the recorded rows overwrite it. Those arguments come
        // from counters that only accumulate while this screen's ViewModel is alive, so any run the user
        // backgrounded arrives here under-reported — which is the same bug the whole feature exists to
        // fix. Reading the rows makes the summary agree with the statistics screen.
        //
        // Seeding first also means a recorder failure degrades to "slightly wrong" instead of "empty".
        // Null only for an entry that was already on the back stack before the id existed; that one
        // keeps the seeded numbers rather than showing nothing.
        route.clientRunId?.let { runId ->
            viewModelScope.launch {
                pomodoroSessionRepository.observeRun(runId).collect { summary ->
                    _uiState.update {
                        it.copy(
                            focusSessions = summary.focusSessions,
                            totalFocusMinutes = summary.totalFocusMinutes,
                            totalBreakMinutes = summary.totalBreakMinutes,
                        )
                    }
                }
            }
        }
    }

    fun onAction(action: UiAction) {
        when (action) {
            UiAction.OnStartAgainTap -> onStartAgain()
            UiAction.OnEditSettingsTap -> onEditSettings()
            UiAction.OnCloseTap -> onClose()
        }
    }

    private fun onStartAgain() {
        viewModelScope.launch {
            val pomodoro = pomodoroRepository.getSavedPomodoroSettings() ?: return@launch
            val queue = buildSessionQueue(pomodoro)
            engine.setSessionQueue(queue)
            engine.prepare()
            engine.start()
            _navEffect.trySend(NavigationEffect.Navigate(Screen.Pomodoro, popUpTo = Screen.Home))
        }
    }

    private fun onEditSettings() {
        _navEffect.trySend(NavigationEffect.Navigate(Screen.PomodoroLaunch, Screen.Home))
    }

    private fun buildSessionQueue(pomodoro: Pomodoro): ArrayDeque<Session> {
        val queue = ArrayDeque<Session>()
        val focusSeconds = pomodoro.focusTime * SECONDS_PER_MINUTE
        val shortSeconds = pomodoro.shortBreak * SECONDS_PER_MINUTE
        val longSeconds = pomodoro.longBreak * SECONDS_PER_MINUTE
        val sectionCount = pomodoro.sectionCount.coerceAtLeast(1)
        for (i in 1..pomodoro.sessionCount) {
            queue.addLast(Session(durationSeconds = focusSeconds, mode = PomodoroMode.Focus))
            if (i != pomodoro.sessionCount) {
                val breakMode = if (i % sectionCount == 0) PomodoroMode.LongBreak else PomodoroMode.ShortBreak
                queue.addLast(
                    Session(
                        durationSeconds = if (breakMode == PomodoroMode.LongBreak) longSeconds else shortSeconds,
                        mode = breakMode,
                    ),
                )
            }
        }
        return queue
    }

    private fun onClose() {
        _navEffect.trySend(NavigationEffect.Navigate(Screen.Home, Screen.Home))
    }

    private companion object {
        private const val SECONDS_PER_MINUTE: Long = 60L
    }
}
