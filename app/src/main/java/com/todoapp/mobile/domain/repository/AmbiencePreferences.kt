package com.todoapp.mobile.domain.repository

import com.todoapp.mobile.domain.ambience.PomodoroAmbience
import kotlinx.coroutines.flow.Flow

/**
 * The user's Pomodoro ambience choices: which soundscape, how loud, and whether it keeps playing
 * once the app leaves the foreground.
 *
 * Background playback defaults to **false** — turning it on is what makes the session a
 * `mediaPlayback` foreground service, and that should be the user's deliberate call.
 */
interface AmbiencePreferences {
    fun observeSelection(): Flow<PomodoroAmbience>

    suspend fun setSelection(value: PomodoroAmbience)

    /** 0f..1f. */
    fun observeVolume(): Flow<Float>

    suspend fun setVolume(value: Float)

    fun observePlayInBackground(): Flow<Boolean>

    suspend fun setPlayInBackground(value: Boolean)

    companion object {
        const val DEFAULT_VOLUME: Float = 0.6f
    }
}
