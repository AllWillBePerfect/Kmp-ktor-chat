package org.example.project.v2.client.api.event

import kotlinx.coroutines.flow.StateFlow
import org.example.project.v2.client.events.CidEvent
import org.example.project.v2.client.events.HasChannel
import org.example.project.v2.client.events.MemberAddedEvent
import org.example.project.v2.client.events.MemberRemovedEvent
import org.example.project.v2.client.events.MemberUpdatedEvent
import org.example.project.v2.client.events.NewMessageEvent
import org.example.project.v2.client.events.NotificationAddedToChannelEvent
import org.example.project.v2.client.events.NotificationMessageNewEvent
import org.example.project.v2.client.events.NotificationRemovedFromChannelEvent
import org.example.project.v2.client.setup.state.ClientState
import org.example.project.v2.core.models.Channel
import org.example.project.v2.core.models.FilterObject

open class DefaultChatEventHandler(
    protected val channels: StateFlow<Map<String, Channel>?>,
    protected val clientState: ClientState,
) : BaseChatEventHandler() {

    override fun handleChannelEvent(event: HasChannel, filter: FilterObject): EventHandlingResult {
        return when (event) {
            is NotificationMessageNewEvent -> EventHandlingResult.WatchAndAdd(event.cid)
            is NotificationAddedToChannelEvent -> EventHandlingResult.WatchAndAdd(event.cid)
            is NotificationRemovedFromChannelEvent -> removeIfCurrentUserLeftChannel(event.cid, event.member.user.id)
            else -> super.handleChannelEvent(event, filter)
        }
    }

    override fun handleCidEvent(
        event: CidEvent,
        filter: FilterObject,
        cachedChannel: Channel?,
    ): EventHandlingResult {
        return when (event) {
            is NewMessageEvent -> handleNewMessageEvent(event, cachedChannel)
            is MemberRemovedEvent -> removeIfCurrentUserLeftChannel(event.cid, event.member.user.id)
            is MemberAddedEvent -> addIfCurrentUserJoinedChannel(cachedChannel, event.member.user.id)
            is MemberUpdatedEvent -> addIfMembershipUpdated(cachedChannel, event.member.user.id)
            else -> super.handleCidEvent(event, filter, cachedChannel)
        }
    }

    private fun handleNewMessageEvent(
        event: NewMessageEvent,
        cachedChannel: Channel?,
    ): EventHandlingResult {
        return if (event.message.type == SYSTEM_MESSAGE) {
            EventHandlingResult.Skip
        } else {
            addIfChannelIsAbsent(cachedChannel)
        }
    }

    protected fun addIfChannelIsAbsent(channel: Channel?): EventHandlingResult {
        val channelsMap = channels.value

        return when {
            channelsMap == null || channel == null -> EventHandlingResult.Skip
            channelsMap.containsKey(channel.cid) -> EventHandlingResult.Skip
            else -> EventHandlingResult.Add(channel)
        }
    }

    private fun addIfMembershipUpdated(channel: Channel?, memberUserId: String): EventHandlingResult {
        return if (channel?.members?.any { it.id == memberUserId } == true) {
            EventHandlingResult.Add(channel)
        } else {
            EventHandlingResult.Skip
        }
    }

    protected fun removeIfChannelExists(cid: String): EventHandlingResult {
        val channelsMap = channels.value
        return when {
            channelsMap == null -> EventHandlingResult.Skip
            channelsMap.containsKey(cid) -> EventHandlingResult.Remove(cid)
            else -> EventHandlingResult.Skip
        }
    }

    protected fun addIfCurrentUserJoinedChannel(channel: Channel?, memberUserId: String): EventHandlingResult {
        return if (clientState.user.value?.id == memberUserId) {
            addIfChannelIsAbsent(channel)
        } else {
            EventHandlingResult.Skip
        }
    }

    private fun removeIfCurrentUserLeftChannel(cid: String, memberUserId: String): EventHandlingResult {
        return if (memberUserId != clientState.user.value?.id) {
            EventHandlingResult.Skip
        } else {
            removeIfChannelExists(cid)
        }
    }

    private companion object {
        const val SYSTEM_MESSAGE = "system"
    }
}
