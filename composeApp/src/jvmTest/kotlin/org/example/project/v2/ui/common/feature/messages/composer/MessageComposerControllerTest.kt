package org.example.project.v2.ui.common.feature.messages.composer

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
import org.example.project.v2.client.network.NetworkStateProvider
import org.example.project.v2.client.socket.FakeChatSocket
import org.example.project.v2.core.models.Channel
import org.example.project.v2.core.models.Message
import org.example.project.v2.core.models.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MessageComposerControllerTest {

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun setMessageInputUpdatesStateAndSendMessageClearsIt() = runTest {
        val fixture = fixture(backgroundScope)
        val controller = MessageComposerController(
            channelCid = "messaging:general",
            chatClient = fixture.client,
            scope = backgroundScope,
        )

        controller.setMessageInput("hello")
        assertEquals("hello", controller.state.value.inputValue)
        assertTrue(controller.state.value.canSendMessage)

        controller.sendMessage()
        runCurrent()

        assertEquals("", controller.state.value.inputValue)
        assertEquals(listOf("hello"), fixture.api.sentTexts)
    }

    @Test
    fun blankInputDoesNotSend() = runTest {
        val fixture = fixture(backgroundScope)
        val controller = MessageComposerController(
            channelCid = "messaging:general",
            chatClient = fixture.client,
            scope = backgroundScope,
        )

        controller.setMessageInput("   ")
        assertFalse(controller.state.value.canSendMessage)

        controller.sendMessage()

        assertTrue(fixture.api.sentTexts.isEmpty())
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
        return Fixture(client = client, api = api)
    }

    private data class Fixture(
        val client: ChatClient,
        val api: FakeChatApi,
    )

    private class FakeChatApi : ChatApi {
        val sentTexts = mutableListOf<String>()

        override suspend fun queryChannels(request: QueryChannelsRequest): List<Channel> = emptyList()

        override suspend fun markRead(
            channelType: String,
            channelId: String,
            messageId: String,
        ) = Unit

        override suspend fun sendMessage(
            channelType: String,
            channelId: String,
            request: SendMessageRequest,
        ): Message {
            sentTexts += request.text
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
        ): Channel = Channel(id = channelId, type = channelType)
    }
}
