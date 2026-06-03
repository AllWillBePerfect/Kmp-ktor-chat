package org.example.project.v2.client.api2

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import org.example.project.v2.client.api.ChatApi
import org.example.project.v2.client.api.models.QueryChannelRequest
import org.example.project.v2.client.api.models.QueryChannelsRequest
import org.example.project.v2.client.api.models.SendMessageRequest
import org.example.project.v2.client.api2.model.requests.MarkReadRequest
import org.example.project.v2.client.api2.model.responses.QueryChannelResponse
import org.example.project.v2.client.api2.model.responses.QueryChannelsResponse
import org.example.project.v2.client.api2.model.responses.SendMessageResponse
import org.example.project.v2.client.parser.adapters.toChannel
import org.example.project.v2.client.parser.adapters.toMessage
import org.example.project.v2.core.models.Channel
import org.example.project.v2.core.models.Message
import org.example.project.v2.platform.taggedLogger

internal class KtorChatApi(
    private val baseUrl: String,
    private val apiKey: String,
    private val tokenProvider: () -> String?,
    private val client: HttpClient,
) : ChatApi {
    private val logger by taggedLogger("Chat:KtorChatApi")

    override suspend fun queryChannels(request: QueryChannelsRequest): List<Channel> {
        val body = org.example.project.v2.client.api2.model.requests.QueryChannelsRequest(
            state = request.state,
            watch = request.watch,
            presence = request.presence,
            offset = request.offset,
            limit = request.limit,
            messageLimit = request.messageLimit,
            memberLimit = request.memberLimit,
        )
        val response = client.post("$baseUrl/channels/query") {
            header("X-Api-Key", apiKey)
            tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(body)
        }.body<QueryChannelsResponse>()

        val channels = response.channels.map { it.toChannel() }
        logger.d { "[queryChannels] fetched ${channels.size} channels" }
        return channels
    }

    override suspend fun markRead(
        channelType: String,
        channelId: String,
        messageId: String,
    ) {
        client.post("$baseUrl/channels/$channelType/$channelId/read") {
            header("X-Api-Key", apiKey)
            tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(MarkReadRequest(messageId = messageId.ifBlank { null }))
        }
        logger.d { "[markRead] completed for $channelType:$channelId" }
    }

    override suspend fun sendMessage(
        channelType: String,
        channelId: String,
        request: SendMessageRequest,
    ): Message {
        val body = org.example.project.v2.client.api2.model.requests.SendMessageRequest(
            text = request.text,
            clientMessageId = request.clientMessageId,
        )
        val response = client.post("$baseUrl/channels/$channelType/$channelId/messages") {
            header("X-Api-Key", apiKey)
            tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(body)
        }.body<SendMessageResponse>()

        val message = response.message
            ?.toMessage()
            ?: error("SendMessage response does not contain 'message'")
        logger.d { "[sendMessage] sent message to $channelType:$channelId id=${message.id}" }
        return message
    }

    override suspend fun queryChannel(
        channelType: String,
        channelId: String,
        query: QueryChannelRequest,
    ): Channel {
        val request = org.example.project.v2.client.api2.model.requests.QueryChannelRequest(
            state = query.state,
            watch = query.watch,
            presence = query.presence,
            messages = query.messages,
            watchers = query.watchers,
            members = query.members,
            data = query.data,
        )
        val response = client.post("$baseUrl/channels/$channelType/$channelId/query") {
            header("X-Api-Key", apiKey)
            tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(request)
        }.body<QueryChannelResponse>()

        val channel = response.channel
            ?: error("QueryChannel response does not contain 'channel'")
        logger.d { "[queryChannel] fetched $channelType:$channelId" }
        return channel.toChannel()
    }
}
