package com.todoapp.mobile.domain.repository

import com.todoapp.mobile.domain.model.Pomodoro

interface PomodoroRepository {
    suspend fun getSavedPomodoroSettings(): Pomodoro?

    suspend fun updatePomodoro(pomodoro: Pomodoro)

    suspend fun insertPomodoro(pomodoro: Pomodoro)

    /**
     * Returns the saved settings, seeding [Pomodoro.DEFAULT] on first access if none exist yet.
     * Guarantees a non-null result so the running timer can't crash on a missing first-launch seed.
     */
    suspend fun getOrCreateDefaultSettings(): Pomodoro
}
