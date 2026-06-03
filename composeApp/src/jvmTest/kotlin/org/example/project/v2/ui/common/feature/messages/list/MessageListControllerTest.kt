package org.example.project.v2.ui.common.feature.messages.list

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.example.project.v2.client.ChatClient
import org.example.project.v2.client.api.ChatApi
import org.example.project.v2.client.api.internal.TokenProvider
import org.example.project.v2.client.api.models.QueryChannelRequest
import org.example.project.v2.client.api.models.QueryChannelsRequest
import org.example.project.v2.client.api.models.SendMessageRequest
import org.example.project.v2.client.events.ConnectedEvent
import org.example.project.v2.client.internal.state.plugin.state.channel.internal.ChannelStateImpl
import org.example.project.v2.client.network.NetworkStateProvider
import org.example.project.v2.client.socket.FakeChatSocket
import org.example.project.v2.core.models.Channel
import org.example.project.v2.core.models.Message
import org.example.project.v2.core.models.User
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageListControllerTest {

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun stateReflectsMessagesTypingReadsAndCurrentUser() = runTest {
        val fixture = fixture(backgroundScope)
        val controller = MessageListController(
            cid = "messaging:general",
            chatClient = fixture.client,
            channelState = fixture.client.channelState("messaging:general"),
            scope = backgroundScope,
        )

        fixture.client.connectUser(User(id = "alice", name = "Alice"), token = "token")
        fixture.socket.mockConnected(
            ConnectedEvent(
                type = "health.check",
                createdAt = "2026-05-31T10:00:00Z",
                rawCreatedAt = "2026-05-31T10:00:00Z",
                me = User(id = "alice", name = "Alice"),
                connectionId = "connection-1",
            ),
        )
        fixture.client.queryChannel("messaging", "general", QueryChannelRequest())
        val stateImpl = fixture.client.channelState("messaging:general") as ChannelStateImpl
        stateImpl.startTyping(User(id = "bob", name = "Bob"))
        stateImpl.setRead(userId = "bob", lastReadMessageId = "message-1")
        runCurrent()

        assertEquals(1, controller.state.value.messages.size)
        assertEquals("Bob is typing...", controller.state.value.typingLabel)
        assertEquals(1, controller.state.value.readCountForMessage(controller.state.value.messages.first()))
        assertEquals("alice", controller.state.value.currentUserId)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun latestMessageTriggersMarkReadOnce() = runTest {
        val fixture = fixture(backgroundScope)
        val controller = MessageListController(
            cid = "messaging:general",
            chatClient = fixture.client,
            channelState = fixture.client.channelState("messaging:general"),
            scope = backgroundScope,
        )

        fixture.client.connectUser(User(id = "alice", name = "Alice"), token = "token")
        fixture.socket.mockConnected(
            ConnectedEvent(
                type = "health.check",
                createdAt = "2026-05-31T10:00:00Z",
                rawCreatedAt = "2026-05-31T10:00:00Z",
                me = User(id = "alice", name = "Alice"),
                connectionId = "connection-1",
            ),
        )
        fixture.client.queryChannel("messaging", "general", QueryChannelRequest())
        runCurrent()
        fixture.client.queryChannel("messaging", "general", QueryChannelRequest())
        runCurrent()

        assertEquals(listOf("message-1"), fixture.api.markReadMessageIds)
        controller.onCleared()
    }

    private fun fixture(scope: CoroutineScope): Fixture {
        val networkStateProvider = object : NetworkStateProvider {
            override fun isConnected(): Boolean = true
        }
        val socket = FakeChatSocket(scope)
        val api = FakeChatApi()
        val client = ChatClient(
            apiKey = "test-api-key",
            wssUrl = "wss://example.com/ws",
            networkStateProvider = networkStateProvider,
            clientScope = scope,
            chatApi = api,
            tokenProvider = TokenProvider(),
            chatSocket = socket,
        )
        return Fixture(client = client, socket = socket, api = api)
    }

    private data class Fixture(
        val client: ChatClient,
        val socket: FakeChatSocket,
        val api: FakeChatApi,
    )

    private class FakeChatApi : ChatApi {
        val markReadMessageIds = mutableListOf<String>()

        override suspend fun queryChannels(request: QueryChannelsRequest): List<Channel> = emptyList()

        override suspend fun markRead(
            channelType: String,
            channelId: String,
            messageId: String,
        ) {
            markReadMessageIds += messageId
        }

        override suspend fun sendMessage(
            channelType: String,
            channelId: String,
            request: SendMessageRequest,
        ): Message {
            return Message(
                id = "sent-1",
                cid = "$channelType:$channelId",
                text = request.text,
                clientMessageId = request.clientMessageId,
                user = User(id = "alice", name = "Alice"),
            )
        }

        override suspend fun queryChannel(
            channelType: String,
            channelId: String,
            query: QueryChannelRequest,
        ): Channel {
            return Channel(
                id = channelId,
                type = channelType,
                messages = listOf(
                    Message(
                        id = "message-1",
                        cid = "$channelType:$channelId",
                        text = "hello",
                        createdAt = "2026-05-31T10:00:00Z",
                        user = User(id = "bob", name = "Bob"),
                    ),
                ),
                members = listOf(
                    User(id = "alice", name = "Alice"),
                    User(id = "bob", name = "Bob"),
                ),
                unreadCount = 1,
            )
        }
    }
}
