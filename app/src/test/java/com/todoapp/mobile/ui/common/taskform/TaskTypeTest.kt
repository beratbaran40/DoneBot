package com.todoapp.mobile.ui.common.taskform

import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.Subtask
import com.todoapp.mobile.domain.model.Task
import com.todoapp.mobile.domain.model.TaskType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * The task-shape rules: what the data implies, and when a stored declaration overrides it.
 *
 * This package had no tests at all, which is how a task the user explicitly created as "Custom"
 * could report itself as a Routine for as long as it did — the derivation was only ever exercised
 * by hand.
 */
class TaskTypeTest {
    // region derivation

    @Test
    fun `a plain task derives as one-time`() {
        assertEquals(TaskType.ONE_TIME, derivedTaskType(task().capabilities()))
    }

    @Test
    fun `a task that only repeats derives as a routine`() {
        assertEquals(TaskType.ROUTINE, derivedTaskType(task(recurrence = Recurrence.DAILY).capabilities()))
    }

    @Test
    fun `a task that only has steps derives as staged`() {
        assertEquals(TaskType.STAGED, derivedTaskType(task(subtasks = listOf(step("Giriş"))).capabilities()))
    }

    @Test
    fun `a recurring staged task derives as custom rather than hiding its schedule`() {
        val caps = task(recurrence = Recurrence.WEEKLY, subtasks = listOf(step("Esneme"))).capabilities()
        assertEquals(TaskType.CUSTOM, derivedTaskType(caps))
    }

    @Test
    fun `more than one reminder time derives as custom`() {
        val caps = task(
            recurrence = Recurrence.DAILY,
            reminderTimes = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)),
        ).capabilities()
        assertEquals(TaskType.CUSTOM, derivedTaskType(caps))
    }

    @Test
    fun `a single reminder time is not enough to be custom`() {
        val caps = task(recurrence = Recurrence.DAILY, reminderTimes = listOf(LocalTime.of(8, 0))).capabilities()
        assertEquals(TaskType.ROUTINE, derivedTaskType(caps))
    }

    @Test
    fun `a scheduled end does not make a repeat a different shape`() {
        // Pins the documented decision in TaskCapabilities.isCustom: UNTIL sits beside FREQ in one
        // rule, so "every day for a month" is a routine with a finish line, not another shape. It is
        // also exactly why the derivation alone could never honour the user's Custom choice.
        val caps = task(
            recurrence = Recurrence.DAILY,
            recurrenceUntil = LocalDate.of(2026, 9, 16),
        ).capabilities()
        assertEquals(TaskType.ROUTINE, derivedTaskType(caps))
    }

    @Test
    fun `list surfaces count steps from the cheap total`() {
        // The full subtasks list isn't loaded on list surfaces; subtaskTotal stands in for it.
        assertEquals(TaskType.STAGED, derivedTaskType(task(subtaskTotal = 3).capabilities()))
    }

    // endregion

    // region declaration

    @Test
    fun `a declared type wins over the derivation`() {
        // The bug, as one assertion: Custom + a bounded daily repeat used to render as "Routine".
        val declared = task(
            recurrence = Recurrence.DAILY,
            recurrenceUntil = LocalDate.of(2026, 9, 16),
            declaredType = TaskType.CUSTOM,
        )
        assertEquals(TaskType.CUSTOM, declared.resolvedType())
    }

    @Test
    fun `a declared type holds even when the data says something else entirely`() {
        val declared =
            task(recurrence = Recurrence.WEEKLY, subtasks = listOf(step("x")), declaredType = TaskType.ROUTINE)
        assertEquals(TaskType.ROUTINE, declared.resolvedType())
    }

    @Test
    fun `an undeclared task falls back to the derivation`() {
        // Every row older than the column, and every row that arrived from the server.
        assertEquals(TaskType.ROUTINE, task(recurrence = Recurrence.DAILY).resolvedType())
        assertEquals(TaskType.ONE_TIME, task().resolvedType())
    }

    // endregion

    // region storage

    @Test
    fun `fromStorage round-trips every value`() {
        TaskType.entries.forEach { assertEquals(it, TaskType.fromStorage(it.name)) }
    }

    @Test
    fun `fromStorage returns null rather than a default`() {
        // Deliberately unlike Recurrence and TaskCategory: "no declaration" is a real state, and
        // defaulting to any of the four would relabel every pre-column task on its first read.
        assertNull(TaskType.fromStorage(null))
        assertNull(TaskType.fromStorage(""))
        assertNull(TaskType.fromStorage("SOMETHING_A_NEWER_BUILD_WROTE"))
    }

    // endregion

    private fun task(
        recurrence: Recurrence = Recurrence.NONE,
        recurrenceUntil: LocalDate? = null,
        subtasks: List<Subtask> = emptyList(),
        subtaskTotal: Int = 0,
        reminderTimes: List<LocalTime> = emptyList(),
        declaredType: TaskType? = null,
    ) = Task(
        title = "Task",
        description = null,
        date = LocalDate.of(2026, 8, 17),
        timeStart = LocalTime.of(9, 0),
        timeEnd = LocalTime.of(10, 0),
        isCompleted = false,
        isSecret = false,
        recurrence = recurrence,
        recurrenceUntil = recurrenceUntil,
        subtasks = subtasks,
        subtaskTotal = subtaskTotal,
        reminderTimes = reminderTimes,
        declaredType = declaredType,
    )

    private fun step(title: String) = Subtask(title = title, orderIndex = 0)
}
