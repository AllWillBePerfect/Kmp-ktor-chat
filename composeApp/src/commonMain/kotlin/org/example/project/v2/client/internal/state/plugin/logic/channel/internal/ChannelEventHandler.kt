package org.example.project.v2.client.internal.state.plugin.logic.channel.internal

import org.example.project.v2.client.events.ChatEvent
import org.example.project.v2.core.models.Channel

internal interface ChannelEventHandler {
    fun handle(event: ChatEvent)

    fun syncChannel(channel: Channel)
}
