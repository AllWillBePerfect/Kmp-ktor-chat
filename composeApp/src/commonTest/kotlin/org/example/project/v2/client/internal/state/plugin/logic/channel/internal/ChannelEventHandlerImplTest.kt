package org.example.project.v2.client.internal.state.plugin.logic.channel.internal

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.example.project.v2.client.api.state.StateRegistry
import org.example.project.v2.client.events.MessageReadEvent
import org.example.project.v2.client.events.NotificationMessageNewEvent
import org.example.project.v2.client.events.TypingStartEvent
import org.example.project.v2.core.models.Channel
import org.example.project.v2.core.models.Message
import org.example.project.v2.core.models.SyncStatus
import org.example.project.v2.core.models.User
import kotlin.test.Test
import kotlin.test.assertEquals

class ChannelEventHandlerImplTest {

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun handleNotificationMessageNewUpdatesMessagesAndUnread() = runTest {
        val stateRegistry = StateRegistry(scope = backgroundScope, currentUserId = { "alice" })
        val handler = ChannelEventHandlerImpl(
            cid = "messaging:general",
            stateRegistry = stateRegistry,
            messageStateUpdater = ChannelMessageStateUpdater(
                cid = "messaging:general",
                stateRegistry = stateRegistry,
                currentUserIdProvider = { "alice" },
            ),
            onChannelUpdated = {},
        )
        stateRegistry.upsertChannel(Channel(id = "general", type = "messaging"))

        handler.handle(
            NotificationMessageNewEvent(
                type = "notification.message_new",
                createdAt = "2026-05-31T10:00:00Z",
                rawCreatedAt = "2026-05-31T10:00:00Z",
                cid = "messaging:general",
                channelType = "messaging",
                channelId = "general",
                channel = Channel(id = "general", type = "messaging", unreadCount = 3),
                message = Message(
                    id = "message-1",
                    cid = "messaging:general",
                    text = "hello",
                    user = User(id = "bob", name = "Bob"),
                ),
                totalUnreadCount = 3,
                unreadChannels = 1,
            ),
        )
        runCurrent()

        assertEquals(1, stateRegistry.getMessages("messaging:general").size)
        assertEquals(3, (stateRegistry.channel("messaging:general") as org.example.project.v2.client.internal.state.plugin.state.channel.internal.ChannelStateImpl).unreadCount.value)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun handleTypingAndReadUpdatesChannelScopedState() = runTest {
        val stateRegistry = StateRegistry(scope = backgroundScope, currentUserId = { "alice" })
        val handler = ChannelEventHandlerImpl(
            cid = "messaging:general",
            stateRegistry = stateRegistry,
            messageStateUpdater = ChannelMessageStateUpdater(
                cid = "messaging:general",
                stateRegistry = stateRegistry,
                currentUserIdProvider = { "alice" },
            ),
            onChannelUpdated = {},
        )
        stateRegistry.upsertChannel(Channel(id = "general", type = "messaging"))

        handler.handle(
            TypingStartEvent(
                type = "typing.start",
                createdAt = "2026-05-31T10:00:01Z",
                rawCreatedAt = "2026-05-31T10:00:01Z",
                user = User(id = "bob", name = "Bob"),
                cid = "messaging:general",
                channelType = "messaging",
                channelId = "general",
                parentId = null,
            ),
        )
        handler.handle(
            MessageReadEvent(
                type = "message.read",
                createdAt = "2026-05-31T10:00:02Z",
                rawCreatedAt = "2026-05-31T10:00:02Z",
                user = User(id = "bob", name = "Bob"),
                cid = "messaging:general",
                channelType = "messaging",
                channelId = "general",
                lastReadMessageId = "message-1",
            ),
        )
        runCurrent()

        val state = stateRegistry.channel("messaging:general") as org.example.project.v2.client.internal.state.plugin.state.channel.internal.ChannelStateImpl
        assertEquals(listOf("bob"), state.typing.value.map { it.id })
        assertEquals("message-1", state.reads.value["bob"])
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun syncChannelKeepsLocalPendingMessagesUntilSnapshotContainsServerMessage() = runTest {
        val stateRegistry = StateRegistry(scope = backgroundScope, currentUserId = { "alice" })
        val updater = ChannelMessageStateUpdater(
            cid = "messaging:general",
            stateRegistry = stateRegistry,
            currentUserIdProvider = { "alice" },
        )
        val handler = ChannelEventHandlerImpl(
            cid = "messaging:general",
            stateRegistry = stateRegistry,
            messageStateUpdater = updater,
            onChannelUpdated = {},
        )
        updater.applyLocalSend(
            Message(
                id = "client-1",
                cid = "messaging:general",
                text = "pending",
                clientMessageId = "client-1",
                syncStatus = SyncStatus.IN_PROGRESS,
                createdAt = "2026-05-31T10:00:01Z",
                createdLocallyAt = "2026-05-31T10:00:01Z",
                user = User(id = "alice", name = "Alice"),
            ),
        )

        handler.syncChannel(
            Channel(
                id = "general",
                type = "messaging",
                messages = listOf(
                    Message(
                        id = "server-1",
                        cid = "messaging:general",
                        text = "existing",
                        createdAt = "2026-05-31T10:00:00Z",
                        user = User(id = "bob", name = "Bob"),
                    ),
                ),
            ),
        )
        runCurrent()

        val messagesAfterFirstSnapshot = stateRegistry.getMessages("messaging:general")
        assertEquals(listOf("existing", "pending"), messagesAfterFirstSnapshot.map { it.text })

        handler.syncChannel(
            Channel(
                id = "general",
                type = "messaging",
                messages = listOf(
                    Message(
                        id = "server-1",
                        cid = "messaging:general",
                        text = "existing",
                        createdAt = "2026-05-31T10:00:00Z",
                        user = User(id = "bob", name = "Bob"),
                    ),
                    Message(
                        id = "server-2",
                        cid = "messaging:general",
                        text = "pending",
                        clientMessageId = "client-1",
                        createdAt = "2026-05-31T10:00:02Z",
                        user = User(id = "alice", name = "Alice"),
                    ),
                ),
            ),
        )
        runCurrent()

        val messagesAfterAckSnapshot = stateRegistry.getMessages("messaging:general")
        assertEquals(listOf("existing", "pending"), messagesAfterAckSnapshot.map { it.text })
        assertEquals(listOf("server-1", "server-2"), messagesAfterAckSnapshot.map { it.id })
    }
}
