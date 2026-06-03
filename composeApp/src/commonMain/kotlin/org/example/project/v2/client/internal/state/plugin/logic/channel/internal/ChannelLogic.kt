package org.example.project.v2.client.internal.state.plugin.logic.channel.internal

import org.example.project.v2.client.api.models.QueryChannelRequest
import org.example.project.v2.client.api.models.SendMessageRequest
import org.example.project.v2.client.channel.state.ChannelState
import org.example.project.v2.client.events.ChatEvent
import org.example.project.v2.core.models.Channel
import org.example.project.v2.core.models.Message

internal interface ChannelLogic {
    val cid: String

    fun channelState(): ChannelState

    suspend fun watch(request: QueryChannelRequest): Channel

    suspend fun sendMessage(request: SendMessageRequest): Message

    suspend fun recover()

    fun markRead(): Boolean

    fun handleEvent(event: ChatEvent)
}
