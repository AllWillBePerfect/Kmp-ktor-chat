package org.example.project.v2.client.internal.state.plugin.state.querychannels.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.example.project.v2.client.api.models.QueryChannelsRequest
import org.example.project.v2.client.api.state.ChannelsStateData
import org.example.project.v2.client.api.state.QueryChannelsState
import org.example.project.v2.client.internal.state.plugin.QueryChannelsIdentifier
import org.example.project.v2.core.models.Channel
import org.example.project.v2.core.models.FilterObject

internal class QueryChannelsMutableState(
    val identifier: QueryChannelsIdentifier,
    override val filter: FilterObject,
    scope: CoroutineScope,
    latestChannels: StateFlow<Map<String, Channel>?>,
) : QueryChannelsState {

    internal var rawChannels: Map<String, Channel>?
        get() = _channels.value
        private set(value) {
            _channels.value = value
        }

    private val _channels = MutableStateFlow<Map<String, Channel>?>(null)
    private val _loading = MutableStateFlow(false)
    private val _loadingMore = MutableStateFlow(false)
    private val _endOfChannels = MutableStateFlow(false)
    private val _currentRequest = MutableStateFlow<QueryChannelsRequest?>(null)

    override val currentRequest: StateFlow<QueryChannelsRequest?> = _currentRequest
    override val loading: StateFlow<Boolean> = _loading
    override val loadingMore: StateFlow<Boolean> = _loadingMore
    override val endOfChannels: StateFlow<Boolean> = _endOfChannels
    override val channels: StateFlow<List<Channel>?> =
        _channels
            .combine(latestChannels) { channelsMap, latest ->
                channelsMap
                    ?.values
                    ?.map { channel -> latest?.get(channel.cid) ?: channel }
                    ?.sortedByDescending { it.lastMessageAt.orEmpty() }
            }
            .stateIn(scope, SharingStarted.Eagerly, null)
    override val channelsStateData: StateFlow<ChannelsStateData> =
        _loading
            .combine(channels) { isLoading, channelList ->
                when {
                    isLoading || channelList == null -> ChannelsStateData.Loading
                    channelList.isEmpty() -> ChannelsStateData.OfflineNoResults
                    else -> ChannelsStateData.Result(channelList)
                }
            }
            .stateIn(scope, SharingStarted.Eagerly, ChannelsStateData.NoQueryActive)

    fun setChannels(channels: Map<String, Channel>) {
        rawChannels = channels
    }

    fun setLoading(isLoading: Boolean) {
        _loading.value = isLoading
    }

    fun setLoadingMore(isLoading: Boolean) {
        _loadingMore.value = isLoading
    }

    fun setEndOfChannels(endOfChannels: Boolean) {
        _endOfChannels.value = endOfChannels
    }

    fun setCurrentRequest(request: QueryChannelsRequest) {
        _currentRequest.value = request
    }
}

internal fun QueryChannelsState.toMutableState(): QueryChannelsMutableState = this as QueryChannelsMutableState
