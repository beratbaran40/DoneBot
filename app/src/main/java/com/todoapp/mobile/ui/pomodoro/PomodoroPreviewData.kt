package com.todoapp.mobile.ui.pomodoro

import com.todoapp.mobile.common.toUiMode
import com.todoapp.mobile.domain.ambience.PomodoroAmbience
import com.todoapp.mobile.domain.engine.PomodoroMode
import com.todoapp.mobile.ui.pomodoro.PomodoroContract.UiState

/** Sample states for the Pomodoro previews, so each preview reads as one line. */
object PomodoroPreviewData {
    private const val SECONDS_PER_MINUTE = 60L

    val focus =
        UiState(
            min = 24,
            second = 57,
            mode = PomodoroMode.Focus.toUiMode(),
            totalSessionSeconds = 25L * SECONDS_PER_MINUTE,
            totalSessions = 15,
            currentSessionIndex = 2,
            isRunning = true,
        )

    val shortBreak =
        UiState(
            min = 4,
            second = 30,
            mode = PomodoroMode.ShortBreak.toUiMode(),
            totalSessionSeconds = 5L * SECONDS_PER_MINUTE,
            totalSessions = 15,
            currentSessionIndex = 5,
        )

    val longBreak =
        UiState(
            min = 18,
            second = 0,
            mode = PomodoroMode.LongBreak.toUiMode(),
            totalSessionSeconds = 20L * SECONDS_PER_MINUTE,
            totalSessions = 15,
            currentSessionIndex = 7,
            isRunning = true,
        )

    val overtime =
        UiState(
            min = 0,
            second = 42,
            mode = PomodoroMode.OverTime.toUiMode(),
            totalSessionSeconds = 25L * SECONDS_PER_MINUTE,
            totalSessions = 15,
            currentSessionIndex = 2,
            isRunning = true,
            isOvertime = true,
        )

    val paused = focus.copy(second = 30, min = 12, isRunning = false)

    val finishEarlyDialog = focus.copy(min = 8, second = 15, showFinishEarlyDialog = true)

    /** 15 focus rounds -> 29 phases; verifies the session dots wrap. */
    val manySessions = focus.copy(totalSessions = 29, currentSessionIndex = 14)

    val withFireplace = focus.copy(ambience = PomodoroAmbience.Fireplace)
    val withRain = shortBreak.copy(ambience = PomodoroAmbience.Rain, isRunning = true)
    val withHandpan = longBreak.copy(ambience = PomodoroAmbience.Handpan)
}
