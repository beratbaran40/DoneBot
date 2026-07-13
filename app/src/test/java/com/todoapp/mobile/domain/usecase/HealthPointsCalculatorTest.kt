package com.todoapp.mobile.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the health-points fold: +1 half-heart per active day, -1 per idle day, clamped to [0, 20].
 * Pure JVM, no mocks — this is the mechanic's source of truth (see ComputeHealthPointsUseCase).
 */
class HealthPointsCalculatorTest {
    @Test
    fun `single active day gains one half-heart`() {
        assertEquals(11, HealthPointsCalculator.fold(startHalfHearts = 10, days = listOf(DAY), activeDays = setOf(DAY)))
    }

    @Test
    fun `single idle day loses one half-heart`() {
        assertEquals(9, HealthPointsCalculator.fold(startHalfHearts = 10, days = listOf(DAY), activeDays = emptySet()))
    }

    @Test
    fun `hearts never drop below zero`() {
        assertEquals(0, HealthPointsCalculator.fold(startHalfHearts = 1, days = listOf(DAY, DAY + 1), activeDays = emptySet()))
    }

    @Test
    fun `hearts never exceed the maximum`() {
        assertEquals(24, HealthPointsCalculator.fold(startHalfHearts = 23, days = listOf(DAY, DAY + 1), activeDays = setOf(DAY, DAY + 1)))
    }

    @Test
    fun `an out-of-range start is clamped before folding`() {
        assertEquals(24, HealthPointsCalculator.fold(startHalfHearts = 30, days = emptyList(), activeDays = emptySet()))
    }

    @Test
    fun `mixed active and idle days net out`() {
        val days = listOf(DAY, DAY + 1, DAY + 2, DAY + 3)
        val active = setOf(DAY, DAY + 2)
        assertEquals(10, HealthPointsCalculator.fold(startHalfHearts = 10, days = days, activeDays = active))
    }

    @Test
    fun `an empty day range is a no-op`() {
        assertEquals(14, HealthPointsCalculator.fold(startHalfHearts = 14, days = emptyList(), activeDays = emptySet()))
    }

    private companion object {
        const val DAY = 20_000L
    }
}
