package com.todoapp.mobile.domain.usecase

import com.todoapp.mobile.domain.repository.MAX_HALF_HEARTS

/**
 * Pure fold of the health-points streak. Starting from [startHalfHearts], each ended day in [days]
 * gains +1 half-heart when active (that day had ≥1 completion) or loses 1 when idle, clamped to
 * `[0, MAX_HALF_HEARTS]`. Kept side-effect-free so the mechanic can be unit-tested in isolation.
 */
object HealthPointsCalculator {
    fun fold(
        startHalfHearts: Int,
        days: Iterable<Long>,
        activeDays: Set<Long>,
    ): Int {
        var hearts = startHalfHearts.coerceIn(0, MAX_HALF_HEARTS)
        for (day in days) {
            val delta = if (day in activeDays) 1 else -1
            hearts = (hearts + delta).coerceIn(0, MAX_HALF_HEARTS)
        }
        return hearts
    }
}
