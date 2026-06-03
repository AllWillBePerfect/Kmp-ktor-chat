package org.example.project.v2.client.internal.state.plugin.logic.querychannels.internal

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.example.project.v2.client.api.state.StateRegistry
import org.example.project.v2.client.events.MessageReadEvent
import org.example.project.v2.client.events.NotificationMessageNewEvent
import org.example.project.v2.core.models.Channel
import org.example.project.v2.core.models.Message
import org.example.project.v2.core.models.NeutralFilterObject
import org.example.project.v2.core.models.User
import org.example.project.v2.client.internal.state.plugin.QueryChannelsIdentifier
import org.example.project.v2.client.internal.state.plugin.state.querychannels.internal.QueryChannelsMutableState
import kotlin.test.Test
import kotlin.test.assertEquals

class QueryChannelsEventHandlerImplTest {

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun handleNotificationMessageNewUpdatesQueryState() = runTest {
        val stateRegistry = StateRegistry(scope = backgroundScope)
        val mutableState = QueryChannelsMutableState(
            identifier = QueryChannelsIdentifier.Standard(NeutralFilterObject),
            filter = NeutralFilterObject,
            scope = backgroundScope,
        )
        val stateLogic = QueryChannelsStateLogic(mutableState)
        val handler = QueryChannelsEventHandlerImpl(
            stateRegistry = stateRegistry,
            queryChannelsStateLogic = stateLogic,
        )
        val existingChannel = Channel(id = "general", type = "messaging", name = "General")
        stateRegistry.upsertChannel(existingChannel)
        stateLogic.addChannels(listOf(existingChannel))

        val updatedChannel = existingChannel.copy(
            unreadCount = 2,
            lastMessageAt = "2026-05-31T10:00:00Z",
        )
        handler.handle(
            NotificationMessageNewEvent(
                type = "notification.message_new",
                createdAt = "2026-05-31T10:00:00Z",
                rawCreatedAt = "2026-05-31T10:00:00Z",
                cid = updatedChannel.cid,
                channelType = updatedChannel.type,
                channelId = updatedChannel.id,
                channel = updatedChannel,
                message = Message(
                    id = "message-1",
                    cid = updatedChannel.cid,
                    text = "hello",
                    user = User(id = "bob", name = "Bob"),
                ),
                totalUnreadCount = 2,
                unreadChannels = 1,
            ),
        )
        runCurrent()

        val channels = mutableState.channels.value.orEmpty()
        assertEquals(1, channels.size)
        assertEquals(2, channels.first().unreadCount)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun handleMessageReadUpdatesQueryStateFromRegistryChannel() = runTest {
        val stateRegistry = StateRegistry(scope = backgroundScope, currentUserId = { "alice" })
        val mutableState = QueryChannelsMutableState(
            identifier = QueryChannelsIdentifier.Standard(NeutralFilterObject),
            filter = NeutralFilterObject,
            scope = backgroundScope,
        )
        val stateLogic = QueryChannelsStateLogic(mutableState)
        val handler = QueryChannelsEventHandlerImpl(
            stateRegistry = stateRegistry,
            queryChannelsStateLogic = stateLogic,
        )
        val existingChannel = Channel(
            id = "general",
            type = "messaging",
            name = "General",
            unreadCount = 3,
            messages = listOf(
                Message(
                    id = "message-1",
                    cid = "messaging:general",
                    text = "hello",
                    user = User(id = "bob", name = "Bob"),
                ),
            ),
        )
        stateRegistry.upsertChannel(existingChannel)
        stateRegistry.setMessages(existingChannel.cid, existingChannel.messages)
        stateLogic.addChannels(listOf(existingChannel))

        stateRegistry.setRead(existingChannel.cid, "alice", "message-1")

        handler.handle(
            MessageReadEvent(
                type = "message.read",
                createdAt = "2026-05-31T10:00:00Z",
                rawCreatedAt = "2026-05-31T10:00:00Z",
                user = User(id = "alice", name = "Alice"),
                cid = existingChannel.cid,
                channelType = existingChannel.type,
                channelId = existingChannel.id,
                lastReadMessageId = "message-1",
            ),
        )
        runCurrent()

        val channels = mutableState.channels.value.orEmpty()
        assertEquals(1, channels.size)
        assertEquals(0, channels.first().unreadCount)
    }
}
