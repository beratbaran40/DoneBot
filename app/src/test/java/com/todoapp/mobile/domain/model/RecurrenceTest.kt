package com.todoapp.mobile.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class RecurrenceTest {
    private val anchor: LocalDate = LocalDate.of(2026, 6, 1) // a Monday

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

    // region interval

    @Test
    fun `daily interval fires every nth day from the anchor`() {
        val rule = RecurrenceRule(Recurrence.DAILY, interval = 2)
        assertTrue(rule.firesOn(anchor, anchor))
        assertFalse(rule.firesOn(anchor, anchor.plusDays(1)))
        assertTrue(rule.firesOn(anchor, anchor.plusDays(2)))
        assertFalse(rule.firesOn(anchor, anchor.plusDays(3)))
    }

    @Test
    fun `daily interval stays phased across a month boundary`() {
        val rule = RecurrenceRule(Recurrence.DAILY, interval = 3)
        // June has 30 days, so anchor+30 is July 1 and 30 % 3 == 0.
        assertTrue(rule.firesOn(anchor, anchor.plusDays(30)))
        assertFalse(rule.firesOn(anchor, anchor.plusDays(31)))
    }

    @Test
    fun `an interval below one is treated as one`() {
        val rule = RecurrenceRule(Recurrence.DAILY, interval = 0)
        assertTrue(rule.firesOn(anchor, anchor.plusDays(1)))
        assertTrue(rule.firesOn(anchor, anchor.plusDays(2)))
    }

    @Test
    fun `monthly and yearly honour the interval`() {
        val monthly = RecurrenceRule(Recurrence.MONTHLY, interval = 3)
        assertTrue(monthly.firesOn(anchor, anchor.plusMonths(3)))
        assertFalse(monthly.firesOn(anchor, anchor.plusMonths(1)))

        val yearly = RecurrenceRule(Recurrence.YEARLY, interval = 2)
        assertTrue(yearly.firesOn(anchor, anchor.plusYears(2)))
        assertFalse(yearly.firesOn(anchor, anchor.plusYears(1)))
    }

    // endregion

    // region byDay

    @Test
    fun `byDay fires only on the listed weekdays regardless of the anchor weekday`() {
        val rule =
            RecurrenceRule(Recurrence.WEEKLY, byDay = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
        assertTrue(rule.firesOn(anchor, anchor)) // Mon
        assertFalse(rule.firesOn(anchor, anchor.plusDays(1))) // Tue
        assertTrue(rule.firesOn(anchor, anchor.plusDays(2))) // Wed
        assertTrue(rule.firesOn(anchor, anchor.plusDays(4))) // Fri
        assertFalse(rule.firesOn(anchor, anchor.plusDays(5))) // Sat
        assertTrue(rule.firesOn(anchor, anchor.plusDays(7))) // next Mon
    }

    @Test
    fun `a byDay set that excludes the anchor weekday still fires on its own days`() {
        val rule = RecurrenceRule(Recurrence.WEEKLY, byDay = setOf(DayOfWeek.SATURDAY))
        assertFalse(rule.firesOn(anchor, anchor)) // the Monday anchor itself never fires
        assertTrue(rule.firesOn(anchor, anchor.plusDays(5))) // Saturday
    }

    @Test
    fun `byDay is ignored for daily monthly and yearly`() {
        val onlySunday = setOf(DayOfWeek.SUNDAY)
        // anchor+1 is a Tuesday; a daily rule must still fire on it.
        assertTrue(RecurrenceRule(Recurrence.DAILY, byDay = onlySunday).firesOn(anchor, anchor.plusDays(1)))
        assertTrue(RecurrenceRule(Recurrence.MONTHLY, byDay = onlySunday).firesOn(anchor, anchor.plusMonths(1)))
        assertTrue(RecurrenceRule(Recurrence.YEARLY, byDay = onlySunday).firesOn(anchor, anchor.plusYears(1)))
    }

    @Test
    fun `every two weeks with byDay is phased on the anchor ISO week`() {
        val rule = RecurrenceRule(
            Recurrence.WEEKLY,
            interval = 2,
            byDay = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
        )
        // Anchor week (week 0) fires on both days...
        assertTrue(rule.firesOn(anchor, anchor))
        assertTrue(rule.firesOn(anchor, anchor.plusDays(2)))
        // ...the next week is skipped entirely — both days, not just one.
        assertFalse(rule.firesOn(anchor, anchor.plusDays(7)))
        assertFalse(rule.firesOn(anchor, anchor.plusDays(9)))
        // ...and week 2 fires again.
        assertTrue(rule.firesOn(anchor, anchor.plusDays(14)))
        assertTrue(rule.firesOn(anchor, anchor.plusDays(16)))
    }

    // endregion

    // region until

    @Test
    fun `until includes the end day itself and cuts off after it`() {
        val until = anchor.plusDays(29) // a 30-day course
        val rule = RecurrenceRule(Recurrence.DAILY, until = until)
        assertTrue(rule.firesOn(anchor, until))
        assertFalse(rule.firesOn(anchor, until.plusDays(1)))
    }

    @Test
    fun `until and finishedOn both apply and the earlier one wins`() {
        val until = anchor.plusDays(29)
        val finishedOn = anchor.plusDays(9)
        val rule = RecurrenceRule(Recurrence.DAILY, until = until)
        // Retired early: days after finishedOn stop firing even though until is later.
        assertTrue(rule.firesOn(anchor, finishedOn, finishedOn))
        assertFalse(rule.firesOn(anchor, finishedOn.plusDays(1), finishedOn))
        // The reverse: a finishedOn later than until doesn't extend the schedule.
        assertFalse(rule.firesOn(anchor, until.plusDays(1), until.plusDays(5)))
    }

    @Test
    fun `an until before the anchor means the rule never fires`() {
        val rule = RecurrenceRule(Recurrence.DAILY, until = anchor.minusDays(1))
        assertFalse(rule.firesOn(anchor, anchor))
        assertFalse(rule.firesOn(anchor, anchor.plusDays(1)))
    }

    // endregion

    /**
     * The regression net for everything already shipped: with the defaults, the rule engine must be
     * indistinguishable from the enum-only implementation this replaced. [legacyFiresOn] below is an
     * independent transcription of that old code, so this pins the semantics rather than the code.
     */
    @Test
    fun `default rule matches the legacy behaviour for every frequency across a wide window`() {
        val anchors = listOf(
            LocalDate.of(2026, 6, 1), // ordinary day, a Monday
            LocalDate.of(2026, 1, 31), // exercises the monthly day-of-month clamp
            LocalDate.of(2024, 2, 29), // exercises the yearly leap-day clamp
        )
        val finishedOnOptions = listOf(null, LocalDate.of(2026, 6, 21))
        for (testAnchor in anchors) {
            for (finishedOn in finishedOnOptions) {
                for (frequency in Recurrence.entries) {
                    for (offset in -10L..400L) {
                        val day = testAnchor.plusDays(offset)
                        assertEquals(
                            "frequency=$frequency anchor=$testAnchor day=$day finishedOn=$finishedOn",
                            legacyFiresOn(frequency, testAnchor, day, finishedOn),
                            RecurrenceRule(frequency).firesOn(testAnchor, day, finishedOn),
                        )
                    }
                }
            }
        }
    }

    /** Verbatim transcription of the pre-RecurrenceRule implementation. Do not "simplify" this. */
    private fun legacyFiresOn(
        recurrence: Recurrence,
        anchor: LocalDate,
        day: LocalDate,
        finishedOn: LocalDate?,
    ): Boolean {
        if (finishedOn != null && day.isAfter(finishedOn)) return false
        return when (recurrence) {
            Recurrence.NONE -> anchor == day
            Recurrence.DAILY -> !day.isBefore(anchor)
            Recurrence.WEEKLY -> !day.isBefore(anchor) && day.dayOfWeek == anchor.dayOfWeek
            Recurrence.MONTHLY -> !day.isBefore(anchor) &&
                day.dayOfMonth == clampedDayOfMonth(anchor.dayOfMonth, day.year, day.monthValue)
            Recurrence.YEARLY -> !day.isBefore(anchor) &&
                day.monthValue == anchor.monthValue &&
                day.dayOfMonth == clampedDayOfMonth(anchor.dayOfMonth, day.year, day.monthValue)
        }
    }
}
