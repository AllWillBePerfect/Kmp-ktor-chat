package org.example.project.v2.client.internal.state.plugin.logic.channel.internal

import org.example.project.v2.client.api.ChatApi
import org.example.project.v2.client.api.models.QueryChannelRequest
import org.example.project.v2.client.api.models.SendMessageRequest
import org.example.project.v2.client.api.state.StateRegistry
import org.example.project.v2.client.events.ChatEvent
import org.example.project.v2.client.channel.state.ChannelState
import org.example.project.v2.client.internal.state.plugin.state.channel.internal.ChannelStateImpl
import org.example.project.v2.core.models.Channel
import org.example.project.v2.core.models.Message
import org.example.project.v2.platform.taggedLogger

internal class ChannelLogicImpl(
    private val channelType: String,
    private val channelId: String,
    private val chatApi: ChatApi,
    private val state: ChannelStateImpl,
    private val stateRegistry: StateRegistry,
    private val currentUserIdProvider: () -> String?,
    private val onChannelStateUpdated: (String) -> Unit,
) : ChannelLogic {
    private val logger by taggedLogger("Chat:V2ChannelLogic")
    private var lastWatchRequest: QueryChannelRequest? = null

    override val cid: String = "$channelType:$channelId"

    private val messageStateUpdater = ChannelMessageStateUpdater(
        cid = cid,
        stateRegistry = stateRegistry,
        currentUserIdProvider = currentUserIdProvider,
    )

    private val channelEventHandler: ChannelEventHandler = ChannelEventHandlerImpl(
        cid = cid,
        stateRegistry = stateRegistry,
        messageStateUpdater = messageStateUpdater,
        onChannelStateUpdated = onChannelStateUpdated,
    )

    override fun channelState(): ChannelState = state

    override suspend fun watch(request: QueryChannelRequest): Channel {
        lastWatchRequest = request
        state.setLoading(true)
        return try {
            chatApi.queryChannel(
                channelType = channelType,
                channelId = channelId,
                query = request,
            ).also { channel ->
                logger.d { "[watch] cid=$cid messages=${channel.messages.size}" }
                channelEventHandler.syncChannel(channel)
            }
        } finally {
            state.setLoading(false)
        }
    }

    override suspend fun sendMessage(request: SendMessageRequest): Message {
        val localMessage = messageStateUpdater.prepareLocalMessage(request)
        messageStateUpdater.applyLocalSend(localMessage)
        return runCatching {
            chatApi.sendMessage(
                channelType = channelType,
                channelId = channelId,
                request = request.copy(clientMessageId = localMessage.clientMessageId),
            )
        }.onSuccess { message ->
            logger.d { "[sendMessage] cid=$cid id=${message.id}" }
            messageStateUpdater.applySendSuccess(message)
        }.onFailure { throwable ->
            logger.e(throwable) { "[sendMessage] failed; cid=$cid clientMessageId=${localMessage.clientMessageId}" }
            messageStateUpdater.applySendFailure(localMessage)
        }.getOrThrow()
    }

    override suspend fun recover() {
        val request = lastWatchRequest ?: return
        logger.d { "[recover] cid=$cid" }
        watch(request)
    }

    override fun markRead(): Boolean {
        val currentUserId = currentUserIdProvider().orEmpty()
        return state.markRead(currentUserId).also { shouldMarkRead ->
            if (!shouldMarkRead) {
                logger.v { "[markRead] skipped locally; cid=$cid" }
                return@also
            }
            stateRegistry.setRead(cid, currentUserId, state.messages.value.lastOrNull()?.id)
            onChannelStateUpdated(cid)
            logger.d { "[markRead] prepared locally; cid=$cid" }
        }
    }

    override fun handleEvent(event: ChatEvent) {
        channelEventHandler.handle(event)
    }
}
