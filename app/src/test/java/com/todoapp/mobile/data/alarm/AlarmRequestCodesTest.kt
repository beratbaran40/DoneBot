package com.todoapp.mobile.data.alarm

import com.todoapp.mobile.domain.alarm.MAX_REMINDER_SLOTS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * One property, and it is the whole point: **no two (task, slot, scope) triples share a request
 * code**. A collision is silent — `FLAG_UPDATE_CURRENT` replaces the other alarm — so the symptom is
 * "one of my reminders just stopped" with nothing in the logs.
 */
class AlarmRequestCodesTest {
    @Test
    fun `a group task and a personal task with the same id never collide`() {
        // The case that made a separate namespace necessary: group ids come from the server and
        // personal ids from local Room, so both counters start at 1 and overlap forever.
        for (id in 1L..1_000L) {
            for (slot in 0 until MAX_REMINDER_SLOTS) {
                val personal = recurringAlarmRequestCode(id, slot, isGroupTask = false)
                val group = recurringAlarmRequestCode(id, slot, isGroupTask = true)
                assertTrue("id=$id slot=$slot collided at $personal", personal != group)
            }
        }
    }

    @Test
    fun `every code in a realistic id and slot range is unique`() {
        val seen = mutableSetOf<Int>()
        var count = 0
        for (isGroup in listOf(false, true)) {
            for (id in 1L..2_000L) {
                for (slot in 0 until MAX_REMINDER_SLOTS) {
                    val code = recurringAlarmRequestCode(id, slot, isGroup)
                    assertTrue("duplicate code $code at id=$id slot=$slot group=$isGroup", seen.add(code))
                    count++
                }
            }
        }
        assertEquals(2 * 2_000 * MAX_REMINDER_SLOTS, count)
    }

    @Test
    fun `personal slot 0 keeps its pre-multi-reminder code`() {
        // Load-bearing for upgrades: an alarm armed by an older build must be REPLACED by the new
        // one rather than left running beside it, which only happens if the code is identical.
        assertEquals(
            (RECURRING_TASK_REQUEST_BASE + 42L).toInt(),
            recurringAlarmRequestCode(42L, slot = 0, isGroupTask = false),
        )
    }

    @Test
    fun `group codes stay inside the signed int range for any plausible server id`() {
        // 0x4000_0000 + id*8 must not wrap into negative territory, which would land the alarm in
        // some unrelated code's slot. ~134M server task ids of headroom.
        val maxId = 100_000_000L
        val code = recurringAlarmRequestCode(maxId, slot = MAX_REMINDER_SLOTS - 1, isGroupTask = true)
        assertTrue("wrapped to $code", code > 0)
    }

    @Test
    fun `one-shot personal codes do not overlap the recurring or group spaces`() {
        for (id in 1L..2_000L) {
            val oneShot = (TASK_REQUEST_BASE + id).toInt()
            for (slot in 0 until MAX_REMINDER_SLOTS) {
                assertTrue(oneShot != recurringAlarmRequestCode(id, slot, isGroupTask = false))
                assertTrue(oneShot != recurringAlarmRequestCode(id, slot, isGroupTask = true))
            }
        }
    }
}
