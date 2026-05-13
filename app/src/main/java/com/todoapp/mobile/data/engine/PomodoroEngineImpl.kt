package com.todoapp.mobile.data.engine

import com.todoapp.mobile.common.pollFirst
import com.todoapp.mobile.data.notification.PomodoroServiceController
import com.todoapp.mobile.data.notification.PomodoroSessionAlarmScheduler
import com.todoapp.mobile.di.DefaultDispatcher
import com.todoapp.mobile.domain.engine.PomodoroEngine
import com.todoapp.mobile.domain.engine.PomodoroEngineState
import com.todoapp.mobile.domain.engine.PomodoroEvent
import com.todoapp.mobile.domain.engine.PomodoroMode
import com.todoapp.mobile.domain.engine.Session
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PomodoroEngineImpl
@Inject
constructor(
    private val serviceController: PomodoroServiceController,
    private val alarmScheduler: PomodoroSessionAlarmScheduler,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : PomodoroEngine {
    private val scope = CoroutineScope(SupervisorJob() + defaultDispatcher)

    private val _state = MutableStateFlow(PomodoroEngineState())
    override val state = _state.asStateFlow()

    // replay=0 already prevents new subscribers from seeing past emissions
    // (verified against Kotlin's SharedFlow semantics). The buffer is sized to
    // absorb fast back-to-back emissions while a subscriber is busy handling
    // the previous one — without it, tryEmit can drop SessionFinished mid-tick,
    // breaking the in-screen ringtone/transition path. The hasStartedAnySession
    // guard in startNextSession() is what actually prevents stale state from
    // re-emitting PomodoroFinished on a fresh launch.
    private val _events =
        MutableSharedFlow<PomodoroEvent>(
            replay = 0,
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    override val events = _events.asSharedFlow()

    private val sessionQueue: ArrayDeque<Session> = ArrayDeque()

    // Immutable copy of the queue from the most recent setSessionQueue() call.
    // sessionQueue itself is mutated as sessions are popped, so it can't be used
    // to recover the original durations / count after the run starts.
    private var _sessionsSnapshot: List<Session> = emptyList()
    override val sessionsSnapshot: List<Session>
        get() = _sessionsSnapshot

    private var timerJob: Job? = null
    private var overtimeJob: Job? = null

    private var remainingMillis: Long = 0L
    private var overtimeMillis: Long = 0L

    private var totalSessionsCount: Int = 0
    private var sessionIndexCounter: Int = -1

    // True only after a session has actually been popped off the queue. Prevents
    // PomodoroFinished from being emitted when prepare()/skip() lands on an empty
    // queue WITHOUT having started anything (e.g. after setSessionQueue(ArrayDeque())
    // or any leftover state from a previous run that the new ViewModel observes).
    private var hasStartedAnySession: Boolean = false

    // ---------------- QUEUE ----------------

    override fun setSessionQueue(queue: ArrayDeque<Session>) {
        sessionQueue.clear()
        queue.forEach { sessionQueue.addLast(it) }
        _sessionsSnapshot = queue.toList()
        totalSessionsCount = queue.size
        sessionIndexCounter = -1
        hasStartedAnySession = false
        _state.update { it.copy(totalSessions = totalSessionsCount, currentSessionIndex = 0) }
    }

    override fun prepare() {
        startNextSession(autoStart = false)
    }

    // ---------------- CONTROLS ----------------

    override fun start() {
        if (_state.value.isOvertime) return
        startCountdown()
        _state.update { it.copy(isRunning = true) }
        serviceController.start()
        scheduleEndAlarm(remainingMillis)
    }

    override fun pause() {
        cancelRunningJobs()
        _state.update { it.copy(isRunning = false) }
        alarmScheduler.cancel()
    }

    override fun skip(autoStart: Boolean) {
        alarmScheduler.cancel()
        startNextSession(autoStart)
    }

    override fun finish() {
        cancelRunningJobs()
        _state.update { it.copy(isRunning = false) }
        alarmScheduler.cancel()
        serviceController.stop()
        emitIfSubscribed(PomodoroEvent.PomodoroFinished)
    }

    /**
     * Drop the event entirely if no PomodoroViewModel is currently collecting.
     * Without this, [extraBufferCapacity] would queue the emission and hand it
     * to the next subscriber that connects (e.g. on a fresh Pomodoro launch),
     * sending the user straight to Summary without ever starting a session.
     */
    private fun emitIfSubscribed(event: PomodoroEvent) {
        if (_events.subscriptionCount.value > 0) {
            _events.tryEmit(event)
        }
    }

    override fun updateBannerVisibility(isVisible: Boolean) {
        _state.update { it.copy(isVisible = isVisible) }
    }

    override fun shutdown() {
        cancelRunningJobs()
        alarmScheduler.cancel()
        serviceController.stop()
        scope.cancel()
    }

    override fun resetState() {
        cancelRunningJobs()
        sessionQueue.clear()
        _sessionsSnapshot = emptyList()
        sessionIndexCounter = -1
        totalSessionsCount = 0
        remainingMillis = ZERO_MILLIS
        overtimeMillis = ZERO_MILLIS
        hasStartedAnySession = false
        _state.value = PomodoroEngineState()
        alarmScheduler.cancel()
        serviceController.stop()
    }

    private fun scheduleEndAlarm(remainingMs: Long) {
        if (remainingMs <= ZERO_MILLIS) return
        alarmScheduler.scheduleAt(System.currentTimeMillis() + remainingMs)
    }

    // ---------------- CORE LOGIC ----------------

    private fun startCountdown() {
        if (timerJob?.isActive == true) return
        if (remainingMillis <= ZERO_MILLIS) return

        timerJob =
            scope.launch {
                runCountdown()
            }
    }

    private fun startOvertime() {
        overtimeJob?.cancel()
        alarmScheduler.cancel()
        overtimeMillis = ZERO_MILLIS
        sessionIndexCounter++

        _state.update {
            it.copy(
                isOvertime = true,
                isRunning = true,
                mode = PomodoroMode.OverTime,
                currentSessionIndex = sessionIndexCounter,
            )
        }

        emitIfSubscribed(PomodoroEvent.SessionFinished)

        overtimeJob =
            scope.launch {
                runOvertime()
            }
    }

    private fun startNextSession(autoStart: Boolean) {
        val wasOvertime = _state.value.isOvertime
        pause()
        overtimeMillis = ZERO_MILLIS

        _state.update { it.copy(isOvertime = false) }

        val next = sessionQueue.pollFirst()
        if (next == null) {
            alarmScheduler.cancel()
            serviceController.stop()
            // Only emit PomodoroFinished if we actually started at least one session in
            // this run. Without this guard, calling prepare() on an empty queue (e.g.
            // observed leftover state) would navigate to Summary on a fresh launch.
            if (hasStartedAnySession) {
                emitIfSubscribed(PomodoroEvent.PomodoroFinished)
            }
            updateBannerVisibility(false)
            return
        }

        hasStartedAnySession = true

        // When coming from overtime, startOvertime() already incremented the counter.
        // Only increment here for initial prepare() and manual skips mid-session.
        if (!wasOvertime) {
            sessionIndexCounter++
        }

        remainingMillis = next.durationSeconds * MILLIS_PER_SECOND
        _state.update {
            it.copy(
                mode = next.mode,
                currentSessionIndex = sessionIndexCounter.coerceAtLeast(0),
                currentSessionTotalSeconds = next.durationSeconds,
            )
        }
        publishRemaining(remainingMillis)

        if (autoStart) start()
    }

    private fun onSessionFinished() {
        startOvertime()
    }

    private fun cancelRunningJobs() {
        timerJob?.cancel()
        overtimeJob?.cancel()
    }

    private suspend fun runCountdown() {
        publishRemaining(remainingMillis)

        while (currentCoroutineContext().isActive && remainingMillis > ZERO_MILLIS) {
            delay(TICK_MILLIS)
            remainingMillis = (remainingMillis - TICK_MILLIS).coerceAtLeast(ZERO_MILLIS)
            publishRemaining(remainingMillis)
        }

        if (currentCoroutineContext().isActive && remainingMillis == ZERO_MILLIS) {
            onSessionFinished()
        }
    }

    private suspend fun runOvertime() {
        while (currentCoroutineContext().isActive) {
            delay(TICK_MILLIS)
            overtimeMillis += TICK_MILLIS
            publishRemaining(overtimeMillis)
        }
    }

    // ---------------- TIME ----------------

    private fun publishRemaining(millis: Long) {
        val totalSeconds = (millis / MILLIS_PER_SECOND).coerceAtLeast(ZERO_SECONDS)
        _state.update { it.copy(remainingSeconds = totalSeconds) }
    }

    private companion object {
        const val TICK_MILLIS: Long = 1_000L
        const val MILLIS_PER_SECOND: Long = 1_000L
        const val ZERO_MILLIS: Long = 0L
        const val ZERO_SECONDS: Long = 0L
    }
}
