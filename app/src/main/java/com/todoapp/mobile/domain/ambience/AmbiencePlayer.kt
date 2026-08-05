package com.todoapp.mobile.domain.ambience

import kotlinx.coroutines.flow.StateFlow

/**
 * Plays one looping ambience bed. A process singleton: the loop has to outlive the Pomodoro
 * screen's ViewModel, because the timer itself keeps running app-wide behind the banner.
 *
 * Nothing here decides *whether* to play — that is [AmbienceCoordinator]'s single
 * responsibility. This interface only does what it is told.
 */
interface AmbiencePlayer {
    val state: StateFlow<AmbiencePlaybackState>

    /** Starts (or switches to) [ambience], fading in. [PomodoroAmbience.None] is the same as [stop]. */
    fun play(ambience: PomodoroAmbience)

    /** Fades out and holds position, so [resume] picks the loop back up where it left off. */
    fun pause()

    fun resume()

    /** Fades out and releases the decoder. */
    fun stop()

    /** @param volume 0f..1f, applied on top of the device media volume. */
    fun setVolume(volume: Float)

    /**
     * Releases the decoder and abandons audio focus for good. Called from
     * `Application.onDestroy` alongside `PomodoroEngine.shutdown()`.
     */
    fun shutdown()
}

data class AmbiencePlaybackState(
    val current: PomodoroAmbience = PomodoroAmbience.None,
    val isPlaying: Boolean = false,
)
