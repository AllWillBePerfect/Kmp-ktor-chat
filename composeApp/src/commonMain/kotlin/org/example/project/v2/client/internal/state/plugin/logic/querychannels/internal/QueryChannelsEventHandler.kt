package org.example.project.v2.client.internal.state.plugin.logic.querychannels.internal

import org.example.project.v2.client.events.ChatEvent
import org.example.project.v2.core.models.Channel

internal interface QueryChannelsEventHandler {
    suspend fun handle(event: ChatEvent)

    fun onChannelUpdated(channel: Channel)
}
