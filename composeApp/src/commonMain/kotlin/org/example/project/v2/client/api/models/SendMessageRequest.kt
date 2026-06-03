package org.example.project.v2.client.api.models

data class SendMessageRequest(
    val text: String,
    val clientMessageId: String? = null,
)
