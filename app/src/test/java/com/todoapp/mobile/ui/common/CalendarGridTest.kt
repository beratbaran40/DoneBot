package com.todoapp.mobile.ui.common

import com.todoapp.uikit.components.CALENDAR_WEEK_ROWS
import com.todoapp.uikit.components.calendarGridWeeks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * The month grid is pure, so its one non-negotiable invariant is cheap to pin: **every day of the
 * month must appear in the grid**, or that day simply cannot be tapped in the app.
 *
 * Lives in `:app` (which depends on `:uikit`) because `:uikit` has no test source set and this
 * doesn't justify bootstrapping one.
 */
class CalendarGridTest {
    @Test
    fun `grid contains every day of the month for all seven possible start weekdays`() {
        // 2026 conveniently gives us months starting on each weekday.
        var month = YearMonth.of(2026, 1)
        val coveredStartDays = mutableSetOf<DayOfWeek>()
        repeat(24) {
            val days = calendarGridWeeks(month).flatten().toSet()
            coveredStartDays += month.atDay(1).dayOfWeek
            for (day in 1..month.lengthOfMonth()) {
                val date = month.atDay(day)
                assertTrue("$date is missing from its own month grid", date in days)
            }
            month = month.plusMonths(1)
        }
        assertEquals("expected months starting on every weekday", DayOfWeek.entries.toSet(), coveredStartDays)
    }

    @Test
    fun `august 2026 renders the 31st`() {
        // The exact regression: August 2026 starts on a Saturday, so a five-row grid began five days
        // early and ran out at the 30th — the 31st was unreachable in the UI.
        val days = calendarGridWeeks(YearMonth.of(2026, 8)).flatten()
        assertTrue("31 Aug 2026 must be selectable", LocalDate.of(2026, 8, 31) in days)
    }

    @Test
    fun `grid is always six full weeks starting on a Monday`() {
        val weeks = calendarGridWeeks(YearMonth.of(2026, 2))
        assertEquals(CALENDAR_WEEK_ROWS, weeks.size)
        assertTrue("every row is a full week", weeks.all { it.size == 7 })
        assertEquals(DayOfWeek.MONDAY, weeks.first().first().dayOfWeek)
        // Contiguous: the grid must not skip or repeat a day across row boundaries.
        val flat = weeks.flatten()
        assertTrue(flat.zipWithNext().all { (a, b) -> b == a.plusDays(1) })
    }

    @Test
    fun `grid height does not change between months`() {
        // A per-month row count would make the dialog jump as the user pages; it is fixed for that.
        val sizes = (1..12).map { calendarGridWeeks(YearMonth.of(2026, it)).flatten().size }.toSet()
        assertEquals(setOf(CALENDAR_WEEK_ROWS * 7), sizes)
    }
}
