package org.example.project.v2.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.example.project.v2.client.ChatEventListener
import org.example.project.v2.client.api.ChatApi
import org.example.project.v2.client.api.internal.TokenProvider
import org.example.project.v2.client.api.models.QueryChannelRequest
import org.example.project.v2.client.api.models.QueryChannelsRequest
import org.example.project.v2.client.api.models.SendMessageRequest
import org.example.project.v2.client.api.state.QueryChannelsState
import org.example.project.v2.client.channel.state.ChannelState
import org.example.project.v2.client.channel.ChannelClient
import org.example.project.v2.client.di.ChatModule
import org.example.project.v2.client.events.ChatEvent
import org.example.project.v2.client.internal.state.plugin.factory.StreamStatePluginFactory
import org.example.project.v2.client.internal.state.plugin.internal.StatePlugin
import org.example.project.v2.client.internal.state.plugin.state.querychannels.internal.toMutableState
import org.example.project.v2.client.network.NetworkStateProvider
import org.example.project.v2.client.setup.state.ClientState
import org.example.project.v2.client.socket.ChatSocket
import org.example.project.v2.client.socket.SocketFactory
import org.example.project.v2.client.socket.SocketListener
import org.example.project.v2.client.utils.observable.ChatEventsObservable
import org.example.project.v2.client.utils.observable.Disposable
import org.example.project.v2.core.models.Channel
import org.example.project.v2.core.models.ConnectionState
import org.example.project.v2.core.models.InitializationState
import org.example.project.v2.core.models.Message
import org.example.project.v2.core.models.User
import org.example.project.v2.platform.taggedLogger
import io.ktor.client.HttpClient

