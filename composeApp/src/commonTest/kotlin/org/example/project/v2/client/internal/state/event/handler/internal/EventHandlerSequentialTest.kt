package org.example.project.v2.client.internal.state.event.handler.internal

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.example.project.v2.client.ChatClient
import org.example.project.v2.client.api.event.ChatEventHandler
import org.example.project.v2.client.api.event.EventHandlingResult
import org.example.project.v2.client.api.state.StateRegistry
import org.example.project.v2.client.events.ChatEvent
import org.example.project.v2.client.events.ConnectedEvent
import org.example.project.v2.client.events.NewMessageEvent
import org.example.project.v2.client.events.MessageReadEvent
import org.example.project.v2.client.internal.state.plugin.logic.internal.LogicRegistry
import org.example.project.v2.client.internal.state.plugin.logic.querychannels.internal.QueryChannelsLogic
import org.example.project.v2.client.internal.state.plugin.logic.querychannels.internal.QueryChannelsStateLogic
import org.example.project.v2.client.internal.state.plugin.state.querychannels.internal.QueryChannelsMutableState
import org.example.project.v2.core.models.Channel
import org.example.project.v2.core.models.NeutralFilterObject
import org.example.project.v2.core.models.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EventHandlerSequentialTest {

    @Test
    fun handleEvents_addAndRemoveChannelUpdatesStateRegistryInOrder() = runTest {
        val stateRegistry = StateRegistry(scope = backgroundScope)
        val channel = channel(id = "general")
        val addEvent = newMessageEvent(channel)
        val removeEvent = connectedEvent()
        val eventHandler = ChatEventHandler { event, _, _ ->
            when (event) {
                addEvent -> EventHandlingResult.Add(channel)
                removeEvent -> EventHandlingResult.Remove(channel.cid)
                else -> EventHandlingResult.Skip
            }
        }
        val sut = EventHandlerSequential(
            events = emptyFlow(),
            queryChannelsLogic = QueryChannelsLogic(
                chatClient = clientWithChannelResponse(channel, backgroundScope),
                stateRegistry = stateRegistry,
                queryChannelsStateLogic = QueryChannelsStateLogic(
                    QueryChannelsMutableState(
                        identifier = org.example.project.v2.client.internal.state.plugin.QueryChannelsIdentifier.Standard(NeutralFilterObject),
                        filter = NeutralFilterObject,
                        scope = backgroundScope,
                        latestChannels = stateRegistry.channels,
                    ),
                ),
                eventHandler = eventHandler,
                filter = NeutralFilterObject,
            ),
            logicRegistry = logicRegistry(clientWithChannelResponse(channel, backgroundScope), stateRegistry, eventHandler),
            scope = backgroundScope,
        )

        sut.handleEvents(addEvent, removeEvent)

        assertNull(stateRegistry.getChannel(channel.cid))
    }

    @Test
    fun handleEvents_watchAndAddFetchesChannelAndStoresIt() = runTest {
        val stateRegistry = StateRegistry(scope = backgroundScope)
        val channel = channel(
            id = "general",
            name = "General",
            messages = listOf(
                org.example.project.v2.core.models.Message(
                    id = "m1",
                    cid = "messaging:general",
                    text = "hello",
                    createdAt = "2026-05-30T10:00:00Z",
                    user = User(id = "alice", name = "Alice"),
                ),
            ),
        )
        val watchEvent = connectedEvent()
        val sut = EventHandlerSequential(
            events = emptyFlow(),
            queryChannelsLogic = QueryChannelsLogic(
                chatClient = clientWithChannelResponse(channel, backgroundScope),
                stateRegistry = stateRegistry,
                queryChannelsStateLogic = QueryChannelsStateLogic(
                    QueryChannelsMutableState(
                        identifier = org.example.project.v2.client.internal.state.plugin.QueryChannelsIdentifier.Standard(NeutralFilterObject),
                        filter = NeutralFilterObject,
                        scope = backgroundScope,
                        latestChannels = stateRegistry.channels,
                    ),
                ),
                eventHandler = ChatEventHandler { _, _, _ -> EventHandlingResult.WatchAndAdd(channel.cid) },
                filter = NeutralFilterObject,
            ),
            logicRegistry = logicRegistry(
                clientWithChannelResponse(channel, backgroundScope),
                stateRegistry,
                ChatEventHandler { _, _, _ -> EventHandlingResult.WatchAndAdd(channel.cid) },
            ),
            scope = backgroundScope,
        )

        sut.handleEvents(watchEvent)

        assertEquals(channel, stateRegistry.getChannel(channel.cid))
    }

    @Test
    fun handleEvents_newMessageUpdatesStoredMessagesForExistingChannel() = runTest {
        val stateRegistry = StateRegistry(scope = backgroundScope)
        val channel = channel(id = "general", name = "General")
        stateRegistry.upsertChannel(channel)
        val event = newMessageEvent(channel)
        val sut = EventHandlerSequential(
            events = emptyFlow(),
            queryChannelsLogic = QueryChannelsLogic(
                chatClient = clientWithChannelResponse(channel, backgroundScope),
                stateRegistry = stateRegistry,
                queryChannelsStateLogic = QueryChannelsStateLogic(
                    QueryChannelsMutableState(
                        identifier = org.example.project.v2.client.internal.state.plugin.QueryChannelsIdentifier.Standard(NeutralFilterObject),
                        filter = NeutralFilterObject,
                        scope = backgroundScope,
                        latestChannels = stateRegistry.channels,
                    ),
                ),
                eventHandler = ChatEventHandler { _, _, _ -> EventHandlingResult.Skip },
                filter = NeutralFilterObject,
            ),
            logicRegistry = logicRegistry(
                clientWithChannelResponse(channel, backgroundScope),
                stateRegistry,
                ChatEventHandler { _, _, _ -> EventHandlingResult.Skip },
            ),
            scope = backgroundScope,
        )

        sut.handleEvents(event)

        assertEquals(listOf(event.message), stateRegistry.getMessages(channel.cid))
        assertEquals(listOf(event.message), stateRegistry.getChannel(channel.cid)?.messages)
    }

    @Test
    fun handleEvents_messageReadUpdatesReadsState() = runTest {
        val stateRegistry = StateRegistry(
            scope = backgroundScope,
            currentUserId = { "bob" },
        )
        val channel = channel(id = "general", name = "General")
        stateRegistry.upsertChannel(channel)
        stateRegistry.setMessages(
            channel.cid,
            listOf(
                org.example.project.v2.core.models.Message(
                    id = "m1",
                    cid = channel.cid,
                    text = "hello",
                    createdAt = "2026-05-30T10:00:00Z",
                    user = User(id = "bob", name = "Bob"),
                ),
            ),
        )
        val event = MessageReadEvent(
            type = "message.read",
            createdAt = "2026-05-30T10:01:00Z",
            rawCreatedAt = "2026-05-30T10:01:00Z",
            user = User(id = "alice", name = "Alice"),
            cid = channel.cid,
            channelType = channel.type,
            channelId = channel.id,
            lastReadMessageId = "m1",
        )
        val eventHandler = ChatEventHandler { _, _, _ -> EventHandlingResult.Skip }
        val sut = EventHandlerSequential(
            events = emptyFlow(),
            queryChannelsLogic = QueryChannelsLogic(
                chatClient = clientWithChannelResponse(channel, backgroundScope),
                stateRegistry = stateRegistry,
                queryChannelsStateLogic = QueryChannelsStateLogic(
                    QueryChannelsMutableState(
                        identifier = org.example.project.v2.client.internal.state.plugin.QueryChannelsIdentifier.Standard(NeutralFilterObject),
                        filter = NeutralFilterObject,
                        scope = backgroundScope,
                        latestChannels = stateRegistry.channels,
                    ),
                ),
                eventHandler = eventHandler,
                filter = NeutralFilterObject,
            ),
            logicRegistry = logicRegistry(
                clientWithChannelResponse(channel, backgroundScope),
                stateRegistry,
                eventHandler,
            ),
            scope = backgroundScope,
        )

        sut.handleEvents(event)

        val reads = (stateRegistry.channel(channel.cid) as org.example.project.v2.client.internal.state.plugin.state.channel.internal.ChannelStateImpl).reads.value
        assertEquals("m1", reads["alice"])
    }

    private fun clientWithChannelResponse(
        channel: Channel,
        scope: kotlinx.coroutines.CoroutineScope,
    ): ChatClient {
        val engine = MockEngine {
            respond(
                content = """
                    {
                      "channel": {
                        "id": "${channel.id}",
                        "type": "${channel.type}",
                        "name": "${channel.name}",
                        "messages": [
                          {
                            "id": "m1",
                            "cid": "${channel.cid}",
                            "text": "hello",
                            "created_at": "2026-05-30T10:00:00Z",
                            "user": {
                              "id": "alice",
                              "name": "Alice"
                            }
                          }
                        ],
                        "members": []
                      }
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = io.ktor.http.headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        encodeDefaults = true
                    },
                )
            }
        }

        return ChatClient.Builder(
            apiKey = "test-api-key",
            wssUrl = "wss://example.com/ws",
        )
            .baseUrl("https://example.com")
            .httpClient(httpClient)
            .clientScope(scope)
            .build()
    }

    private fun channel(
        id: String,
        name: String = "Channel",
        type: String = "messaging",
        messages: List<org.example.project.v2.core.models.Message> = emptyList(),
    ): Channel = Channel(
        id = id,
        type = type,
        name = name,
        messages = messages,
    )

    private fun newMessageEvent(channel: Channel): NewMessageEvent {
        val user = User(id = "alice", name = "Alice")
        return NewMessageEvent(
            type = "message.new",
            createdAt = "2026-05-30T10:00:00Z",
            rawCreatedAt = "2026-05-30T10:00:00Z",
            user = user,
            cid = channel.cid,
            channelType = channel.type,
            channelId = channel.id,
            message = org.example.project.v2.core.models.Message(
                id = "m1",
                cid = channel.cid,
                text = "hello",
                createdAt = "2026-05-30T10:00:00Z",
                user = user,
            ),
        )
    }

    private fun connectedEvent(): ChatEvent = ConnectedEvent(
        type = "health.check",
        createdAt = "2026-05-30T10:00:00Z",
        rawCreatedAt = "2026-05-30T10:00:00Z",
        me = User(id = "alice", name = "Alice"),
        connectionId = "connection-1",
    )

    private fun logicRegistry(
        chatClient: ChatClient,
        stateRegistry: StateRegistry,
        eventHandler: ChatEventHandler,
    ): LogicRegistry = LogicRegistry(
        chatClient = chatClient,
        stateRegistry = stateRegistry,
        chatApi = object : org.example.project.v2.client.api.ChatApi {
            override suspend fun queryChannels(request: org.example.project.v2.client.api.models.QueryChannelsRequest): List<Channel> = emptyList()
            override suspend fun markRead(channelType: String, channelId: String, messageId: String) = Unit
            override suspend fun sendMessage(
                channelType: String,
                channelId: String,
                request: org.example.project.v2.client.api.models.SendMessageRequest,
            ): org.example.project.v2.core.models.Message = org.example.project.v2.core.models.Message()
            override suspend fun queryChannel(
                channelType: String,
                channelId: String,
                query: org.example.project.v2.client.api.models.QueryChannelRequest,
            ): Channel = Channel(id = channelId, type = channelType)
        },
        chatEventHandler = eventHandler,
        currentUserIdProvider = { "alice" },
    )
}
