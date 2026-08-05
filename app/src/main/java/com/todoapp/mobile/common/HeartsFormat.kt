package com.todoapp.mobile.common

/**
 * Full-heart label from half-heart units: `"12"` when whole, `"6½"` for an odd (half) count.
 *
 * Shared so the Activity bar and the Profile badge cannot drift apart — they read the same
 * `HealthPoints.halfHearts` and must render the same number for it.
 */
internal fun heartsLabel(halfHearts: Int): String {
    val fullHearts = halfHearts / 2
    return if (halfHearts % 2 == 1) "$fullHearts½" else fullHearts.toString()
}
