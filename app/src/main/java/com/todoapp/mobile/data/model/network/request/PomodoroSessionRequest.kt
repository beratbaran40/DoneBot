package com.todoapp.mobile.data.model.network.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Body for `POST /pomodoro/sessions`. Up to fifty rows per call.
 *
 * Batched because these rows — unlike every other synced entity here — are immutable, append-only,
 * unordered and carry their own idempotency key. A partial failure is retried whole and the server's
 * unique index absorbs the overlap, so the batch cannot double-count. A week offline is about fifty
 * rows, and the backend's database scales to zero, so one-at-a-time would be fifty cold starts.
 */
@Serializable
data class PomodoroSessionUploadRequest(
    @SerialName("sessions") val sessions: List<PomodoroSessionDto>,
)

/**
 * One recorded interval on the wire.
 *
 * Field names and units are the platform-independent contract — the iOS client must send exactly these,
 * or the two clients write contradictory rows into one table. Seconds are seconds, timestamps are epoch
 * **milliseconds** UTC, and `localDate` is an epoch **day** in the device's own zone.
 */
@Serializable
data class PomodoroSessionDto(
    @SerialName("clientSessionId") val clientSessionId: String,
    @SerialName("clientRunId") val clientRunId: String,
    @SerialName("sessionIndex") val sessionIndex: Int,
    @SerialName("mode") val mode: String,
    @SerialName("plannedSeconds") val plannedSeconds: Int,
    /** What actually ran. Every focus-time figure sums this; nothing sums [plannedSeconds]. */
    @SerialName("elapsedSeconds") val elapsedSeconds: Int,
    @SerialName("completed") val completed: Boolean,
    @SerialName("startedAt") val startedAt: Long,
    @SerialName("endedAt") val endedAt: Long,
    @SerialName("localDate") val localDate: Long,
    @SerialName("tzOffsetMinutes") val tzOffsetMinutes: Int? = null,
)
