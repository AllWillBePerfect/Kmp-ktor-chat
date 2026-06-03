package org.example.project.v2.client.internal.state.plugin.logic.channel.internal

import org.example.project.v2.client.api.state.StateRegistry
import org.example.project.v2.client.events.ChannelUpdatedByUserEvent
import org.example.project.v2.client.events.ChannelUpdatedEvent
import org.example.project.v2.client.events.ChatEvent
import org.example.project.v2.client.events.MemberAddedEvent
import org.example.project.v2.client.events.MemberRemovedEvent
import org.example.project.v2.client.events.MemberUpdatedEvent
import org.example.project.v2.client.events.MessageDeletedEvent
import org.example.project.v2.client.events.MessageReadEvent
import org.example.project.v2.client.events.MessageUpdatedEvent
import org.example.project.v2.client.events.NewMessageEvent
import org.example.project.v2.client.events.NotificationMessageNewEvent
import org.example.project.v2.client.events.TypingStartEvent
import org.example.project.v2.client.events.TypingStopEvent
import org.example.project.v2.core.models.Channel
import org.example.project.v2.platform.taggedLogger

internal class ChannelEventHandlerImpl(
    private val cid: String,
    private val stateRegistry: StateRegistry,
    private val messageStateUpdater: ChannelMessageStateUpdater,
    private val onChannelStateUpdated: (String) -> Unit,
) : ChannelEventHandler {
    private val logger by taggedLogger("Chat:V2ChannelEventHandler")

    override fun handle(event: ChatEvent) {
        when (event) {
            is NewMessageEvent -> if (event.cid == cid) messageStateUpdater.applyNewMessage(event.message)
            is NotificationMessageNewEvent -> if (event.cid == cid) {
                messageStateUpdater.applyNewMessage(event.message)
                stateRegistry.setUnreadCount(cid, event.channel.unreadCount)
                onChannelStateUpdated(cid)
            }
            is MessageUpdatedEvent -> if (event.cid == cid) messageStateUpdater.applyUpdatedMessage(event.message)
            is MessageDeletedEvent -> if (event.cid == cid) messageStateUpdater.applyDeletedMessage(event.message.id)
            is ChannelUpdatedEvent -> if (event.cid == cid) syncChannel(event.channel)
            is ChannelUpdatedByUserEvent -> if (event.cid == cid) syncChannel(event.channel)
            is MemberAddedEvent -> if (event.cid == cid) stateRegistry.upsertMember(cid, event.member.user)
            is MemberUpdatedEvent -> if (event.cid == cid) stateRegistry.upsertMember(cid, event.member.user)
            is MemberRemovedEvent -> if (event.cid == cid) stateRegistry.removeMember(cid, event.member.user.id)
            is TypingStartEvent -> if (event.cid == cid) stateRegistry.startTyping(cid, event.user)
            is TypingStopEvent -> if (event.cid == cid) stateRegistry.stopTyping(cid, event.user.id)
            is MessageReadEvent -> if (event.cid == cid) {
                logger.d { "[handle] message.read cid=$cid user=${event.user.id} lastRead=${event.lastReadMessageId}" }
                stateRegistry.setRead(cid, event.user.id, event.lastReadMessageId)
                onChannelStateUpdated(cid)
            }
            else -> Unit
        }
    }

    override fun syncChannel(channel: Channel) {
        if (channel.messages.isNotEmpty()) {
            messageStateUpdater.applySnapshot(channel)
        }
        stateRegistry.upsertChannel(channel)
        onChannelStateUpdated(cid)
    }
}
