package com.todoapp.mobile.ambience

import android.content.Context
import com.todoapp.mobile.data.ambience.AmbienceCoordinator
import com.todoapp.mobile.data.notification.PomodoroServiceController
import com.todoapp.mobile.domain.ambience.AmbiencePlaybackState
import com.todoapp.mobile.domain.ambience.AmbiencePlayer
import com.todoapp.mobile.domain.ambience.PomodoroAmbience
import com.todoapp.mobile.domain.engine.PomodoroEngine
import com.todoapp.mobile.domain.engine.PomodoroEngineState
import com.todoapp.mobile.domain.engine.PomodoroMode
import com.todoapp.mobile.domain.repository.AmbiencePreferences
import com.todoapp.mobile.util.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * The coordinator owns the whole "is ambience playing right now?" question, so this is the truth
 * table for it: a running timer, a chosen soundscape, and either the background toggle or the app
 * being on screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AmbienceCoordinatorTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val engineState = MutableStateFlow(PomodoroEngineState())
    private val selection = MutableStateFlow(PomodoroAmbience.None)
    private val volume = MutableStateFlow(0.6f)
    private val playInBackground = MutableStateFlow(false)
    private val playbackState = MutableStateFlow(AmbiencePlaybackState())

    private val engine =
        mockk<PomodoroEngine>(relaxed = true) {
            every { state } returns engineState
        }

    private val player =
        mockk<AmbiencePlayer>(relaxed = true) {
            every { state } returns playbackState
            every { play(any()) } answers {
                playbackState.value = AmbiencePlaybackState(firstArg(), isPlaying = true)
            }
            every { pause() } answers { playbackState.value = playbackState.value.copy(isPlaying = false) }
            every { resume() } answers { playbackState.value = playbackState.value.copy(isPlaying = true) }
        }

    private val preferences =
        mockk<AmbiencePreferences>(relaxed = true) {
            every { observeSelection() } returns selection
            every { observeVolume() } returns volume
            every { observePlayInBackground() } returns playInBackground
        }

    // The real controller rather than a mock: its whole job here is a two-state machine, and driving
    // it through the same callbacks the service uses is what makes these assertions mean anything.
    private val serviceController = PomodoroServiceController(mockk<Context>(relaxed = true))

    private fun coordinator() = AmbienceCoordinator(engine, player, preferences, serviceController, mainDispatcherRule.dispatcher)

    private fun running(isRunning: Boolean) {
        engineState.value = engineState.value.copy(isRunning = isRunning)
    }

    /**
     * What `PomodoroEngineImpl` produces for every session on a device where notifications are
     * allowed. The tests written before background playback depended on the service all assume it,
     * so it is the fixture default rather than a line repeated in each of them.
     */
    @Before
    fun serviceIsUp() {
        serviceController.onForegroundStarted()
    }

    @Test
    fun `a running timer with a chosen soundscape starts playing`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coordinator().start()
        selection.value = PomodoroAmbience.Rain
        running(true)
        advanceUntilIdle()

        verify { player.play(PomodoroAmbience.Rain) }
        assertEquals(true, playbackState.value.isPlaying)
    }

    @Test
    fun `silence never starts playback however the session goes`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coordinator().start()
        running(true)
        playInBackground.value = true
        advanceUntilIdle()

        verify(exactly = 0) { player.play(any()) }
        verify(exactly = 0) { player.resume() }
    }

    @Test
    fun `pausing the timer pauses the bed and resuming picks it back up`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coordinator().start()
        selection.value = PomodoroAmbience.Fireplace
        running(true)
        advanceUntilIdle()

        running(false)
        advanceUntilIdle()
        verify { player.pause() }
        assertEquals(false, playbackState.value.isPlaying)

        running(true)
        advanceUntilIdle()
        verify { player.resume() }
        assertEquals(true, playbackState.value.isPlaying)
    }

    @Test
    fun `with background playback off, leaving the app pauses the bed`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val coordinator = coordinator()
        coordinator.start()
        selection.value = PomodoroAmbience.Rain
        running(true)
        advanceUntilIdle()

        coordinator.onAppBackgrounded()
        advanceUntilIdle()
        verify { player.pause() }

        coordinator.onAppForegrounded()
        advanceUntilIdle()
        verify { player.resume() }
    }

    @Test
    fun `with background playback on, leaving the app keeps the bed running`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val coordinator = coordinator()
        coordinator.start()
        selection.value = PomodoroAmbience.Handpan
        playInBackground.value = true
        running(true)
        advanceUntilIdle()

        coordinator.onAppBackgrounded()
        advanceUntilIdle()

        verify(exactly = 0) { player.pause() }
        assertEquals(true, playbackState.value.isPlaying)
    }

    @Test
    fun `with no foreground service, leaving the app pauses the bed even with background playback on`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        // Notifications denied on API 33+: PomodoroServiceController.start() declines, so nothing
        // is holding this process up. Background audio without a foreground service behind it is
        // both killable mid-loop and outside what we declare to Play.
        serviceController.onForegroundStopped()

        val coordinator = coordinator()
        coordinator.start()
        selection.value = PomodoroAmbience.Rain
        playInBackground.value = true
        running(true)
        advanceUntilIdle()

        coordinator.onAppBackgrounded()
        advanceUntilIdle()

        verify { player.pause() }
        assertEquals(false, playbackState.value.isPlaying)
    }

    @Test
    fun `losing the foreground service mid-session pauses a bed that was playing in the background`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val coordinator = coordinator()
        coordinator.start()
        selection.value = PomodoroAmbience.Fireplace
        playInBackground.value = true
        running(true)
        coordinator.onAppBackgrounded()
        advanceUntilIdle()
        verify(exactly = 0) { player.pause() }

        // The service going down is not hypothetical: the user can swipe the notification's
        // channel off, or the system can tear the service down and leave the process behind.
        serviceController.onForegroundStopped()
        advanceUntilIdle()

        verify { player.pause() }
    }

    @Test
    fun `on screen, the bed plays whether or not a foreground service is up`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        // The service only matters for surviving backgrounding. With the app on screen the process
        // is alive by definition, so requiring one here would break ambience for every user who
        // declined notifications — the opposite of the fix.
        serviceController.onForegroundStopped()

        coordinator().start()
        selection.value = PomodoroAmbience.Rain
        running(true)
        advanceUntilIdle()

        verify { player.play(PomodoroAmbience.Rain) }
        assertEquals(true, playbackState.value.isPlaying)
    }

    @Test
    fun `overtime counts as running, so the bed carries across the session boundary`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coordinator().start()
        selection.value = PomodoroAmbience.Rain
        running(true)
        advanceUntilIdle()

        // The engine keeps isRunning true when a focus block rolls into overtime.
        engineState.value =
            engineState.value.copy(mode = PomodoroMode.OverTime, isOvertime = true, isRunning = true)
        advanceUntilIdle()

        verify(exactly = 0) { player.pause() }
        assertEquals(true, playbackState.value.isPlaying)
    }

    @Test
    fun `switching soundscape mid-session swaps the source instead of resuming the old one`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coordinator().start()
        selection.value = PomodoroAmbience.Rain
        running(true)
        advanceUntilIdle()

        selection.value = PomodoroAmbience.Fireplace
        advanceUntilIdle()

        verify { player.play(PomodoroAmbience.Fireplace) }
        assertEquals(PomodoroAmbience.Fireplace, playbackState.value.current)
    }

    @Test
    fun `choosing silence releases the player instead of parking it`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coordinator().start()
        selection.value = PomodoroAmbience.Rain
        running(true)
        advanceUntilIdle()

        selection.value = PomodoroAmbience.None
        advanceUntilIdle()

        // Silence means done, not paused — a paused MediaPlayer would keep a decoder open.
        verify { player.stop() }
        verify(exactly = 0) { player.pause() }
    }

    @Test
    fun `volume changes are forwarded without touching playback`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coordinator().start()
        selection.value = PomodoroAmbience.Rain
        running(true)
        advanceUntilIdle()

        volume.value = 0.25f
        advanceUntilIdle()

        verify { player.setVolume(0.25f) }
        // One play() for the initial start; a volume change must not restart the loop.
        verify(exactly = 1) { player.play(any()) }
    }
}
