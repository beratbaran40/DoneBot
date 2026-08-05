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
     * Tools the turn executed, in call order. Drives the post-chat task re-sync: only a WRITE tool can
     * have changed server state, and re-fetching after a pure read turn ("what's due today?") burned a
     * full task sync per question.
     *
     * Defaulted so an older backend that doesn't send the field still deserializes — the ViewModel
     * falls back to the old `roundTrips > 1` heuristic when the list is empty.
     */
    @SerialName("toolsCalled") val toolsCalled: List<String> = emptyList(),
)
