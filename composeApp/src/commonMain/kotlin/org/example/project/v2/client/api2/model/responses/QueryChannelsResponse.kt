package org.example.project.v2.client.api2.model.responses

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class QueryChannelsResponse(
    val channels: List<JsonObject> = emptyList(),
)
