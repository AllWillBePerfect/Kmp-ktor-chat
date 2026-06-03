package org.example.project.v2.client.plugin.listeners

internal fun interface ChannelMarkReadListener {
    fun onChannelMarkReadPrecondition(
        channelType: String,
        channelId: String,
    ): Boolean
}
