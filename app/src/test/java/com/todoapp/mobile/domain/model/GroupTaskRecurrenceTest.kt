package com.todoapp.mobile.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * A group task is the same server row as a personal one, so it must answer "does this belong on that
 * day?" identically. Every surface used to compare the due date for equality, which is right for a
 * one-off and silently wrong for a routine — a daily chore appeared on its start day only.
 */
class GroupTaskRecurrenceTest {
    @Test
    fun `a one-off group task belongs to its own day and no other`() {
        val task = groupTask(start = LocalDate.of(2026, 8, 10))

        assertTrue(task.firesOnDate(LocalDate.of(2026, 8, 10)))
        assertFalse(task.firesOnDate(LocalDate.of(2026, 8, 11)))
        assertFalse(task.firesOnDate(LocalDate.of(2026, 8, 9)))
    }

    @Test
    fun `a daily group task belongs to every day from its start`() {
        val task = groupTask(start = LocalDate.of(2026, 8, 10), recurrence = Recurrence.DAILY)

        assertTrue(task.firesOnDate(LocalDate.of(2026, 8, 10)))
        assertTrue(task.firesOnDate(LocalDate.of(2026, 8, 11)))
        assertTrue(task.firesOnDate(LocalDate.of(2026, 12, 31)))
        // Never before the anchor — a routine created today does not retroactively fill the past.
        assertFalse(task.firesOnDate(LocalDate.of(2026, 8, 9)))
    }

    @Test
    fun `an every-other-day group task skips the days in between`() {
        val task = groupTask(
            start = LocalDate.of(2026, 8, 10),
            recurrence = Recurrence.DAILY,
            interval = 2,
        )

        assertTrue(task.firesOnDate(LocalDate.of(2026, 8, 10)))
        assertFalse(task.firesOnDate(LocalDate.of(2026, 8, 11)))
        assertTrue(task.firesOnDate(LocalDate.of(2026, 8, 12)))
    }

    @Test
    fun `a by-weekday group task fires on exactly those weekdays`() {
        // 10 Aug 2026 is a Monday.
        val task = groupTask(
            start = LocalDate.of(2026, 8, 10),
            recurrence = Recurrence.WEEKLY,
            byDay = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
        )

        assertTrue(task.firesOnDate(LocalDate.of(2026, 8, 12)))
        assertFalse(task.firesOnDate(LocalDate.of(2026, 8, 13)))
        assertTrue(task.firesOnDate(LocalDate.of(2026, 8, 14)))
    }

    @Test
    fun `a bounded group routine stops after its scheduled end`() {
        val task = groupTask(
            start = LocalDate.of(2026, 8, 10),
            recurrence = Recurrence.DAILY,
            until = LocalDate.of(2026, 8, 12),
        )

        assertTrue(task.firesOnDate(LocalDate.of(2026, 8, 12)))
        assertFalse(task.firesOnDate(LocalDate.of(2026, 8, 13)))
    }

    @Test
    fun `a group task with no due date belongs to no day at all`() {
        // dueDate is nullable on the wire; without an anchor there is nothing to count from, and
        // guessing "today" would make an undated task appear on whatever day you happened to look.
        val task = GroupTask(
            id = 1L,
            title = "Untethered",
            description = null,
            isCompleted = false,
            priority = null,
            dueDate = null,
            assignee = null,
            recurrence = Recurrence.DAILY,
        )

        assertFalse(task.firesOnDate(LocalDate.now()))
    }

    @Test
    fun `the group rule feeds the same day-N-of-M helpers the personal side uses`() {
        val task = groupTask(
            start = LocalDate.of(2026, 8, 10),
            recurrence = Recurrence.DAILY,
            until = LocalDate.of(2026, 9, 8),
        )

        assertEquals(30, task.recurrenceRule.occurrenceTotal(LocalDate.of(2026, 8, 10)))
        assertEquals(
            3,
            task.recurrenceRule.occurrenceIndex(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 12)),
        )
    }

    private fun groupTask(
        start: LocalDate,
        recurrence: Recurrence = Recurrence.NONE,
        interval: Int = 1,
        byDay: Set<DayOfWeek> = emptySet(),
        until: LocalDate? = null,
    ) = GroupTask(
        id = 1L,
        title = "Take out the bins",
        description = null,
        isCompleted = false,
        priority = null,
        dueDate = start.atTime(LocalTime.NOON).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        assignee = null,
        recurrence = recurrence,
        recurrenceInterval = interval,
        recurrenceByDay = byDay,
        recurrenceUntil = until,
    )
}
