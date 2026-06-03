package org.example.project.v2.client.api.state

import kotlinx.coroutines.flow.StateFlow
import org.example.project.v2.client.api.models.QueryChannelsRequest
import org.example.project.v2.core.models.Channel
import org.example.project.v2.core.models.FilterObject

interface QueryChannelsState {
    val filter: FilterObject
    val currentRequest: StateFlow<QueryChannelsRequest?>
    val loading: StateFlow<Boolean>
    val loadingMore: StateFlow<Boolean>
    val endOfChannels: StateFlow<Boolean>
    val channels: StateFlow<List<Channel>?>
    val channelsStateData: StateFlow<ChannelsStateData>
}
