package com.todoapp.mobile.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Pomodoro(
    val id: Long,
    val sessionCount: Int,
    val focusTime: Int,
    val shortBreak: Int,
    val longBreak: Int,
    val sectionCount: Int,
) {
    companion object {
        /**
         * Canonical first-launch defaults, mirroring the Pomodoro config screen's initial state.
         * Seeded lazily by PomodoroRepository.getOrCreateDefaultSettings so the running timer never
         * faces a null settings row (which previously crashed via requireNotNull).
         */
        val DEFAULT = Pomodoro(
            id = 0,
            sessionCount = 8,
            focusTime = 25,
            shortBreak = 5,
            longBreak = 20,
            sectionCount = 4,
        )
    }
}
