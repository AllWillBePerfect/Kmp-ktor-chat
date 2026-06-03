package org.example.project.v2.client.api2.model.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MarkReadRequest(
    @SerialName("message_id")
    val messageId: String? = null,
)
