package org.example.project.v2.client.internal.state.plugin.factory

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import org.example.project.v2.client.ChatClient
import org.example.project.v2.client.api.ChatApi
import org.example.project.v2.client.api.event.ChatEventHandler
import org.example.project.v2.client.api.event.DefaultChatEventHandler
import org.example.project.v2.client.api.state.StateRegistry
import org.example.project.v2.client.events.ChatEvent
import org.example.project.v2.client.internal.state.event.handler.internal.EventHandler
import org.example.project.v2.client.internal.state.event.handler.internal.EventHandlerSequential
import org.example.project.v2.client.internal.state.plugin.listener.internal.ChannelMarkReadListenerState
import org.example.project.v2.client.internal.state.plugin.logic.internal.LogicRegistry
import org.example.project.v2.client.internal.state.plugin.logic.querychannels.internal.QueryChannelsLogic
import org.example.project.v2.client.internal.state.plugin.logic.querychannels.internal.QueryChannelsStateLogic
import org.example.project.v2.client.internal.state.plugin.internal.StatePlugin
import org.example.project.v2.client.network.NetworkStateProvider
import org.example.project.v2.client.setup.state.internal.MutableClientState
import org.example.project.v2.core.models.NeutralFilterObject
import org.example.project.v2.platform.taggedLogger

internal class StreamStatePluginFactory(
    private val networkStateProvider: NetworkStateProvider,
) {
    private val logger by taggedLogger("Chat:StatePluginFactory")

    fun create(
        chatClient: ChatClient,
        chatApi: ChatApi,
        scope: CoroutineScope,
        events: Flow<ChatEvent>,
    ): StatePlugin {
        logger.d { "[create] creating v2 state plugin" }

        val clientState = MutableClientState(networkStateProvider)
        val stateRegistry = StateRegistry(
            scope = scope,
            currentUserId = { clientState.user.value?.id },
        )
        val chatEventHandler: ChatEventHandler = DefaultChatEventHandler(
            channels = stateRegistry.channels,
            clientState = clientState,
        )
        val logicRegistry = LogicRegistry(
            chatClient = chatClient,
            stateRegistry = stateRegistry,
            chatApi = chatApi,
            chatEventHandler = chatEventHandler,
            currentUserIdProvider = { clientState.user.value?.id },
        )
        val queryChannelsLogic = QueryChannelsLogic(
            chatClient = chatClient,
            stateRegistry = stateRegistry,
            queryChannelsStateLogic = QueryChannelsStateLogic(
                stateRegistry.queryChannels(NeutralFilterObject)
                    .let { it as org.example.project.v2.client.internal.state.plugin.state.querychannels.internal.QueryChannelsMutableState }
            ),
            eventHandler = chatEventHandler,
            filter = NeutralFilterObject,
        )
        val eventHandler: EventHandler = EventHandlerSequential(
            events = events,
            queryChannelsLogic = queryChannelsLogic,
            logicRegistry = logicRegistry,
            scope = scope,
        )

        return StatePlugin(
            eventHandler = eventHandler,
            chatEventHandler = chatEventHandler,
            logicRegistry = logicRegistry,
            channelMarkReadListener = ChannelMarkReadListenerState(logicRegistry),
            mutableClientState = clientState,
            clientState = clientState,
            stateRegistry = stateRegistry,
        )
    }
}
