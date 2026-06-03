package org.example.project.v2.client.api

import org.example.project.v2.client.api.models.QueryChannelRequest
import org.example.project.v2.client.api.models.QueryChannelsRequest
import org.example.project.v2.client.api.models.SendMessageRequest
import org.example.project.v2.core.models.Channel
import org.example.project.v2.core.models.Message

internal interface ChatApi {
    suspend fun queryChannels(request: QueryChannelsRequest): List<Channel>

    suspend fun markRead(
        channelType: String,
        channelId: String,
        messageId: String = "",
    )

    suspend fun sendMessage(
        channelType: String,
        channelId: String,
        request: SendMessageRequest,
    ): Message

    suspend fun queryChannel(
        channelType: String,
        channelId: String,
        query: QueryChannelRequest,
    ): Channel
}
