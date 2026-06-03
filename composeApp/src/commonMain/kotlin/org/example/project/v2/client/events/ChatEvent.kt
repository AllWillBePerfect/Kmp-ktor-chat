package org.example.project.v2.client.events

import java.util.concurrent.atomic.AtomicInteger
import org.example.project.v2.core.models.Channel
import org.example.project.v2.core.models.Member
import org.example.project.v2.core.models.Message
import org.example.project.v2.core.models.User

private val seqGenerator = AtomicInteger()

sealed class ChatEvent {
    abstract val type: String
    abstract val createdAt: String?
    abstract val rawCreatedAt: String?
    val seq: Int = seqGenerator.incrementAndGet()
}

sealed class CidEvent : ChatEvent() {
    abstract val cid: String
    abstract val channelType: String
    abstract val channelId: String
}

sealed interface HasConversationId {
    val conversationId: String
}

sealed interface HasConversation {
    val conversation: Channel
}

sealed interface HasChannel {
    val channel: Channel
}

sealed interface HasMessage {
    val message: Message
}

sealed interface HasMember {
    val member: Member
}

sealed interface HasUser {
    val user: User
}

sealed interface HasOwnUser {
    val me: User
}

sealed interface HasClientMessageId {
    val clientMessageId: String?
}

sealed interface HasUnreadCounts {
    val totalUnreadCount: Int
    val unreadChannels: Int
}

data class ConnectedEvent(
    override val type: String,
    override val createdAt: String?,
    override val rawCreatedAt: String?,
    override val me: User,
    val connectionId: String,
) : ChatEvent(), HasOwnUser

data class HealthEvent(
    override val type: String,
    override val createdAt: String?,
    override val rawCreatedAt: String?,
    val connectionId: String,
) : ChatEvent()

data class ConnectionErrorEvent(
    override val type: String,
    override val createdAt: String?,
    override val rawCreatedAt: String?,
    val connectionId: String?,
    val errorMessage: String,
) : ChatEvent()

data class ConnectingEvent(
    override val type: String,
    override val createdAt: String?,
    override val rawCreatedAt: String?,
) : ChatEvent()

data class DisconnectedEvent(
    override val type: String,
    override val createdAt: String?,
    override val rawCreatedAt: String?,
    val disconnectCause: String? = null,
) : ChatEvent()

data class NewMessageEvent(
    override val type: String,
    override val createdAt: String?,
    override val rawCreatedAt: String?,
    override val user: User,
    override val cid: String,
    override val channelType: String,
    override val channelId: String,
    override val message: Message,
    override val totalUnreadCount: Int = 0,
    override val unreadChannels: Int = 0,
) : CidEvent(), HasUser, HasMessage, HasUnreadCounts

data class MemberAddedEvent(
    override val type: String,
    override val createdAt: String?,
    override val rawCreatedAt: String?,
    override val user: User,
    override val cid: String,
    override val channelType: String,
    override val channelId: String,
    override val member: Member,
) : CidEvent(), HasUser, HasMember

data class MemberRemovedEvent(
    override val type: String,
    override val createdAt: String?,
    override val rawCreatedAt: String?,
    override val user: User,
    override val cid: String,
    override val channelType: String,
    override val channelId: String,
    override val member: Member,
) : CidEvent(), HasUser, HasMember

data class MemberUpdatedEvent(
    override val type: String,
    override val createdAt: String?,
    override val rawCreatedAt: String?,
    override val user: User,
    override val cid: String,
    override val channelType: String,
    override val channelId: String,
    override val member: Member,
) : CidEvent(), HasUser, HasMember

data class MessageDeletedEvent(
    override val type: String,
    override val createdAt: String?,
    override val rawCreatedAt: String?,
    override val cid: String,
    override val channelType: String,
    override val channelId: String,
    override val message: Message,
    val user: User? = null,
    val hardDelete: Boolean,
) : CidEvent(), HasMessage

data class MessageUpdatedEvent(
    override val type: String,
    override val createdAt: String?,
    override val rawCreatedAt: String?,
    override val user: User,
    override val cid: String,
    override val channelType: String,
    override val channelId: String,
    override val message: Message,
) : CidEvent(), HasUser, HasMessage

data class ChannelDeletedEvent(
    override val type: String,
    override val createdAt: String?,
    override val rawCreatedAt: String?,
    override val cid: String,
    override val channelType: String,
    override val channelId: String,
    override val channel: Channel,
    val user: User? = null,
) : CidEvent(), HasChannel

data class ChannelUpdatedEvent(
    override val type: String,
    override val createdAt: String?,
    override val rawCreatedAt: String?,
    override val cid: String,
    override val channelType: String,
    override val channelId: String,
    override val channel: Channel,
    val message: Message? = null,
) : CidEvent(), HasChannel

data class ChannelUpdatedByUserEvent(
    override val type: String,
    override val createdAt: String?,
    override val rawCreatedAt: String?,
    override val cid: String,
    override val channelType: String,
    override val channelId: String,
    override val user: User,
    override val channel: Channel,
    val message: Message? = null,
) : CidEvent(), HasUser, HasChannel

data class NotificationMessageNewEvent(
    override val type: String,
    override val createdAt: String?,
    override val rawCreatedAt: String?,
    override val cid: String,
    override val channelType: String,
    override val channelId: String,
    override val channel: Channel,
    override val message: Message,
    override val totalUnreadCount: Int = 0,
    override val unreadChannels: Int = 0,
) : CidEvent(), HasChannel, HasMessage, HasUnreadCounts

data class NotificationAddedToChannelEvent(
    override val type: String,
    override val createdAt: String?,
    override val rawCreatedAt: String?,
    override val cid: String,
    override val channelType: String,
    override val channelId: String,
    override val channel: Channel,
    override val member: Member,
    override val totalUnreadCount: Int = 0,
    override val unreadChannels: Int = 0,
) : CidEvent(), HasChannel, HasMember, HasUnreadCounts

data class NotificationRemovedFromChannelEvent(
    override val type: String,
    override val createdAt: String?,
    override val rawCreatedAt: String?,
    override val cid: String,
    override val channelType: String,
    override val channelId: String,
    override val channel: Channel,
    override val member: Member,
) : CidEvent(), HasChannel, HasMember

data class MessageReadEvent(
    override val type: String,
    override val createdAt: String?,
    override val rawCreatedAt: String?,
    override val user: User,
    override val cid: String,
    override val channelType: String,
    override val channelId: String,
    val lastReadMessageId: String?,
) : CidEvent(), HasUser

data class TypingStartEvent(
    override val type: String,
    override val createdAt: String?,
    override val rawCreatedAt: String?,
    override val user: User,
    override val cid: String,
    override val channelType: String,
    override val channelId: String,
    val parentId: String?,
) : CidEvent(), HasUser

data class TypingStopEvent(
    override val type: String,
    override val createdAt: String?,
    override val rawCreatedAt: String?,
    override val user: User,
    override val cid: String,
    override val channelType: String,
    override val channelId: String,
    val parentId: String?,
) : CidEvent(), HasUser

data class UnknownEvent(
    override val type: String,
    override val createdAt: String? = null,
    override val rawCreatedAt: String? = null,
    val rawPayload: String,
) : ChatEvent()
