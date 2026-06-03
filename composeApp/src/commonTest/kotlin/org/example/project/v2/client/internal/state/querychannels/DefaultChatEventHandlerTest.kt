package org.example.project.v2.client.internal.state.querychannels

import kotlinx.coroutines.flow.MutableStateFlow
import org.example.project.v2.client.api.event.DefaultChatEventHandler
import org.example.project.v2.client.api.event.EventHandlingResult
import org.example.project.v2.client.events.MemberAddedEvent
import org.example.project.v2.client.events.MemberRemovedEvent
import org.example.project.v2.client.events.NewMessageEvent
import org.example.project.v2.client.events.NotificationAddedToChannelEvent
import org.example.project.v2.client.events.NotificationMessageNewEvent
import org.example.project.v2.client.events.NotificationRemovedFromChannelEvent
import org.example.project.v2.client.network.NetworkStateProvider
import org.example.project.v2.client.setup.state.ClientState
import org.example.project.v2.client.setup.state.internal.MutableClientState
import org.example.project.v2.core.models.Channel
import org.example.project.v2.core.models.Member
import org.example.project.v2.core.models.NeutralFilterObject
import org.example.project.v2.core.models.Message
import org.example.project.v2.core.models.User
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultChatEventHandlerTest {

    @Test
    fun givenChannelPresent_whenMemberRemovedForCurrentUser_shouldRemoveChannel() {
        val channel = channel()
        val currentUser = user("alice")
        val clientState = clientState(currentUser)
        val eventHandler = DefaultChatEventHandler(MutableStateFlow(mapOf(channel.cid to channel)), clientState)
        val event = memberRemovedEvent(channel = channel, actor = currentUser, member = member(currentUser))

        val result = eventHandler.handleChatEvent(event = event, filter = NeutralFilterObject, cachedChannel = null)

        assertEquals(EventHandlingResult.Remove(channel.cid), result)
    }

    @Test
    fun givenChannelPresent_whenMemberRemovedForDifferentUser_shouldSkip() {
        val channel = channel()
        val currentUser = user("alice")
        val clientState = clientState(currentUser)
        val eventHandler = DefaultChatEventHandler(MutableStateFlow(mapOf(channel.cid to channel)), clientState)
        val event = memberRemovedEvent(channel = channel, actor = currentUser, member = member(user("bob")))

        val result = eventHandler.handleChatEvent(event = event, filter = NeutralFilterObject, cachedChannel = null)

        assertEquals(EventHandlingResult.Skip, result)
    }

    @Test
    fun givenChannelAbsent_whenMemberAddedForCurrentUser_shouldAddChannel() {
        val channel = channel()
        val currentUser = user("alice")
        val clientState = clientState(currentUser)
        val eventHandler = DefaultChatEventHandler(MutableStateFlow(emptyMap()), clientState)
        val event = memberAddedEvent(channel = channel, actor = currentUser, member = member(currentUser))

        val result = eventHandler.handleChatEvent(event = event, filter = NeutralFilterObject, cachedChannel = channel)

        assertEquals(EventHandlingResult.Add(channel), result)
    }

    @Test
    fun whenReceivedNotificationMessageNew_shouldWatchAndAdd() {
        val channel = channel()
        val eventHandler = DefaultChatEventHandler(MutableStateFlow(emptyMap()), clientState(null))
        val event = notificationMessageNewEvent(channel)

        val result = eventHandler.handleChatEvent(event = event, filter = NeutralFilterObject, cachedChannel = null)

        assertEquals(EventHandlingResult.WatchAndAdd(channel.cid), result)
    }

    @Test
    fun whenReceivedNotificationAddedToChannel_shouldWatchAndAdd() {
        val channel = channel()
        val eventHandler = DefaultChatEventHandler(MutableStateFlow(emptyMap()), clientState(null))
        val event = notificationAddedToChannelEvent(channel, member(user("alice")))

        val result = eventHandler.handleChatEvent(event = event, filter = NeutralFilterObject, cachedChannel = null)

        assertEquals(EventHandlingResult.WatchAndAdd(channel.cid), result)
    }

    @Test
    fun givenChannelPresent_whenNotificationRemovedFromChannelForCurrentUser_shouldRemoveChannel() {
        val channel = channel()
        val currentUser = user("alice")
        val eventHandler = DefaultChatEventHandler(
            MutableStateFlow(mapOf(channel.cid to channel)),
            clientState(currentUser),
        )
        val event = notificationRemovedFromChannelEvent(channel, member(currentUser))

        val result = eventHandler.handleChatEvent(event = event, filter = NeutralFilterObject, cachedChannel = null)

        assertEquals(EventHandlingResult.Remove(channel.cid), result)
    }

    @Test
    fun givenChannelAbsent_whenNewMessageWithRegularType_shouldAddChannel() {
        val channel = channel()
        val eventHandler = DefaultChatEventHandler(MutableStateFlow(emptyMap()), clientState(null))
        val event = newMessageEvent(channel, type = Message.TYPE_REGULAR)

        val result = eventHandler.handleChatEvent(event = event, filter = NeutralFilterObject, cachedChannel = channel)

        assertEquals(EventHandlingResult.Add(channel), result)
    }

    @Test
    fun givenChannelAbsent_whenNewMessageWithSystemType_shouldSkip() {
        val channel = channel()
        val eventHandler = DefaultChatEventHandler(MutableStateFlow(emptyMap()), clientState(null))
        val event = newMessageEvent(channel, type = "system")

        val result = eventHandler.handleChatEvent(event = event, filter = NeutralFilterObject, cachedChannel = channel)

        assertEquals(EventHandlingResult.Skip, result)
    }

    @Test
    fun givenChannelPresent_whenNewMessageWithRegularType_shouldSkip() {
        val channel = channel()
        val eventHandler = DefaultChatEventHandler(MutableStateFlow(mapOf(channel.cid to channel)), clientState(null))
        val event = newMessageEvent(channel, type = Message.TYPE_REGULAR)

        val result = eventHandler.handleChatEvent(event = event, filter = NeutralFilterObject, cachedChannel = channel)

        assertEquals(EventHandlingResult.Skip, result)
    }

    private fun clientState(currentUser: User?): ClientState {
        return MutableClientState(
            networkStateProvider = object : NetworkStateProvider {
                override fun isConnected(): Boolean = true
            },
        ).apply {
            if (currentUser != null) {
                setUser(currentUser)
            }
        }
    }

    private fun channel(id: String = "ch1", type: String = "messaging"): Channel =
        Channel(id = id, type = type, name = "Channel", members = listOf(user("alice")))

    private fun user(id: String): User = User(id = id, name = id.replaceFirstChar(Char::uppercase))

    private fun member(user: User): Member = Member(user = user)

    private fun memberAddedEvent(channel: Channel, actor: User, member: Member): MemberAddedEvent =
        MemberAddedEvent(
            type = "member.added",
            createdAt = null,
            rawCreatedAt = null,
            user = actor,
            cid = channel.cid,
            channelType = channel.type,
            channelId = channel.id,
            member = member,
        )

    private fun memberRemovedEvent(channel: Channel, actor: User, member: Member): MemberRemovedEvent =
        MemberRemovedEvent(
            type = "member.removed",
            createdAt = null,
            rawCreatedAt = null,
            user = actor,
            cid = channel.cid,
            channelType = channel.type,
            channelId = channel.id,
            member = member,
        )

    private fun notificationMessageNewEvent(channel: Channel): NotificationMessageNewEvent =
        NotificationMessageNewEvent(
            type = "notification.message_new",
            createdAt = null,
            rawCreatedAt = null,
            cid = channel.cid,
            channelType = channel.type,
            channelId = channel.id,
            channel = channel,
            message = Message(
                id = "m-notification",
                cid = channel.cid,
                text = "hello",
                createdAt = "2026-05-30T10:00:00Z",
                user = user("alice"),
            ),
        )

    private fun notificationAddedToChannelEvent(channel: Channel, member: Member): NotificationAddedToChannelEvent =
        NotificationAddedToChannelEvent(
            type = "notification.added_to_channel",
            createdAt = null,
            rawCreatedAt = null,
            cid = channel.cid,
            channelType = channel.type,
            channelId = channel.id,
            channel = channel,
            member = member,
        )

    private fun notificationRemovedFromChannelEvent(channel: Channel, member: Member): NotificationRemovedFromChannelEvent =
        NotificationRemovedFromChannelEvent(
            type = "notification.removed_from_channel",
            createdAt = null,
            rawCreatedAt = null,
            cid = channel.cid,
            channelType = channel.type,
            channelId = channel.id,
            channel = channel,
            member = member,
        )

    private fun newMessageEvent(channel: Channel, type: String): NewMessageEvent {
        val user = user("alice")
        return NewMessageEvent(
            type = "message.new",
            createdAt = null,
            rawCreatedAt = null,
            user = user,
            cid = channel.cid,
            channelType = channel.type,
            channelId = channel.id,
            message = Message(
                id = "m1",
                cid = channel.cid,
                text = "hello",
                type = type,
                createdAt = "2026-05-30T10:00:00Z",
                user = user,
            ),
        )
    }
}
