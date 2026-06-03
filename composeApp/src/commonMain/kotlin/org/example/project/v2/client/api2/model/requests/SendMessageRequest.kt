package org.example.project.v2.client.api2.model.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SendMessageRequest(
    val text: String,
    @SerialName("client_message_id")
    val clientMessageId: String? = null,
)
