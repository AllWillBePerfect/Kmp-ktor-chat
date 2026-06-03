package org.example.project.v2.client.api.state

import org.example.project.v2.core.models.Channel

sealed interface ChannelsStateData {
    data object NoQueryActive : ChannelsStateData
    data object Loading : ChannelsStateData
    data object OfflineNoResults : ChannelsStateData
    data class Result(val channels: List<Channel>) : ChannelsStateData
}
