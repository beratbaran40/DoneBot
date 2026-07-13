package com.todoapp.mobile.domain.usecase

import com.todoapp.mobile.domain.repository.HealthCheckpoint
import com.todoapp.mobile.domain.repository.HealthPointsPreferences
import com.todoapp.mobile.domain.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Pins the derived health-points mechanic: settle ended days once (idempotent), then overlay today
 * as a live +1 opportunity, and level-trigger the depletion dialog. Recurring completions must count
 * as "active" (the old numeric streak ignored them). Pure JVM with a fixed clock.
 */
class ComputeHealthPointsUseCaseTest {
    private val repository = mockk<TaskRepository>(relaxed = true)
    private val preferences = mockk<HealthPointsPreferences>(relaxed = true)
    private val clock = Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)
    private val useCase = ComputeHealthPointsUseCase(repository, preferences, clock)

    private fun stubToday(active: Boolean) {
        every { repository.observeCompletedCountsByDateRange(eq(TODAY), eq(TODAY), any()) } returns
            flowOf(if (active) mapOf(TODAY to 1) else emptyMap())
    }

    private fun stubSettleWindow(counts: Map<LocalDate, Int>) {
        every { repository.observeCompletedCountsByDateRange(any(), eq(YESTERDAY), any()) } returns flowOf(counts)
    }

    @Test
    fun `first run starts full and seeds the checkpoint at yesterday`() = runTest {
        coEvery { preferences.getCheckpoint() } returns HealthCheckpoint(0, lastSettledEpochDay = null, dialogShown = false)
        every { preferences.observeDialogShown() } returns flowOf(false)
        stubToday(active = false)

        val result = useCase().first()

        assertEquals(MAX, result.halfHearts)
        assertFalse(result.showDepletionDialog)
        coVerify { preferences.setCheckpoint(MAX, YESTERDAY.toEpochDay(), false) }
    }

    @Test
    fun `an ended active day gains a half-heart`() = runTest {
        coEvery { preferences.getCheckpoint() } returns HealthCheckpoint(10, TWO_DAYS_AGO.toEpochDay(), false)
        every { preferences.observeDialogShown() } returns flowOf(false)
        stubSettleWindow(mapOf(YESTERDAY to 2))
        stubToday(active = false)

        val result = useCase().first()

        assertEquals(11, result.halfHearts)
        coVerify { preferences.setCheckpoint(11, YESTERDAY.toEpochDay(), false) }
    }

    @Test
    fun `an ended idle day loses a half-heart`() = runTest {
        coEvery { preferences.getCheckpoint() } returns HealthCheckpoint(10, TWO_DAYS_AGO.toEpochDay(), false)
        every { preferences.observeDialogShown() } returns flowOf(false)
        stubSettleWindow(emptyMap())
        stubToday(active = false)

        assertEquals(9, useCase().first().halfHearts)
    }

    @Test
    fun `today completion adds a live half-heart on top of settled`() = runTest {
        coEvery { preferences.getCheckpoint() } returns HealthCheckpoint(8, YESTERDAY.toEpochDay(), false)
        every { preferences.observeDialogShown() } returns flowOf(false)
        stubToday(active = true)

        assertEquals(9, useCase().first().halfHearts)
    }

    @Test
    fun `depletion dialog shows when display is zero and not acknowledged`() = runTest {
        coEvery { preferences.getCheckpoint() } returns HealthCheckpoint(0, YESTERDAY.toEpochDay(), false)
        every { preferences.observeDialogShown() } returns flowOf(false)
        stubToday(active = false)

        val result = useCase().first()

        assertEquals(0, result.halfHearts)
        assertTrue(result.showDepletionDialog)
    }

    @Test
    fun `depletion dialog stays hidden once acknowledged`() = runTest {
        coEvery { preferences.getCheckpoint() } returns HealthCheckpoint(0, YESTERDAY.toEpochDay(), true)
        every { preferences.observeDialogShown() } returns flowOf(true)
        stubToday(active = false)

        assertFalse(useCase().first().showDepletionDialog)
    }

    @Test
    fun `climbing back above zero re-arms the dialog flag`() = runTest {
        coEvery { preferences.getCheckpoint() } returns HealthCheckpoint(0, TWO_DAYS_AGO.toEpochDay(), true)
        every { preferences.observeDialogShown() } returns flowOf(true)
        stubSettleWindow(mapOf(YESTERDAY to 1))
        stubToday(active = false)

        useCase().first()

        coVerify { preferences.setCheckpoint(1, YESTERDAY.toEpochDay(), false) }
    }

    @Test
    fun `active-day detection includes recurring completions`() = runTest {
        coEvery { preferences.getCheckpoint() } returns HealthCheckpoint(10, TWO_DAYS_AGO.toEpochDay(), false)
        every { preferences.observeDialogShown() } returns flowOf(false)
        stubSettleWindow(mapOf(YESTERDAY to 1))
        stubToday(active = false)

        val result = useCase().first()

        // A recurring-only completion (fused into the count) must count the day as active.
        assertEquals(11, result.halfHearts)
        verify { repository.observeCompletedCountsByDateRange(any(), eq(YESTERDAY), eq(true)) }
    }

    private companion object {
        const val MAX = 24
        val TODAY: LocalDate = LocalDate.of(2026, 7, 13)
        val YESTERDAY: LocalDate = LocalDate.of(2026, 7, 12)
        val TWO_DAYS_AGO: LocalDate = LocalDate.of(2026, 7, 11)
    }
}
