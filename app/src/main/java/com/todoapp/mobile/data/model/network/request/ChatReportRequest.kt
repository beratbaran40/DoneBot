package com.todoapp.mobile.data.model.network.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Body for `POST /chat/report` — flags an offensive or inappropriate DoneBot (AI) reply
 * for human moderation review. Required by Google Play's Generative AI policy, which
 * mandates an in-app way to report AI-generated content.
 */
@Serializable
data class ChatReportRequest(
    @SerialName("messageContent") val messageContent: String,
    @SerialName("reason") val reason: String? = null,
)
