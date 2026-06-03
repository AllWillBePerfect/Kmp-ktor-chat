package org.example.project.v2.client.channel

import org.example.project.v2.client.ChatClient
import org.example.project.v2.client.api.models.SendMessageRequest
import org.example.project.v2.client.api.models.WatchChannelRequest
import org.example.project.v2.core.models.Channel
import org.example.project.v2.core.models.Message

/**
 * Minimal v2 channel client aligned with the SDK entry point.
 */
class ChannelClient internal constructor(
    val channelType: String,
    val channelId: String,
    private val client: ChatClient,
) {
    val cid: String = "$channelType:$channelId"

    suspend fun watch(request: WatchChannelRequest): Channel? {
        return client.queryChannel(channelType = channelType, channelId = channelId, request = request)
    }

    suspend fun watch(data: Map<String, Any>): Channel? {
        val request = WatchChannelRequest()
        request.data.putAll(data)
        return watch(request)
    }

    suspend fun watch(): Channel? {
        return client.queryChannel(
            channelType = channelType,
            channelId = channelId,
            request = WatchChannelRequest(),
        )
    }

    suspend fun sendMessage(message: Message): Message {
        return client.sendMessage(
            channelType = channelType,
            channelId = channelId,
            request = SendMessageRequest(
                text = message.text,
                clientMessageId = message.clientMessageId,
            ),
        )
    }

    suspend fun markRead() {
        client.markRead(channelType = channelType, channelId = channelId)
    }

    suspend fun keystroke(parentId: String? = null) {
        client.keystroke(
            channelType = channelType,
            channelId = channelId,
            parentId = parentId,
        )
    }

    suspend fun stopTyping(parentId: String? = null) {
        client.stopTyping(
            channelType = channelType,
            channelId = channelId,
            parentId = parentId,
        )
    }
}
