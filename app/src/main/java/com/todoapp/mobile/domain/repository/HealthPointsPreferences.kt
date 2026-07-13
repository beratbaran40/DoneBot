package com.todoapp.mobile.domain.repository

import kotlinx.coroutines.flow.Flow

/** Activity "health points" bar: 10 hearts, tracked in half-heart units (0..[MAX_HALF_HEARTS]). */
const val HEART_COUNT: Int = 12
const val MAX_HALF_HEARTS: Int = HEART_COUNT * 2

/**
 * Persisted health-points checkpoint. Only fully-ended days (up to yesterday) are folded into
 * [settledHalfHearts]; today is applied live on top by ComputeHealthPointsUseCase.
 *
 * [lastSettledEpochDay] == null marks a first-ever run — the use case then starts FULL and does not
 * fold any real history.
 */
data class HealthCheckpoint(
    val settledHalfHearts: Int,
    val lastSettledEpochDay: Long?,
    val dialogShown: Boolean,
)

/**
 * Device-local persistence for the Activity health-points streak replacement. Reactive only for the
 * depletion-dialog flag (dismiss must reflect immediately); the checkpoint itself is read/written
 * one-shot from the use case, so no full `observe()` of it is needed.
 */
interface HealthPointsPreferences {
    suspend fun getCheckpoint(): HealthCheckpoint

    suspend fun setCheckpoint(
        settledHalfHearts: Int,
        lastSettledEpochDay: Long,
        dialogShown: Boolean,
    )

    fun observeDialogShown(): Flow<Boolean>

    suspend fun setDialogShown(shown: Boolean)

    suspend fun clear()
}
