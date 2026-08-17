package com.todoapp.mobile.data.engine

import com.todoapp.mobile.common.pollFirst
import com.todoapp.mobile.data.notification.PomodoroServiceController
import com.todoapp.mobile.data.notification.PomodoroSessionAlarmScheduler
import com.todoapp.mobile.di.DefaultDispatcher
import com.todoapp.mobile.domain.engine.PomodoroEngine
import com.todoapp.mobile.domain.engine.PomodoroEngineState
import com.todoapp.mobile.domain.engine.PomodoroEvent
import com.todoapp.mobile.domain.engine.PomodoroMode
import com.todoapp.mobile.domain.engine.PomodoroSessionRecord
import com.todoapp.mobile.domain.engine.PomodoroSessionRecorder
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
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PomodoroEngineImpl
@Inject
constructor(
    private val serviceController: PomodoroServiceController,
    private val alarmScheduler: PomodoroSessionAlarmScheduler,
    private val recorder: PomodoroSessionRecorder,
    private val clock: Clock,
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

    // ---------------- RECORDING ----------------
    //
    // The interval currently in flight, tracked so it can be persisted at whichever boundary ends it.
    // Elapsed time is NOT tracked here — it is derived from remainingMillis at the moment of recording,
    // so there is only ever one source of truth for how far a session got.

    private var runId: String? = null
    private var activeSession: Session? = null
    private var activeIndex: Int = 0
    private var activeStartedAtMillis: Long = 0L

    override val currentRunId: String?
        get() = runId

    /**
     * Persists the interval in flight and clears it. **Idempotent by construction** — the second call
     * finds `activeSession` null and does nothing, which is what makes the matrix below safe when two
     * boundaries fire in sequence (a manual skip landing on an empty queue, say).
     *
     * Nulls [activeSession] *before* building the record, not after: an exception between the two would
     * otherwise leave the interval eligible to be recorded a second time.
     *
     * A zero-elapsed interval is skipped. Leaving `prepare()` before the first tick must leave no trace,
     * or every chart fills with meaningless zeros.
     */
    private fun recordActive(completed: Boolean) {
        val session = activeSession ?: return
        val run = runId ?: return
        activeSession = null

        val plannedMillis = session.durationSeconds * MILLIS_PER_SECOND
        val elapsedSeconds = (plannedMillis - remainingMillis)
            .coerceIn(ZERO_MILLIS, plannedMillis) / MILLIS_PER_SECOND
        if (elapsedSeconds <= ZERO_SECONDS) return

        recorder.record(
            PomodoroSessionRecord(
                clientSessionId = UUID.randomUUID().toString(),
                clientRunId = run,
                sessionIndex = activeIndex,
                mode = session.mode,
                plannedSeconds = session.durationSeconds,
                elapsedSeconds = elapsedSeconds,
                completed = completed,
                startedAtMillis = activeStartedAtMillis,
                endedAtMillis = clock.millis(),
            ),
        )
    }

    /** Closes the run so the next [setSessionQueue] starts a fresh one, and cues the recorder to flush. */
    private fun endRun() {
        runId = null
        recorder.onRunEnded()
    }

    // ---------------- QUEUE ----------------

    override fun setSessionQueue(queue: ArrayDeque<Session>) {
        // Replacing the queue abandons whatever was running, including the "end session" dialog's path
        // of handing in an empty queue. Record it before the old run is forgotten.
        recordActive(completed = false)

        sessionQueue.clear()
        queue.forEach { sessionQueue.addLast(it) }
        _sessionsSnapshot = queue.toList()
        totalSessionsCount = queue.size
        sessionIndexCounter = -1
        hasStartedAnySession = false

        if (queue.isEmpty()) {
            endRun()
        } else {
            runId = UUID.randomUUID().toString()
        }

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
        recordActive(completed = false)
        endRun()
        cancelRunningJobs()
        _state.update { it.copy(isRunning = false) }
        alarmScheduler.cancel()
        serviceController.stop()
        emitIfSubscribed(PomodoroEvent.PomodoroFinished)
    }

    override fun stop(record: Boolean) {
        if (record) recordActive(completed = false)
        endRun()
        cancelRunningJobs()
        _state.update { it.copy(isRunning = false) }
        alarmScheduler.cancel()
        serviceController.stop()
        // No event on purpose. This is the difference from finish(): a listener must not be told the
        // pomodoro "finished" when the run is being torn down for some other reason.
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
        // Discards the interval in flight WITHOUT recording it, which is the whole contract of this
        // method: PomodoroViewModel.init calls it defensively on every screen mount, so recording here
        // would invent rows for sessions nobody ran. Clearing is still required — leaving activeSession
        // set would hand it to the recordActive() inside the setSessionQueue() that follows.
        activeSession = null
        runId = null
        activeIndex = 0
        activeStartedAtMillis = 0L
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
        // The one and only place a natural completion is detected, so the one and only place a session
        // is recorded as completed. Before sessionIndexCounter moves, so the row carries the index of
        // the interval that just ended rather than the one about to start.
        recordActive(completed = true)

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

        // Arriving from overtime means startOvertime() already recorded this interval as completed, so
        // recording it again would inflate every focus figure and stay invisible until someone noticed
        // 40-minute pomodoros in a 20-minute run.
        //
        // This is the SECOND of two independent guards, and measured to be redundant on its own: the
        // first is recordActive() nulling activeSession before it builds the record, which makes a
        // repeat call a no-op. Removing either one alone leaves the behaviour correct and every test
        // green — verified by mutation, both ways round. Only removing BOTH double-counts, and then
        // PomodoroEngineRecordingTest reports 8 rows for a 4-session run.
        //
        // Kept anyway. Defence in depth is cheap here, and the explicit branch says what the flow means
        // where the idempotency only says what it does.
        if (!wasOvertime) recordActive(completed = false)

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
            // The run is over: its last interval was already recorded by whichever boundary got here.
            endRun()
            updateBannerVisibility(false)
            return
        }

        hasStartedAnySession = true

        // When coming from overtime, startOvertime() already incremented the counter.
        // Only increment here for initial prepare() and manual skips mid-session.
        if (!wasOvertime) {
            sessionIndexCounter++
        }

        // Becomes the interval in flight now that the counter is final for this session.
        activeSession = next
        activeIndex = sessionIndexCounter.coerceAtLeast(0)
        activeStartedAtMillis = clock.millis()

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
