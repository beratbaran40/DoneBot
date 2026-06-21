package com.todoapp.mobile.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RecurrenceTest {
    private val anchor: LocalDate = LocalDate.of(2026, 6, 1)

    @Test
    fun `daily fires on and after the anchor when not finished`() {
        assertTrue(Recurrence.DAILY.firesOn(anchor, anchor))
        assertTrue(Recurrence.DAILY.firesOn(anchor, anchor.plusDays(30)))
        assertFalse(Recurrence.DAILY.firesOn(anchor, anchor.minusDays(1)))
    }

    @Test
    fun `null finishedOn preserves the original behaviour`() {
        assertTrue(Recurrence.DAILY.firesOn(anchor, anchor.plusDays(100), finishedOn = null))
    }

    @Test
    fun `finished daily stops firing only on days after finishedOn`() {
        val finishedOn = LocalDate.of(2026, 6, 21)
        // The finish day itself and earlier days still fire (history is preserved).
        assertTrue(Recurrence.DAILY.firesOn(anchor, finishedOn, finishedOn))
        assertTrue(Recurrence.DAILY.firesOn(anchor, finishedOn.minusDays(1), finishedOn))
        // Days after the finish day no longer fire.
        assertFalse(Recurrence.DAILY.firesOn(anchor, finishedOn.plusDays(1), finishedOn))
        assertFalse(Recurrence.DAILY.firesOn(anchor, finishedOn.plusDays(30), finishedOn))
    }

    @Test
    fun `finishedOn cutoff applies before the weekly day-of-week check`() {
        val finishedOn = anchor.plusWeeks(2) // same weekday as the anchor
        val nextOccurrence = anchor.plusWeeks(3) // same weekday, but after the finish day
        assertTrue(Recurrence.WEEKLY.firesOn(anchor, finishedOn, finishedOn))
        assertFalse(Recurrence.WEEKLY.firesOn(anchor, nextOccurrence, finishedOn))
    }

    @Test
    fun `finishedOn cutoff applies to monthly and yearly`() {
        val finishedOn = anchor.plusMonths(2) // same day-of-month as the anchor
        assertTrue(Recurrence.MONTHLY.firesOn(anchor, finishedOn, finishedOn))
        assertFalse(Recurrence.MONTHLY.firesOn(anchor, anchor.plusMonths(3), finishedOn))

        val yearFinish = anchor.plusYears(1)
        assertTrue(Recurrence.YEARLY.firesOn(anchor, yearFinish, yearFinish))
        assertFalse(Recurrence.YEARLY.firesOn(anchor, anchor.plusYears(2), yearFinish))
    }
}
