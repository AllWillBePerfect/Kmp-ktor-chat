package org.example.project.v2.client.api2.model.requests

import kotlinx.serialization.Serializable

@Serializable
internal data class QueryChannelRequest(
    val state: Boolean,
    val watch: Boolean,
    val presence: Boolean,
    val messages: Map<String, @Serializable(with = AnyValueSerializer::class) Any> = emptyMap(),
    val watchers: Map<String, @Serializable(with = AnyValueSerializer::class) Any> = emptyMap(),
    val members: Map<String, @Serializable(with = AnyValueSerializer::class) Any> = emptyMap(),
    val data: Map<String, @Serializable(with = AnyValueSerializer::class) Any> = emptyMap(),
)
