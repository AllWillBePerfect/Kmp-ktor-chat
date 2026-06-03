package org.example.project.v2.client.internal.state.plugin.logic.querychannels.internal

import org.example.project.v2.client.ChatClient
import kotlinx.coroutines.flow.StateFlow
import org.example.project.v2.client.api.event.ChatEventHandler
import org.example.project.v2.client.api.event.EventHandlingResult
import org.example.project.v2.client.api.models.QueryChannelsRequest
import org.example.project.v2.client.api.state.StateRegistry
import org.example.project.v2.client.events.ChatEvent
import org.example.project.v2.client.events.CidEvent
import org.example.project.v2.client.internal.state.plugin.logic.channel.internal.ChannelMessageStateUpdater
import org.example.project.v2.core.models.Channel
import org.example.project.v2.core.models.FilterObject
import org.example.project.v2.platform.taggedLogger

internal class QueryChannelsLogic(
    private val chatClient: ChatClient,
    private val stateRegistry: StateRegistry,
    private val queryChannelsStateLogic: QueryChannelsStateLogic,
    private val eventHandler: ChatEventHandler,
    private val filter: FilterObject,
) {
    private val logger by taggedLogger("Chat:V2QueryChannelsLogic")
    private val queryChannelsEventHandler: QueryChannelsEventHandler = QueryChannelsEventHandlerImpl(
        stateRegistry = stateRegistry,
        queryChannelsStateLogic = queryChannelsStateLogic,
    )

    internal val channels: StateFlow<Map<String, Channel>?> = stateRegistry.channels

    internal suspend fun query(request: QueryChannelsRequest): List<Channel> {
        val hasOffset = request.offset > 0
        setLoading(true, hasOffset)
        queryChannelsStateLogic.setCurrentRequest(request)
        return try {
            val channels = chatClient.queryChannelsInternal(request)
            queryChannelsStateLogic.addChannels(channels)
            queryChannelsStateLogic.setEndOfChannels(channels.size < request.limit)
            channels.forEach { channel ->
                addChannel(channel)
            }
            channels
        } catch (throwable: Throwable) {
            queryChannelsStateLogic.initializeChannelsIfNeeded()
            throw throwable
        } finally {
            setLoading(false, hasOffset)
        }
    }

    internal suspend fun recover() {
        val request = queryChannelsStateLogic.getState().currentRequest.value ?: return
        logger.d { "[recover] filter=$filter" }
        query(request)
    }

    private fun setLoading(isLoading: Boolean, hasOffset: Boolean) {
        if (hasOffset) {
            queryChannelsStateLogic.setLoadingMore(isLoading)
        } else {
            queryChannelsStateLogic.setLoadingFirstPage(isLoading)
        }
    }

    internal suspend fun addChannel(channel: Channel) {
        if (channel.messages.isNotEmpty()) {
            ChannelMessageStateUpdater(
                cid = channel.cid,
                stateRegistry = stateRegistry,
                currentUserIdProvider = { null },
            ).applySnapshot(channel)
        }
        stateRegistry.upsertChannel(channel)
    }

    internal fun refreshChannelState(cid: String) {
        val channel = stateRegistry.getActiveChannel(cid) ?: return
        queryChannelsStateLogic.refreshChannelState(cid, channel)
    }

    internal fun refreshChannelsState(cids: Collection<String>) {
        val refreshed = cids.mapNotNull { cid ->
            stateRegistry.getActiveChannel(cid)?.let { cid to it }
        }.toMap()
        if (refreshed.isEmpty()) return
        queryChannelsStateLogic.refreshChannelsState(refreshed)
    }

    internal suspend fun watchAndAddChannel(cid: String) {
        val channel = chatClient.channel(cid).watch()
        if (channel != null) {
            addChannel(channel)
        } else {
            logger.w { "[watchAndAddChannel] watch returned null; cid=$cid" }
        }
    }

    internal suspend fun removeChannel(cid: String) {
        stateRegistry.removeChannel(cid)
    }

    internal suspend fun applyQueryStateForEvent(event: ChatEvent) {
        queryChannelsEventHandler.handle(event)
    }

    internal suspend fun parseChatEventResults(chatEvents: List<ChatEvent>): List<EventHandlingResult> {
        val cids = chatEvents.filterIsInstance<CidEvent>().map(CidEvent::cid).distinct()
        val resolvedChannels = stateRegistry.getChannels(cids)

        return chatEvents.map { event ->
            val cachedChannel = (event as? CidEvent)?.let { resolvedChannels[it.cid] }
            eventHandler.handleChatEvent(
                event = event,
                filter = filter,
                cachedChannel = cachedChannel,
            )
        }
    }
}