class ChatClient internal constructor(
    private val apiKey: String,
    private val wssUrl: String,
    private val networkStateProvider: NetworkStateProvider,
    private val clientScope: CoroutineScope,
    private val chatApi: ChatApi,
    private val tokenProvider: TokenProvider,
    private val chatSocket: ChatSocket,
) {
    private val logger by taggedLogger(TAG)
    private val eventsObservable = ChatEventsObservable(clientScope, chatSocket)

    private val statePlugin: StatePlugin
    private var isManualDisconnect: Boolean = false
    private var shouldRecoverOnConnect: Boolean = false
    private val clientStateMutable
        get() = statePlugin.mutableClientState
    private val socketStateListener = object : SocketListener() {
        override fun onConnecting() {
            clientStateMutable.setConnectionState(ConnectionState.CONNECTING)
        }

        override fun onConnected(event: org.example.project.v2.client.events.ConnectedEvent) {
            clientStateMutable.setConnectionState(ConnectionState.CONNECTED)
            clientStateMutable.setUser(event.me)
            if (shouldRecoverOnConnect && !isManualDisconnect) {
                shouldRecoverOnConnect = false
                clientScope.launch {
                    logger.d { "[recover] start" }
                    runCatching {
                        statePlugin.logicRegistry.recover()
                    }.onFailure { throwable ->
                        logger.e(throwable) { "[recover] failed" }
                    }
                }
            }
        }

        override fun onDisconnected(cause: String?) {
            clientStateMutable.setConnectionState(ConnectionState.OFFLINE)
            if (!isManualDisconnect) {
                shouldRecoverOnConnect = true
            }
        }

        override fun onError(error: Throwable) {
            clientStateMutable.setConnectionState(ConnectionState.OFFLINE)
            if (!isManualDisconnect) {
                shouldRecoverOnConnect = true
            }
        }
    }

    val clientState: ClientState
        get() = statePlugin.clientState
    val events: Flow<ChatEvent> = chatSocket.events
    val channels: StateFlow<Map<String, Channel>?>
        get() = statePlugin.stateRegistry.channels
    val messagesByCid: StateFlow<Map<String, List<Message>>>
        get() = statePlugin.stateRegistry.messagesByCid

    fun queryChannelsState(filter: org.example.project.v2.core.models.FilterObject): QueryChannelsState {
        return statePlugin.stateRegistry.queryChannels(filter)
    }

    fun channelState(cid: String): ChannelState {
        return statePlugin.stateRegistry.channel(cid)
    }

    init {
        statePlugin = StreamStatePluginFactory(
            networkStateProvider = networkStateProvider,
        ).create(
            chatClient = this,
            chatApi = chatApi,
            scope = clientScope,
            events = chatSocket.events,
        )
        chatSocket.subscribe(socketStateListener)
    }

    fun subscribe(
        filter: (ChatEvent) -> Boolean = { true },
        listener: ChatEventListener<ChatEvent>,
    ): Disposable {
        return eventsObservable.subscribe(filter, listener)
    }

    fun subscribeSingle(
        filter: (ChatEvent) -> Boolean = { true },
        listener: ChatEventListener<ChatEvent>,
    ): Disposable {
        return eventsObservable.subscribeSingle(filter, listener)
    }

    fun subscribe(listener: SocketListener) {
        chatSocket.subscribe(listener)
    }

    fun unsubscribe(listener: SocketListener) {
        chatSocket.unsubscribe(listener)
    }

    fun channel(channelType: String, channelId: String): ChannelClient {
        return ChannelClient(channelType = channelType, channelId = channelId, client = this)
    }

    fun channel(cid: String): ChannelClient {
        val separatorIndex = cid.indexOf(':')
        require(separatorIndex > 0 && separatorIndex < cid.lastIndex) {
            "Expected cid in format 'type:id', but was '$cid'"
        }
        val type = cid.substring(0, separatorIndex)
        val id = cid.substring(separatorIndex + 1)
        return channel(channelType = type, channelId = id)
    }

    fun connectUser(
        user: User,
        token: String?,
    ) {
        isManualDisconnect = false
        shouldRecoverOnConnect = false
        tokenProvider.token = token
        val connectionConf = SocketFactory.ConnectionConf.UserConnectionConf(
            endpoint = wssUrl,
            apiKey = apiKey,
            user = user,
            token = token,
        )
        logger.i { "[connect] start" }
        clientStateMutable.setInitializationState(InitializationState.INITIALIZING)
        clientStateMutable.setUser(user)
        statePlugin.eventHandler.startListening()
        chatSocket.connect(connectionConf)
        clientStateMutable.setInitializationState(InitializationState.COMPLETE)
    }

    fun disconnect() {
        logger.i { "[disconnect] start" }
        isManualDisconnect = true
        shouldRecoverOnConnect = false
        chatSocket.disconnect()
        statePlugin.eventHandler.stopListening()
        tokenProvider.token = null
        clientStateMutable.clearState()
    }

    suspend fun queryChannels(request: QueryChannelsRequest): List<Channel> {
        return statePlugin.logicRegistry.queryChannels(request.filter).query(request)
    }

    internal suspend fun queryChannelsInternal(request: QueryChannelsRequest): List<Channel> {
        return chatApi.queryChannels(request)
    }

    suspend fun sendMessage(
        channelType: String,
        channelId: String,
        request: SendMessageRequest,
    ): Message {
        return statePlugin.logicRegistry.channel(channelType, channelId).sendMessage(request)
    }

    suspend fun markRead(
        channelType: String,
        channelId: String,
    ) {
        val shouldMarkRead = statePlugin.channelMarkReadListener
            .onChannelMarkReadPrecondition(channelType, channelId)
        if (!shouldMarkRead) {
            logger.v { "[markRead] skipped; cid=$channelType:$channelId" }
            return
        }
        val channelState = statePlugin.logicRegistry.channel(channelType, channelId).channelState()
        val messageId = channelState.messages.value.lastOrNull()?.id.orEmpty()
        chatApi.markRead(
            channelType = channelType,
            channelId = channelId,
            messageId = messageId,
        )
        logger.d { "[markRead] completed; cid=$channelType:$channelId lastMessageId=$messageId" }
    }

    fun markReadAsync(
        channelType: String,
        channelId: String,
    ) {
        val shouldMarkRead = statePlugin.channelMarkReadListener
            .onChannelMarkReadPrecondition(channelType, channelId)
        if (!shouldMarkRead) {
            logger.v { "[markReadAsync] skipped; cid=$channelType:$channelId" }
            return
        }
        val channelState = statePlugin.logicRegistry.channel(channelType, channelId).channelState()
        val messageId = channelState.messages.value.lastOrNull()?.id.orEmpty()
        clientScope.launch {
            runCatching {
                chatApi.markRead(
                    channelType = channelType,
                    channelId = channelId,
                    messageId = messageId,
                )
            }.onSuccess {
                logger.d { "[markReadAsync] completed; cid=$channelType:$channelId lastMessageId=$messageId" }
            }.onFailure { throwable ->
                logger.e(throwable) { "[markReadAsync] failed; cid=$channelType:$channelId" }
            }
        }
    }

    suspend fun keystroke(
        channelType: String,
        channelId: String,
        parentId: String? = null,
    ) {
        chatSocket.send(
            buildJsonObject {
                put("type", JsonPrimitive("typing.start"))
                put("channel_type", JsonPrimitive(channelType))
                put("channel_id", JsonPrimitive(channelId))
                parentId?.let { put("parent_id", JsonPrimitive(it)) }
            }.toString(),
        )
    }

    suspend fun stopTyping(
        channelType: String,
        channelId: String,
        parentId: String? = null,
    ) {
        chatSocket.send(
            buildJsonObject {
                put("type", JsonPrimitive("typing.stop"))
                put("channel_type", JsonPrimitive(channelType))
                put("channel_id", JsonPrimitive(channelId))
                parentId?.let { put("parent_id", JsonPrimitive(it)) }
            }.toString(),
        )
    }

    suspend fun queryChannel(
        channelType: String,
        channelId: String,
        request: QueryChannelRequest,
    ): Channel? {
        return statePlugin.logicRegistry.channel(channelType, channelId).watch(request)
    }

    private companion object {
        const val TAG = "Chat:Client"
    }

    class Builder(
        private val apiKey: String,
        private val wssUrl: String,
    ) {
        private var baseUrl: String = wssUrl
            .replaceFirst("wss://", "https://")
            .replaceFirst("ws://", "http://")
        private var networkStateProvider: NetworkStateProvider = object : NetworkStateProvider {
            override fun isConnected(): Boolean = true
        }
        private var clientScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private var httpClient: HttpClient? = null

        fun baseUrl(baseUrl: String): Builder = apply {
            this.baseUrl = baseUrl.trimEnd('/')
        }

        fun httpClient(httpClient: HttpClient): Builder = apply {
            this.httpClient = httpClient
        }

        fun networkStateProvider(networkStateProvider: NetworkStateProvider): Builder = apply {
            this.networkStateProvider = networkStateProvider
        }

        fun clientScope(scope: CoroutineScope): Builder = apply {
            this.clientScope = scope
        }

        fun build(): ChatClient {
            val chatModule = ChatModule(
                apiKey = apiKey,
                baseUrl = baseUrl,
                clientScope = clientScope,
                networkStateProvider = networkStateProvider,
                customHttpClient = httpClient,
            )
            return ChatClient(
                apiKey = apiKey,
                wssUrl = wssUrl,
                networkStateProvider = chatModule.networkStateProvider(),
                clientScope = clientScope,
                chatApi = chatModule.chatApi,
                tokenProvider = chatModule.tokenProvider(),
                chatSocket = chatModule.chatSocket,
            )
        }
    }
}
