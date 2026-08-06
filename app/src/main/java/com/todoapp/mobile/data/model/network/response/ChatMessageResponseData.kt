package com.todoapp.mobile.data.model.network.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessageResponseData(
    @SerialName("text") val text: String,
    @SerialName("meta") val meta: ChatTurnMeta,
)

@Serializable
data class ChatTurnMeta(
    @SerialName("roundTrips") val roundTrips: Int,
    @SerialName("refused") val refused: Boolean,
    @SerialName("serverMs") val serverMs: Long,
    /**
     * Tools the turn actually executed, in call order. Read-only here — kept for logging and for
     * reading a turn back in a bug report; [mutated] is what drives behaviour.
     */
    @SerialName("toolsCalled") val toolsCalled: List<String> = emptyList(),
    /**
     * Whether the turn wrote anything, decided by the server from its own list of mutating tools.
     *
     * This is the whole point: the client used to carry its own copy of which tool names mutate, and
     * that copy went stale the moment the backend added a write tool — for however many weeks it took
     * a Play release to catch up, the bot would write, the server would change, and the device would
     * quietly show old data.
     *
     * Null (not false) when the field is absent, because "nothing changed" and "this backend is older
     * than the field" must not look alike: defaulting to false would make a new client skip a re-sync
     * it genuinely needs.
     */
    @SerialName("mutated") val mutated: Boolean? = null,
)
