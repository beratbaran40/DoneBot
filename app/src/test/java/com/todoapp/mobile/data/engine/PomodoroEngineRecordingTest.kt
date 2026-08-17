package com.todoapp.mobile.data.engine

import com.todoapp.mobile.data.notification.PomodoroServiceController
import com.todoapp.mobile.data.notification.PomodoroSessionAlarmScheduler
import com.todoapp.mobile.domain.engine.PomodoroMode
import com.todoapp.mobile.domain.engine.PomodoroSessionRecord
import com.todoapp.mobile.domain.engine.PomodoroSessionRecorder
import com.todoapp.mobile.domain.engine.Session
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * The recording matrix, which is the part of this feature that can be wrong without anyone noticing.
 *
 * Two failure modes matter more than the rest and both have their own case here:
 *
 * **Double counting.** `startOvertime()` records a session as completed, and the `startNextSession()`
 * that follows must not record it again. A regression there inflates every focus figure and stays
 * invisible until somebody notices 40-minute pomodoros in a 20-minute run.
 *
 * Verified by mutation rather than assumed. The engine carries two independent guards — the
 * `!wasOvertime` branch and `recordActive()` nulling `activeSession` before building the record — and
 * **each is sufficient alone**, so removing either leaves this suite green. Removing both turns
 * "a full run records one row per session" red with 8 rows for 4 sessions. That is the contract these
 * cases pin: one row per session at every boundary, whatever the mechanism.
 *
 * **Phantom rows.** `resetState()` runs on every screen mount, so recording there would invent sessions
 * nobody ran.
 *
 * The engine is constructed directly with a [StandardTestDispatcher] so its internal countdown runs on
 * virtual time — a 25-minute session is advanced instantly and exactly, with no sleeping.
 *
 * **Never call `advanceUntilIdle()` here.** Once a session reaches zero the engine enters overtime, which
 * is an unbounded `while (true) { delay(TICK) }` loop: the scheduler is never idle again, so the test
 * would spin forever on virtual time without ever failing. Advance by an explicit duration, then
 * `runCurrent()`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PomodoroEngineRecordingTest {

    private val dispatcher = StandardTestDispatcher()
    private val recorder = mockk<PomodoroSessionRecorder>(relaxed = true)
    private val serviceController = mockk<PomodoroServiceController>(relaxed = true)
    private val alarmScheduler = mockk<PomodoroSessionAlarmScheduler>(relaxed = true)
    private val clock = Clock.fixed(Instant.parse("2026-08-17T10:00:00Z"), ZoneOffset.UTC)

    private lateinit var engine: PomodoroEngineImpl

    @Before
    fun setUp() {
        engine = PomodoroEngineImpl(serviceController, alarmScheduler, recorder, clock, dispatcher)
    }

    // ---------------------------------------------------------------- the happy path

    @Test
    fun `a session that runs to zero is recorded as completed with its full length`() = engineTest {
        engine.setSessionQueue(queueOf(Session(FOCUS_SECONDS, PomodoroMode.Focus)))
        engine.prepare()
        engine.start()

        advanceTimeBy(FOCUS_SECONDS * MILLIS_PER_SECOND + MILLIS_PER_SECOND)
        runCurrent()

        val record = capturedRecords().single()
        assertTrue("a countdown that reached zero must be completed", record.completed)
        assertEquals(FOCUS_SECONDS, record.elapsedSeconds)
        assertEquals(FOCUS_SECONDS, record.plannedSeconds)
        assertEquals(PomodoroMode.Focus, record.mode)
        assertEquals(0, record.sessionIndex)
    }

    @Test
    fun `a full run records one row per session, in order, under one run id`() = engineTest {
        engine.setSessionQueue(
            queueOf(
                Session(FOCUS_SECONDS, PomodoroMode.Focus),
                Session(BREAK_SECONDS, PomodoroMode.ShortBreak),
                Session(FOCUS_SECONDS, PomodoroMode.Focus),
                Session(BREAK_SECONDS, PomodoroMode.LongBreak),
            ),
        )
        engine.prepare()
        engine.start()

        // Each session ends naturally, and the overtime that follows is skipped to reach the next one.
        repeat(4) {
            advanceTimeBy(FOCUS_SECONDS * MILLIS_PER_SECOND + MILLIS_PER_SECOND)
            runCurrent()
            engine.skip(autoStart = true)
            runCurrent()
        }

        val records = capturedRecords()
        assertEquals("one row per session, no more", 4, records.size)
        assertEquals(listOf(0, 1, 2, 3), records.map { it.sessionIndex })
        assertEquals("every session of a sitting shares one run id", 1, records.map { it.clientRunId }.distinct().size)
        assertTrue("client session ids must be unique", records.map { it.clientSessionId }.distinct().size == 4)
        assertTrue("all four ran to completion", records.all { it.completed })
    }

    // ---------------------------------------------------------------- double counting

    @Test
    fun `skipping out of overtime does not record the session a second time`() = engineTest {
        engine.setSessionQueue(
            queueOf(
                Session(FOCUS_SECONDS, PomodoroMode.Focus),
                Session(BREAK_SECONDS, PomodoroMode.ShortBreak),
            ),
        )
        engine.prepare()
        engine.start()

        // Reach zero: startOvertime() records the focus session as completed.
        advanceTimeBy(FOCUS_SECONDS * MILLIS_PER_SECOND + MILLIS_PER_SECOND)
        runCurrent()
        assertEquals("startOvertime must record exactly once", 1, capturedRecords().size)

        // Now leave overtime. This is the exact path that would double-count.
        engine.skip(autoStart = false)
        runCurrent()

        assertEquals("leaving overtime must add nothing", 1, capturedRecords().size)
    }

    // ---------------------------------------------------------------- partial sessions

    @Test
    fun `skipping mid-session records what actually ran, not what was planned`() = engineTest {
        engine.setSessionQueue(
            queueOf(
                Session(FOCUS_SECONDS, PomodoroMode.Focus),
                Session(BREAK_SECONDS, PomodoroMode.ShortBreak),
            ),
        )
        engine.prepare()
        engine.start()

        advanceTimeBy(30 * MILLIS_PER_SECOND)
        runCurrent()
        engine.skip(autoStart = false)
        runCurrent()

        val record = capturedRecords().single()
        assertEquals(30L, record.elapsedSeconds)
        assertEquals(FOCUS_SECONDS, record.plannedSeconds)
        assertTrue("an abandoned session must not read as completed", !record.completed)
    }

    @Test
    fun `ending the run early records the partial session and closes the run`() = engineTest {
        engine.setSessionQueue(queueOf(Session(FOCUS_SECONDS, PomodoroMode.Focus)))
        engine.prepare()
        engine.start()
        advanceTimeBy(45 * MILLIS_PER_SECOND)
        runCurrent()

        engine.stop(record = true)
        runCurrent()

        assertEquals(45L, capturedRecords().single().elapsedSeconds)
        assertEquals("the run is over", null, engine.currentRunId)
        verify { recorder.onRunEnded() }
    }

    // ---------------------------------------------------------------- nothing must be recorded

    @Test
    fun `stopping without recording writes nothing`() = engineTest {
        engine.setSessionQueue(queueOf(Session(FOCUS_SECONDS, PomodoroMode.Focus)))
        engine.prepare()
        engine.start()
        advanceTimeBy(60 * MILLIS_PER_SECOND)
        runCurrent()

        // The sign-out path. A row written here could land after the sign-out wipe and leak into the
        // next account.
        engine.stop(record = false)
        runCurrent()

        assertTrue("sign-out must not persist the session in flight", capturedRecords().isEmpty())
    }

    @Test
    fun `resetState writes nothing and leaves no session behind to be recorded later`() = engineTest {
        engine.setSessionQueue(queueOf(Session(FOCUS_SECONDS, PomodoroMode.Focus)))
        engine.prepare()
        engine.start()
        advanceTimeBy(60 * MILLIS_PER_SECOND)
        runCurrent()

        // PomodoroViewModel.init calls this on every screen mount.
        engine.resetState()
        runCurrent()
        assertTrue("resetState must record nothing", capturedRecords().isEmpty())

        // And the discarded session must not resurface through the next queue swap.
        engine.setSessionQueue(queueOf(Session(FOCUS_SECONDS, PomodoroMode.Focus)))
        runCurrent()
        assertTrue("a reset session must not be recorded by the next setSessionQueue", capturedRecords().isEmpty())
    }

    @Test
    fun `a session abandoned before the first tick leaves no row`() = engineTest {
        engine.setSessionQueue(queueOf(Session(FOCUS_SECONDS, PomodoroMode.Focus)))
        engine.prepare()
        // Never started; zero elapsed. Recording it would fill every chart with meaningless zeros.
        engine.stop(record = true)
        runCurrent()

        assertTrue(capturedRecords().isEmpty())
    }

    @Test
    fun `pausing keeps the session alive rather than recording it`() = engineTest {
        engine.setSessionQueue(queueOf(Session(FOCUS_SECONDS, PomodoroMode.Focus)))
        engine.prepare()
        engine.start()
        advanceTimeBy(60 * MILLIS_PER_SECOND)
        runCurrent()

        engine.pause()
        runCurrent()
        assertTrue("a paused session is still running, not finished", capturedRecords().isEmpty())

        // It is recorded when it actually ends, once, with the elapsed time it had.
        engine.stop(record = true)
        runCurrent()
        assertEquals(60L, capturedRecords().single().elapsedSeconds)
    }

    // ---------------------------------------------------------------- run lifecycle

    @Test
    fun `handing in an empty queue closes the run exactly once`() = engineTest {
        engine.setSessionQueue(queueOf(Session(FOCUS_SECONDS, PomodoroMode.Focus)))
        assertNotNull("a queued run has an id", engine.currentRunId)

        engine.setSessionQueue(ArrayDeque())
        runCurrent()

        assertEquals(null, engine.currentRunId)
        verify(exactly = 1) { recorder.onRunEnded() }
    }

    // ---------------------------------------------------------------- helpers

    /**
     * Every case runs through here instead of `runTest` directly, and the engine is shut down **inside**
     * the body.
     *
     * The engine owns a scope on the same TestCoroutineScheduler, and overtime is an unbounded
     * `while (true) { delay(TICK) }`. When the body returns, `runTest` drains the scheduler — so a test
     * that ends while overtime is still live spins on virtual time forever. Measured, not theorised:
     * four Gradle workers sat at 93% CPU for twenty minutes and not one test ever reported.
     *
     * An `@After` hook cannot fix this. JUnit runs it after `runTest` has already returned, which is
     * after the drain that hangs.
     */
    private fun engineTest(body: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        try {
            body()
        } finally {
            engine.shutdown()
        }
    }

    private fun capturedRecords(): List<PomodoroSessionRecord> {
        val slot = mutableListOf<PomodoroSessionRecord>()
        verify(atLeast = 0) { recorder.record(capture(slot)) }
        return slot
    }

    private fun queueOf(vararg sessions: Session) = ArrayDeque(sessions.toList())

    private companion object {
        const val FOCUS_SECONDS = 1500L
        const val BREAK_SECONDS = 300L
        const val MILLIS_PER_SECOND = 1000L
    }
}
