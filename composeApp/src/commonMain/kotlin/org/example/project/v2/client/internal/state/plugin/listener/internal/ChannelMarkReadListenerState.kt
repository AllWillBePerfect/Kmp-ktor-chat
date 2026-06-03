package org.example.project.v2.client.internal.state.plugin.listener.internal

import org.example.project.v2.client.internal.state.plugin.logic.internal.LogicRegistry
import org.example.project.v2.client.plugin.listeners.ChannelMarkReadListener

internal class ChannelMarkReadListenerState(
    private val logic: LogicRegistry,
) : ChannelMarkReadListener {

    override fun onChannelMarkReadPrecondition(
        channelType: String,
        channelId: String,
    ): Boolean {
        return logic.channel(channelType, channelId).markRead()
    }
}
