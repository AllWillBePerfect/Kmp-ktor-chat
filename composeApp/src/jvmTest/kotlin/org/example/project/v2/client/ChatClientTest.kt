package org.example.project.v2.client

import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import org.example.project.v2.client.api.ChatApi
import org.example.project.v2.client.api.internal.TokenProvider
import org.example.project.v2.client.api.models.QueryChannelRequest
import org.example.project.v2.client.api.models.QueryChannelsRequest
import org.example.project.v2.client.api.models.SendMessageRequest
import org.example.project.v2.client.events.ChatEvent
import org.example.project.v2.client.events.ConnectedEvent
import org.example.project.v2.client.events.UnknownEvent
import org.example.project.v2.client.network.NetworkStateProvider
import org.example.project.v2.client.socket.FakeChatSocket
import org.example.project.v2.core.models.Channel
import org.example.project.v2.core.models.ConnectionState
import org.example.project.v2.core.models.InitializationState
import org.example.project.v2.core.models.Message
import org.example.project.v2.core.models.SyncStatus
import org.example.project.v2.core.models.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChatClientTest {

    private companion object {
        val eventA = unknownEvent("a")
        val eventB = unknownEvent("b")
        val eventC = unknownEvent("c")

        fun unknownEvent(type: String) = UnknownEvent(type = type, rawPayload = "{}")
    }

    @Test
    fun instantiateChannelClientByTypeAndId() = runTest {
        val client = fixture(backgroundScope).client

        val channelClient = client.channel("messaging", "general")

        assertEquals("messaging", channelClient.channelType)
        assertEquals("general", channelClient.channelId)
    }

    @Test
    fun instantiateChannelClientByCid() = runTest {
        val client = fixture(backgroundScope).client

        val channelClient = client.channel("messaging:general")

        assertEquals("messaging", channelClient.channelType)
        assertEquals("general", channelClient.channelId)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun simpleSubscribeForMultipleEvents() = runTest {
        val fixture = fixture(backgroundScope)
        val result = mutableListOf<ChatEvent>()

        fixture.client.subscribe { result.add(it) }
        runCurrent()

        fixture.socket.emitEvents(eventA, eventB, eventC)
        runCurrent()

        assertEquals(listOf<ChatEvent>(eventA, eventB, eventC), result)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun subscribeSingleDeliversOnlyFirstMatchingEvent() = runTest {
        val fixture = fixture(backgroundScope)
        val result = mutableListOf<ChatEvent>()

        fixture.client.subscribeSingle(filter = { it.type == "b" }) { result.add(it) }
        runCurrent()

        fixture.socket.emitEvents(eventA, eventB, eventC, eventB)
        runCurrent()

        assertEquals(listOf<ChatEvent>(eventB), result)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun unsubscribeFromEvents() = runTest {
        val fixture = fixture(backgroundScope)
        val result = mutableListOf<ChatEvent>()

        val disposable = fixture.client.subscribe { result.add(it) }
        runCurrent()

        fixture.socket.mockEventReceived(eventA)
        runCurrent()
        disposable.dispose()
        fixture.socket.emitEvents(eventB, eventC)
        runCurrent()

        assertEquals(listOf<ChatEvent>(eventA), result)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun socketConnectedEventUpdatesClientState_andDisconnectClearsIt() = runTest {
        val fixture = fixture(backgroundScope)
        val connected = ConnectedEvent(
            type = "health.check",
            createdAt = "2026-05-30T12:00:00Z",
            rawCreatedAt = "2026-05-30T12:00:00Z",
            me = User(id = "alice", name = "Alice"),
            connectionId = "connection-1",
        )

        fixture.socket.mockConnected(connected)
        runCurrent()

        assertEquals(ConnectionState.CONNECTED, fixture.client.clientState.connectionState.value)
        assertEquals(connected.me, fixture.client.clientState.user.value)

        fixture.client.disconnect()
        runCurrent()

        assertEquals(ConnectionState.OFFLINE, fixture.client.clientState.connectionState.value)
        assertEquals(InitializationState.NOT_INITIALIZED, fixture.client.clientState.initializationState.value)
        assertNull(fixture.client.clientState.user.value)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun reconnectRecoversActiveQueryAndChannelLogic() = runTest {
        val fixture = recoveryFixture(backgroundScope)
        val connected = ConnectedEvent(
            type = "health.check",
            createdAt = "2026-05-30T12:00:00Z",
            rawCreatedAt = "2026-05-30T12:00:00Z",
            me = User(id = "alice", name = "Alice"),
            connectionId = "connection-1",
        )
        fixture.client.connectUser(User(id = "alice", name = "Alice"), token = "token")
        fixture.socket.mockConnected(connected)
        fixture.client.queryChannels(QueryChannelsRequest(limit = 30))
        fixture.client.queryChannel("messaging", "general", QueryChannelRequest())
        runCurrent()

        fixture.socket.mockDisconnected("lost_connection")
        fixture.socket.mockConnected(connected.copy(connectionId = "connection-2"))
        runCurrent()

        assertEquals(2, fixture.api.queryChannelsCalls)
        assertEquals(2, fixture.api.queryChannelCalls)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun sendMessageAddsPendingMessageImmediatelyAndReplacesItWithServerMessage() = runTest {
        val fixture = pendingSendFixture(backgroundScope)
        fixture.client.connectUser(User(id = "alice", name = "Alice"), token = "token")

        val sendDeferred = async {
            fixture.client.channel("messaging", "general").sendMessage(
                Message(text = "hello", clientMessageId = "client-1"),
            )
        }
        runCurrent()

        val pending = fixture.client.channelState("messaging:general").messages.value.single()
        assertEquals("hello", pending.text)
        assertEquals("client-1", pending.clientMessageId)
        assertEquals(SyncStatus.IN_PROGRESS, pending.syncStatus)

        fixture.api.sendOutcome.complete(
            Result.success(
                Message(
                    id = "server-1",
                    cid = "messaging:general",
                    text = "hello",
                    clientMessageId = "client-1",
                    createdAt = "2026-05-31T10:00:00Z",
                    user = User(id = "alice", name = "Alice"),
                ),
            ),
        )
        runCurrent()
        sendDeferred.await()

        val stored = fixture.client.channelState("messaging:general").messages.value.single()
        assertEquals("server-1", stored.id)
        assertEquals(SyncStatus.COMPLETED, stored.syncStatus)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun sendMessageMarksLocalMessageAsFailedWhenApiFails() = runTest {
        val fixture = pendingSendFixture(backgroundScope)
        fixture.client.connectUser(User(id = "alice", name = "Alice"), token = "token")

        val sendDeferred = async {
            runCatching {
                fixture.client.channel("messaging", "general").sendMessage(
                    Message(text = "hello", clientMessageId = "client-1"),
                )
            }
        }
        runCurrent()

        fixture.api.sendOutcome.complete(Result.failure(IllegalStateException("boom")))
        runCurrent()

        assertTrue(sendDeferred.await().isFailure)
        val stored = fixture.client.channelState("messaging:general").messages.value.single()
        assertEquals("client-1", stored.clientMessageId)
        assertEquals(SyncStatus.FAILED_PERMANENTLY, stored.syncStatus)
    }

    private fun fixture(scope: CoroutineScope): Fixture {
        val networkStateProvider = object : NetworkStateProvider {
            override fun isConnected(): Boolean = true
        }
        val socket = FakeChatSocket(scope)
        val client = ChatClient(
            apiKey = "test-api-key",
            wssUrl = "wss://example.com/ws",
            networkStateProvider = networkStateProvider,
            clientScope = scope,
            chatApi = object : ChatApi {
                override suspend fun queryChannels(request: org.example.project.v2.client.api.models.QueryChannelsRequest): List<Channel> {
                    return emptyList()
                }

                override suspend fun sendMessage(
                    channelType: String,
                    channelId: String,
                    request: org.example.project.v2.client.api.models.SendMessageRequest,
                ): org.example.project.v2.core.models.Message {
                    return org.example.project.v2.core.models.Message(
                        cid = "$channelType:$channelId",
                        text = request.text,
                        clientMessageId = request.clientMessageId,
                    )
                }

                override suspend fun markRead(
                    channelType: String,
                    channelId: String,
                    messageId: String,
                ) = Unit

                override suspend fun queryChannel(
                    channelType: String,
                    channelId: String,
                    query: QueryChannelRequest,
                ): Channel = Channel(id = channelId, type = channelType)
            },
            tokenProvider = TokenProvider(),
            chatSocket = socket,
        )
        return Fixture(client = client, socket = socket)
    }

    private fun recoveryFixture(scope: CoroutineScope): RecoveryFixture {
        val networkStateProvider = object : NetworkStateProvider {
            override fun isConnected(): Boolean = true
        }
        val socket = FakeChatSocket(scope)
        val api = RecoveryChatApi()
        val client = ChatClient(
            apiKey = "test-api-key",
            wssUrl = "wss://example.com/ws",
            networkStateProvider = networkStateProvider,
            clientScope = scope,
            chatApi = api,
            tokenProvider = TokenProvider(),
            chatSocket = socket,
        )
        return RecoveryFixture(client = client, socket = socket, api = api)
    }

    private fun pendingSendFixture(scope: CoroutineScope): PendingSendFixture {
        val networkStateProvider = object : NetworkStateProvider {
            override fun isConnected(): Boolean = true
        }
        val socket = FakeChatSocket(scope)
        val api = PendingSendChatApi()
        val client = ChatClient(
            apiKey = "test-api-key",
            wssUrl = "wss://example.com/ws",
            networkStateProvider = networkStateProvider,
            clientScope = scope,
            chatApi = api,
            tokenProvider = TokenProvider(),
            chatSocket = socket,
        )
        return PendingSendFixture(client = client, socket = socket, api = api)
    }

    private data class Fixture(
        val client: ChatClient,
        val socket: FakeChatSocket,
    )

    private data class RecoveryFixture(
        val client: ChatClient,
        val socket: FakeChatSocket,
        val api: RecoveryChatApi,
    )

    private data class PendingSendFixture(
        val client: ChatClient,
        val socket: FakeChatSocket,
        val api: PendingSendChatApi,
    )

    private class RecoveryChatApi : ChatApi {
        var queryChannelsCalls: Int = 0
        var queryChannelCalls: Int = 0

        override suspend fun queryChannels(request: QueryChannelsRequest): List<Channel> {
            queryChannelsCalls += 1
            return listOf(Channel(id = "general", type = "messaging"))
        }

        override suspend fun markRead(channelType: String, channelId: String, messageId: String) = Unit

        override suspend fun sendMessage(
            channelType: String,
            channelId: String,
            request: org.example.project.v2.client.api.models.SendMessageRequest,
        ): org.example.project.v2.core.models.Message {
            return org.example.project.v2.core.models.Message(
                cid = "$channelType:$channelId",
                text = request.text,
                clientMessageId = request.clientMessageId,
            )
        }

        override suspend fun queryChannel(
            channelType: String,
            channelId: String,
            query: QueryChannelRequest,
        ): Channel {
            queryChannelCalls += 1
            return Channel(id = channelId, type = channelType)
        }
    }

    private class PendingSendChatApi : ChatApi {
        val sendOutcome = CompletableDeferred<Result<Message>>()

        override suspend fun queryChannels(request: QueryChannelsRequest): List<Channel> = emptyList()

        override suspend fun markRead(channelType: String, channelId: String, messageId: String) = Unit

        override suspend fun sendMessage(
            channelType: String,
            channelId: String,
            request: SendMessageRequest,
        ): Message {
            return sendOutcome.await().getOrThrow()
        }

        override suspend fun queryChannel(
            channelType: String,
            channelId: String,
            query: QueryChannelRequest,
        ): Channel = Channel(id = channelId, type = channelType)
    }

    private fun FakeChatSocket.emitEvents(vararg events: ChatEvent) {
        events.forEach(::mockEventReceived)
    }

}
