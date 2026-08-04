package com.todoapp.uikit.components

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * Rows a month grid renders. SIX, not five.
 *
 * Five rows is 35 cells, and the grid starts on the Monday of the week containing the 1st — so a
 * 31-day month whose 1st falls on a Saturday begins five days early and runs out before the 31st,
 * which then cannot be rendered or tapped at all. August 2026 is exactly that shape.
 *
 * Fixed at six rather than computed per month so the dialog's height doesn't jump as the user pages
 * between months; the spare row fills with dimmed neighbouring-month days like any calendar.
 */
const val CALENDAR_WEEK_ROWS = 6

private const val DAYS_IN_WEEK = 7

/**
 * The Monday-first day grid for [month], as [CALENDAR_WEEK_ROWS] weeks of 7 dates — including the
 * leading days of the previous month and the trailing days of the next one.
 *
 * Pure and public so the "every day of the month is reachable" invariant can be tested from `:app`
 * without standing up an instrumented UI test — see CalendarGridTest.
 */
fun calendarGridWeeks(month: YearMonth): List<List<LocalDate>> {
    val firstOfMonth = month.atDay(1)
    val gridStart = firstOfMonth.minusDays((firstOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
    return List(CALENDAR_WEEK_ROWS) { week ->
        List(DAYS_IN_WEEK) { day -> gridStart.plusDays((week * DAYS_IN_WEEK + day).toLong()) }
    }
}
