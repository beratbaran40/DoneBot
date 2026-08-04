package com.todoapp.mobile.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class RecurrenceProgressTest {
    private val anchor: LocalDate = LocalDate.of(2026, 6, 1) // a Monday

    @Test
    fun `the anchor is occurrence one`() {
        assertEquals(1, RecurrenceRule(Recurrence.DAILY).occurrenceIndex(anchor, anchor))
        assertEquals(1, RecurrenceRule(Recurrence.WEEKLY).occurrenceIndex(anchor, anchor))
        assertEquals(1, RecurrenceRule(Recurrence.NONE).occurrenceIndex(anchor, anchor))
    }

    @Test
    fun `daily index counts every day`() {
        val rule = RecurrenceRule(Recurrence.DAILY)
        assertEquals(12, rule.occurrenceIndex(anchor, anchor.plusDays(11)))
        assertEquals(30, rule.occurrenceIndex(anchor, anchor.plusDays(29)))
    }

    @Test
    fun `daily index divides by the interval`() {
        val rule = RecurrenceRule(Recurrence.DAILY, interval = 2)
        assertEquals(2, rule.occurrenceIndex(anchor, anchor.plusDays(2)))
        assertEquals(15, rule.occurrenceIndex(anchor, anchor.plusDays(28)))
    }

    @Test
    fun `a day the rule does not fire on has no index`() {
        assertNull(RecurrenceRule(Recurrence.DAILY, interval = 2).occurrenceIndex(anchor, anchor.plusDays(1)))
        assertNull(RecurrenceRule(Recurrence.WEEKLY).occurrenceIndex(anchor, anchor.plusDays(1)))
        // Before the anchor is never an occurrence.
        assertNull(RecurrenceRule(Recurrence.DAILY).occurrenceIndex(anchor, anchor.minusDays(1)))
    }

    @Test
    fun `a day past the scheduled end has no index`() {
        val rule = RecurrenceRule(Recurrence.DAILY, until = anchor.plusDays(29))
        assertEquals(30, rule.occurrenceIndex(anchor, anchor.plusDays(29)))
        assertNull(rule.occurrenceIndex(anchor, anchor.plusDays(30)))
    }

    @Test
    fun `byDay index counts real firing days`() {
        val rule =
            RecurrenceRule(Recurrence.WEEKLY, byDay = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
        assertEquals(1, rule.occurrenceIndex(anchor, anchor)) // Mon
        assertEquals(3, rule.occurrenceIndex(anchor, anchor.plusDays(4))) // Fri of week 1
        assertEquals(4, rule.occurrenceIndex(anchor, anchor.plusDays(7))) // Mon of week 2
        assertEquals(6, rule.occurrenceIndex(anchor, anchor.plusDays(11))) // Fri of week 2
    }

    @Test
    fun `byDay index gives up rather than walking a huge window`() {
        val rule = RecurrenceRule(Recurrence.WEEKLY, byDay = setOf(DayOfWeek.MONDAY))
        val farMonday = anchor.plusDays(406) // still a Monday, but well past the guard
        assertNull(rule.occurrenceIndex(anchor, farMonday))
        // Without byDay the same distance is O(1) math, so it still resolves.
        assertEquals(59, RecurrenceRule(Recurrence.WEEKLY).occurrenceIndex(anchor, farMonday))
    }

    @Test
    fun `an open-ended routine has no total`() {
        assertNull(RecurrenceRule(Recurrence.DAILY).occurrenceTotal(anchor))
        assertNull(RecurrenceRule(Recurrence.WEEKLY).occurrenceTotal(anchor))
    }

    @Test
    fun `a thirty day course totals thirty`() {
        val rule = RecurrenceRule(Recurrence.DAILY, until = anchor.plusDays(29))
        assertEquals(30, rule.occurrenceTotal(anchor))
    }

    @Test
    fun `the total lands on the last firing day, not the end date`() {
        // Every other day for 30 calendar days: the 30th day isn't a firing day, the 29th is.
        val rule = RecurrenceRule(Recurrence.DAILY, interval = 2, until = anchor.plusDays(29))
        assertEquals(15, rule.occurrenceTotal(anchor))
    }

    @Test
    fun `byDay totals count only the listed weekdays`() {
        val rule = RecurrenceRule(
            Recurrence.WEEKLY,
            byDay = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            until = anchor.plusDays(13),
        )
        assertEquals(6, rule.occurrenceTotal(anchor)) // 3 days × 2 weeks
    }

    @Test
    fun `retiring early shrinks the total to what actually ran`() {
        val rule = RecurrenceRule(Recurrence.DAILY, until = anchor.plusDays(29))
        assertEquals(10, rule.occurrenceTotal(anchor, finishedOn = anchor.plusDays(9)))
        // A finishedOn after the scheduled end never extends it.
        assertEquals(30, rule.occurrenceTotal(anchor, finishedOn = anchor.plusDays(50)))
    }

    @Test
    fun `an end before the anchor totals zero`() {
        val rule = RecurrenceRule(Recurrence.DAILY, until = anchor.minusDays(1))
        assertEquals(0, rule.occurrenceTotal(anchor))
    }
}
