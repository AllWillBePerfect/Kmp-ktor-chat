package org.example.project.v2.client.internal.state.plugin.logic.internal

import java.util.concurrent.ConcurrentHashMap
import org.example.project.v2.client.api.ChatApi
import org.example.project.v2.client.api.event.ChatEventHandler
import org.example.project.v2.client.events.ChatEvent
import org.example.project.v2.client.events.CidEvent
import org.example.project.v2.client.internal.state.plugin.logic.channel.internal.ChannelLogic
import org.example.project.v2.client.internal.state.plugin.logic.channel.internal.ChannelLogicImpl
import org.example.project.v2.client.internal.state.plugin.logic.querychannels.internal.QueryChannelsLogic
import org.example.project.v2.client.internal.state.plugin.logic.querychannels.internal.QueryChannelsStateLogic
import org.example.project.v2.client.internal.state.plugin.state.querychannels.internal.toMutableState
import org.example.project.v2.client.ChatClient
import org.example.project.v2.client.internal.state.plugin.state.channel.internal.ChannelStateImpl
import org.example.project.v2.client.api.state.StateRegistry
import org.example.project.v2.core.models.FilterObject

internal class LogicRegistry(
    private val chatClient: ChatClient,
    private val stateRegistry: StateRegistry,
    private val chatApi: ChatApi,
    private val chatEventHandler: ChatEventHandler,
    private val currentUserIdProvider: () -> String?,
) {
    private val channelLogics = ConcurrentHashMap<Pair<String, String>, ChannelLogic>()
    private val queryChannelsLogics = ConcurrentHashMap<FilterObject, QueryChannelsLogic>()

    fun channel(channelType: String, channelId: String): ChannelLogic {
        return channelLogics.getOrPut(channelType to channelId) {
            ChannelLogicImpl(
                channelType = channelType,
                channelId = channelId,
                chatApi = chatApi,
                state = stateRegistry.channel(channelType, channelId) as ChannelStateImpl,
                stateRegistry = stateRegistry,
                currentUserIdProvider = currentUserIdProvider,
                onChannelStateUpdated = ::onChannelStateUpdated,
            )
        }
    }

    fun handleEvent(event: ChatEvent) {
        val cidEvent = event as? CidEvent ?: return
        channel(cidEvent.channelType, cidEvent.channelId).handleEvent(event)
    }

    suspend fun recover() {
        queryChannelsLogics.values.forEach { logic ->
            logic.recover()
        }
        channelLogics.values.forEach { logic ->
            logic.recover()
        }
    }

    fun queryChannels(filter: FilterObject): QueryChannelsLogic {
        return queryChannelsLogics.getOrPut(filter) {
            val state = stateRegistry.queryChannels(filter).toMutableState()
            QueryChannelsLogic(
                chatClient = chatClient,
                stateRegistry = stateRegistry,
                queryChannelsStateLogic = QueryChannelsStateLogic(state),
                eventHandler = chatEventHandler,
                filter = filter,
            )
        }
    }

    private fun onChannelStateUpdated(cid: String) {
        queryChannelsLogics.values.forEach { logic ->
            logic.refreshChannelState(cid)
        }
    }
}
