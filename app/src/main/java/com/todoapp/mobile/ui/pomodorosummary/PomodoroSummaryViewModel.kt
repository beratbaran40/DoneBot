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
