package org.example.project.v2.client.api2.model.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class QueryChannelsRequest(
    val state: Boolean = true,
    val watch: Boolean = true,
    val presence: Boolean = false,
    val offset: Int = 0,
    val limit: Int,
    @SerialName("message_limit")
    val messageLimit: Int? = null,
    @SerialName("member_limit")
    val memberLimit: Int? = null,
)
