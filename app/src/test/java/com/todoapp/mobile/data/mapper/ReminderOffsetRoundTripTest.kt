package com.todoapp.mobile.data.mapper

import com.todoapp.mobile.data.model.network.data.TaskData
import com.todoapp.mobile.domain.model.REMINDER_OFF
import com.todoapp.mobile.domain.model.Task
import com.todoapp.mobile.domain.model.toCreateTaskRequestDto
import com.todoapp.mobile.domain.model.toDomain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * The reminder offset means three things — off, at the start time, N minutes before — and travels
 * through two places that can only hold two of them: a `NOT NULL` Room column and a `NOT NULL` wire
 * field. Both carry [REMINDER_OFF] where the domain carries null.
 *
 * Every boundary crossing therefore has to convert, and the bug this pins is what happens when one of
 * them does not: `?: 0L` turned "off" into "remind me at the start time", so a task whose reminder the
 * user had switched off rang anyway — and, because the same collapse happened on the way to the
 * server, the next reconcile handed that back to every device the user owned.
 */
class ReminderOffsetRoundTripTest {

    private fun task(offset: Long?) = Task(
        id = 7L,
        title = "Vitamin",
        description = null,
        date = LocalDate.of(2026, 8, 6),
        timeStart = LocalTime.of(9, 0),
        timeEnd = LocalTime.of(9, 30),
        isCompleted = false,
        isSecret = false,
        reminderOffsetMinutes = offset,
    )

    private fun remoteTask(offset: Long) = TaskData(
        id = 7L,
        title = "Vitamin",
        description = null,
        date = LocalDate.of(2026, 8, 6).toEpochDay(),
        timeStart = LocalTime.of(9, 0).toSecondOfDay().toLong(),
        timeEnd = LocalTime.of(9, 30).toSecondOfDay().toLong(),
        isCompleted = false,
        isSecret = false,
        reminderOffsetMinutes = offset,
    )

    // ── Room ──────────────────────────────────────────────────────────────────

    @Test
    fun `a switched-off reminder survives a trip through Room`() {
        val stored = task(null).toEntity()

        assertEquals("off must not be stored as 0 — that is a reminder", REMINDER_OFF, stored.reminderOffsetMinutes)
        assertNull("and it has to come back as off, not as on-time", stored.toDomain().reminderOffsetMinutes)
    }

    @Test
    fun `on-time is kept apart from off`() {
        // The form offers these as two chips. 0 rings; null does not.
        val stored = task(0L).toEntity()

        assertEquals(0L, stored.reminderOffsetMinutes)
        assertEquals(0L, stored.toDomain().reminderOffsetMinutes)
    }

    @Test
    fun `an ordinary offset is untouched`() {
        val stored = task(15L).toEntity()

        assertEquals(15L, stored.reminderOffsetMinutes)
        assertEquals(15L, stored.toDomain().reminderOffsetMinutes)
    }

    @Test
    fun `any negative in the column reads as off`() {
        // Generous on the way in: a row written by an older build, or by hand, still must not ring.
        val stored = task(0L).toEntity().copy(reminderOffsetMinutes = -5L)

        assertNull(stored.toDomain().reminderOffsetMinutes)
    }

    // ── The wire ──────────────────────────────────────────────────────────────

    @Test
    fun `a switched-off reminder is sent as off, not as on-time`() {
        // The half that made the bug survive a reinstall: the server was told 0, stored 0, and handed
        // it back as a live reminder on the next reconcile.
        assertEquals(REMINDER_OFF, task(null).toCreateTaskRequestDto().reminderOffsetMinutes)
    }

    @Test
    fun `on-time is sent as on-time`() {
        assertEquals(0L, task(0L).toCreateTaskRequestDto().reminderOffsetMinutes)
    }

    @Test
    fun `off from the server arrives as off`() {
        assertNull(remoteTask(REMINDER_OFF).toDomain().reminderOffsetMinutes)
    }

    @Test
    fun `on-time from the server arrives as on-time`() {
        assertEquals(0L, remoteTask(0L).toDomain().reminderOffsetMinutes)
    }

    // ── Both, end to end ──────────────────────────────────────────────────────

    @Test
    fun `off makes the full loop without becoming a reminder`() {
        // domain -> wire -> (server echoes it back) -> domain -> Room -> domain
        val sent = task(null).toCreateTaskRequestDto().reminderOffsetMinutes
        val backFromServer = remoteTask(sent).toDomain()
        val afterRoom = backFromServer.toEntity().toDomain()

        assertNull("a reminder the user switched off must never come back on", afterRoom.reminderOffsetMinutes)
    }
}
