package com.todoapp.mobile.data.ambience

import com.todoapp.mobile.di.MainDispatcher
import com.todoapp.mobile.domain.ambience.AmbiencePlayer
import com.todoapp.mobile.domain.ambience.PomodoroAmbience
import com.todoapp.mobile.domain.engine.PomodoroEngine
import com.todoapp.mobile.domain.repository.AmbiencePreferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decides when ambience plays. Everything that could reasonably want a say — the engine, the
 * user's preferences, whether the app is on screen — is folded into one rule here rather than
 * scattered across a ViewModel, a service and a screen.
 *
 * It can't live in `PomodoroViewModel`: that dies the moment the user navigates away, while the
 * timer keeps running app-wide behind the banner. Ambience has to follow the session, not the
 * screen.
 */
@Singleton
class AmbienceCoordinator
@Inject
constructor(
    private val engine: PomodoroEngine,
    private val player: AmbiencePlayer,
    private val preferences: AmbiencePreferences,
    // Main, not Default: MediaPlayer is not thread-safe and delivers its callbacks on the main
    // looper. Driving it from a background thread would race its own prepare callback.
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
) {
    private val scope = CoroutineScope(SupervisorJob() + mainDispatcher)
    private val appForeground = MutableStateFlow(true)
    private var job: Job? = null

    /** Called once from `Application.onCreate`. Idempotent. */
    fun start() {
        if (job?.isActive == true) return
        job =
            combine(
                engine.state.map { it.isRunning }.distinctUntilChanged(),
                preferences.observeSelection(),
                preferences.observePlayInBackground(),
                appForeground,
            ) { isRunning, selection, playInBackground, foreground ->
                Decision(
                    ambience = selection,
                    shouldPlay = isRunning && selection != PomodoroAmbience.None && (playInBackground || foreground),
                )
            }.distinctUntilChanged()
                .onEach(::apply)
                .launchIn(scope)

        // Volume rides separately: dragging the slider must not restart the loop.
        preferences
            .observeVolume()
            .onEach(player::setVolume)
            .launchIn(scope)
    }

    /** Fed by `Application`'s ProcessLifecycleOwner observer. */
    fun onAppForegrounded() {
        appForeground.value = true
    }

    fun onAppBackgrounded() {
        appForeground.value = false
    }

    fun shutdown() {
        job?.cancel()
        job = null
        scope.cancel()
    }

    private fun apply(decision: Decision) {
        val playback = player.state.value
        when {
            // Silence is a decision to be done with it, not a lull — release the decoder rather
            // than holding one open for a bed the user turned off.
            decision.ambience == PomodoroAmbience.None ->
                if (playback.current != PomodoroAmbience.None) player.stop()
            // Paused timer or a backgrounded app: hold position so resuming picks the loop back up.
            !decision.shouldPlay -> if (playback.isPlaying) player.pause()
            // Switching soundscape mid-session: play() swaps the source and fades in.
            playback.current != decision.ambience -> player.play(decision.ambience)
            playback.isPlaying -> Unit
            else -> player.resume()
        }
    }

    private data class Decision(
        val ambience: PomodoroAmbience,
        val shouldPlay: Boolean,
    )
}
