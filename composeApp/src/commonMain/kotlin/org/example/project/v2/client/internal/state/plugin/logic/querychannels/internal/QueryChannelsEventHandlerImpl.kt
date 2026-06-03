package org.example.project.v2.client.internal.state.plugin.logic.querychannels.internal

import org.example.project.v2.client.api.state.StateRegistry
import org.example.project.v2.client.events.ChannelUpdatedByUserEvent
import org.example.project.v2.client.events.ChannelUpdatedEvent
import org.example.project.v2.client.events.ChatEvent
import org.example.project.v2.client.events.MessageReadEvent
import org.example.project.v2.client.events.NewMessageEvent
import org.example.project.v2.client.events.NotificationMessageNewEvent
import org.example.project.v2.core.models.Channel

internal class QueryChannelsEventHandlerImpl(
    private val stateRegistry: StateRegistry,
    private val queryChannelsStateLogic: QueryChannelsStateLogic,
) : QueryChannelsEventHandler {

    override suspend fun handle(event: ChatEvent) {
        when (event) {
            is NewMessageEvent -> stateRegistry.getChannel(event.cid)?.let(queryChannelsStateLogic::upsertChannel)
            is NotificationMessageNewEvent -> queryChannelsStateLogic.upsertChannel(event.channel)
            is MessageReadEvent -> (
                stateRegistry.getActiveChannel(event.cid) ?: stateRegistry.getChannel(event.cid)
                )?.let(queryChannelsStateLogic::upsertChannel)
            is ChannelUpdatedEvent -> syncChannel(event.channel)
            is ChannelUpdatedByUserEvent -> syncChannel(event.channel)
            else -> Unit
        }
    }

    override fun onChannelUpdated(channel: Channel) {
        queryChannelsStateLogic.upsertChannel(channel)
    }

    private suspend fun syncChannel(channel: Channel) {
        if (channel.messages.isNotEmpty()) {
            stateRegistry.setMessages(channel.cid, channel.messages)
        }
        stateRegistry.upsertChannel(channel)
        queryChannelsStateLogic.upsertChannel(channel)
    }
}
