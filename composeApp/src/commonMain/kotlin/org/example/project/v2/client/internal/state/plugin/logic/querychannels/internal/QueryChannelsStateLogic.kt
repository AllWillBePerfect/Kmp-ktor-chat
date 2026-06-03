package org.example.project.v2.client.internal.state.plugin.logic.querychannels.internal

import org.example.project.v2.client.api.models.QueryChannelsRequest
import org.example.project.v2.client.api.state.QueryChannelsState
import org.example.project.v2.client.internal.state.plugin.state.querychannels.internal.QueryChannelsMutableState
import org.example.project.v2.core.models.Channel
import org.example.project.v2.platform.taggedLogger

internal class QueryChannelsStateLogic(
    private val mutableState: QueryChannelsMutableState,
) {
    private val logger by taggedLogger("Chat:V2QueryChannelsStateLogic")

    fun getState(): QueryChannelsState = mutableState

    fun setCurrentRequest(request: QueryChannelsRequest) {
        logger.d { "[setCurrentRequest] offset=${request.offset} limit=${request.limit}" }
        mutableState.setCurrentRequest(request)
    }

    fun setLoadingFirstPage(isLoading: Boolean) {
        mutableState.setLoading(isLoading)
    }

    fun setLoadingMore(isLoading: Boolean) {
        mutableState.setLoadingMore(isLoading)
    }

    fun setEndOfChannels(isEnd: Boolean) {
        mutableState.setEndOfChannels(isEnd)
    }

    fun addChannels(channels: List<Channel>) {
        val current = mutableState.rawChannels.orEmpty()
        val updated = current + channels.associateBy(Channel::cid)
        mutableState.setChannels(updated)
    }

    fun upsertChannel(channel: Channel) {
        val current = mutableState.rawChannels.orEmpty()
        mutableState.setChannels(current + (channel.cid to channel))
    }

    fun refreshChannelState(cid: String, channel: Channel) {
        val current = mutableState.rawChannels ?: return
        if (!current.containsKey(cid)) return
        mutableState.setChannels(current + (cid to channel))
    }

    fun refreshChannelsState(channels: Map<String, Channel>) {
        val current = mutableState.rawChannels ?: return
        val refreshed = current + channels.filterKeys(current::containsKey)
        mutableState.setChannels(refreshed)
    }

    fun initializeChannelsIfNeeded() {
        if (mutableState.rawChannels == null) {
            mutableState.setChannels(emptyMap())
        }
    }
}
