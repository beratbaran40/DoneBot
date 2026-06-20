package com.todoapp.mobile.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtaskTest {
    @Test
    fun `empty list is never done`() {
        assertFalse(emptyList<Subtask>().allSubtasksDone())
    }

    @Test
    fun `all steps done is done`() {
        val steps = listOf(
            Subtask(title = "a", isCompleted = true),
            Subtask(title = "b", isCompleted = true),
        )
        assertTrue(steps.allSubtasksDone())
    }

    @Test
    fun `any incomplete step is not done`() {
        val steps = listOf(
            Subtask(title = "a", isCompleted = true),
            Subtask(title = "b", isCompleted = false),
        )
        assertFalse(steps.allSubtasksDone())
    }
}
