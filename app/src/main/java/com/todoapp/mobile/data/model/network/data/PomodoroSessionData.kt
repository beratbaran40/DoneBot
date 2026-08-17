package com.todoapp.mobile.data.model.network.data

import com.todoapp.mobile.data.model.network.request.PomodoroSessionDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Result of an upload.
 *
 * [duplicates] is success, not a warning: a repeated upload of a row the server already has is a no-op
 * by design. The alternative — answering 409 — would wedge the push loop permanently, because the client
 * never stops retrying a batch it believes unsent.
 */
@Serializable
data class PomodoroUploadData(
    @SerialName("accepted") val accepted: Int = 0,
    @SerialName("duplicates") val duplicates: Int = 0,
)

/** Sign-in backfill payload. Same shape as the upload, so one mapper round-trips both directions. */
@Serializable
data class PomodoroSessionListData(
    @SerialName("items") val items: List<PomodoroSessionDto> = emptyList(),
    @SerialName("count") val count: Int = 0,
)
