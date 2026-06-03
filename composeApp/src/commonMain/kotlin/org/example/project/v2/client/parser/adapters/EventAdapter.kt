package org.example.project.v2.client.parser.adapters

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.example.project.v2.client.events.ChannelDeletedEvent
import org.example.project.v2.client.events.ChannelUpdatedByUserEvent
import org.example.project.v2.client.events.ChannelUpdatedEvent
import org.example.project.v2.client.events.ChatEvent
import org.example.project.v2.client.events.ConnectedEvent
import org.example.project.v2.client.events.ConnectionErrorEvent
import org.example.project.v2.client.events.ConnectingEvent
import org.example.project.v2.client.events.DisconnectedEvent
import org.example.project.v2.client.events.HealthEvent
import org.example.project.v2.client.events.MessageDeletedEvent
import org.example.project.v2.client.events.MessageReadEvent
import org.example.project.v2.client.events.MessageUpdatedEvent
import org.example.project.v2.client.events.MemberAddedEvent
import org.example.project.v2.client.events.MemberRemovedEvent
import org.example.project.v2.client.events.MemberUpdatedEvent
import org.example.project.v2.client.events.NewMessageEvent
import org.example.project.v2.client.events.NotificationAddedToChannelEvent
import org.example.project.v2.client.events.NotificationMessageNewEvent
import org.example.project.v2.client.events.NotificationRemovedFromChannelEvent
import org.example.project.v2.client.events.TypingStartEvent
import org.example.project.v2.client.events.TypingStopEvent
import org.example.project.v2.client.events.UnknownEvent
import org.example.project.v2.core.models.Channel
import org.example.project.v2.core.models.Member
import org.example.project.v2.core.models.Message
import org.example.project.v2.core.models.User

internal class EventAdapter(
    private val json: Json,
) {
    fun fromJson(raw: String): ChatEvent {
        val root = json.parseToJsonElement(raw).jsonObject
        val type = root.string("type")
            ?: return UnknownEvent(type = "unknown", rawPayload = raw)

        return when (type) {
            "health.check", "connected" -> {
                val createdAt = root.string("createdAt") ?: root.string("created_at")
                val rawCreatedAt = root.string("rawCreatedAt") ?: root.string("created_at")
                val connectionId = root.string("connectionId") ?: root.string("connection_id").orEmpty()
                val me = root.objectValue("me")?.toUser()

                if (me != null) {
                    ConnectedEvent(
                        type = type,
                        createdAt = createdAt,
                        rawCreatedAt = rawCreatedAt,
                        me = me,
                        connectionId = connectionId,
                    )
                } else {
                    HealthEvent(
                        type = type,
                        createdAt = createdAt,
                        rawCreatedAt = rawCreatedAt,
                        connectionId = connectionId,
                    )
                }
            }
            "connection.error", "connection_error" -> ConnectionErrorEvent(
                type = type,
                createdAt = root.string("createdAt") ?: root.string("created_at"),
                rawCreatedAt = root.string("rawCreatedAt") ?: root.string("created_at"),
                connectionId = root.string("connectionId") ?: root.string("connection_id"),
                errorMessage = root.string("errorMessage") ?: root.string("message").orEmpty(),
            )
            "connecting" -> ConnectingEvent(
                type = type,
                createdAt = root.string("createdAt"),
                rawCreatedAt = root.string("rawCreatedAt"),
            )
            "disconnected" -> DisconnectedEvent(
                type = type,
                createdAt = root.string("createdAt"),
                rawCreatedAt = root.string("rawCreatedAt"),
                disconnectCause = root.string("disconnectCause"),
            )
            "message.new" -> NewMessageEvent(
                type = type,
                createdAt = root.string("createdAt") ?: root.string("created_at"),
                rawCreatedAt = root.string("rawCreatedAt") ?: root.string("created_at"),
                user = root.objectValue("user")?.toUser() ?: User(),
                cid = root.string("cid").orEmpty(),
                channelType = root.string("channelType") ?: root.string("channel_type").orEmpty(),
                channelId = root.string("channelId") ?: root.string("channel_id").orEmpty(),
                message = root.objectValue("message")?.toMessage() ?: Message(),
                totalUnreadCount = root.int("totalUnreadCount")
                    ?: root.int("total_unread_count")
                    ?: root.int("unreadCount")
                    ?: 0,
                unreadChannels = root.int("unreadChannels")
                    ?: root.int("unread_channels")
                    ?: root.int("unreadConversations")
                    ?: 0,
            )
            "message.deleted" -> MessageDeletedEvent(
                type = type,
                createdAt = root.string("createdAt") ?: root.string("created_at"),
                rawCreatedAt = root.string("rawCreatedAt") ?: root.string("created_at"),
                cid = root.string("cid").orEmpty(),
                channelType = root.string("channelType") ?: root.string("channel_type").orEmpty(),
                channelId = root.string("channelId") ?: root.string("channel_id").orEmpty(),
                message = root.objectValue("message")?.toMessage() ?: Message(),
                user = root.objectValue("user")?.toUser(),
                hardDelete = root.boolean("hardDelete") ?: false,
            )
            "message.updated" -> MessageUpdatedEvent(
                type = type,
                createdAt = root.string("createdAt") ?: root.string("created_at"),
                rawCreatedAt = root.string("rawCreatedAt") ?: root.string("created_at"),
                user = root.objectValue("user")?.toUser() ?: User(),
                cid = root.string("cid").orEmpty(),
                channelType = root.string("channelType") ?: root.string("channel_type").orEmpty(),
                channelId = root.string("channelId") ?: root.string("channel_id").orEmpty(),
                message = root.objectValue("message")?.toMessage() ?: Message(),
            )
            "member.added" -> MemberAddedEvent(
                type = type,
                createdAt = root.string("createdAt") ?: root.string("created_at"),
                rawCreatedAt = root.string("rawCreatedAt") ?: root.string("created_at"),
                user = root.objectValue("user")?.toUser() ?: User(),
                cid = root.string("cid").orEmpty(),
                channelType = root.string("channelType") ?: root.string("channel_type").orEmpty(),
                channelId = root.string("channelId") ?: root.string("channel_id").orEmpty(),
                member = root.objectValue("member")?.toMember() ?: root.objectValue("user")?.let { memberUser ->
                    Member(memberUser.toUser())
                } ?: Member(User()),
            )
            "member.removed" -> MemberRemovedEvent(
                type = type,
                createdAt = root.string("createdAt") ?: root.string("created_at"),
                rawCreatedAt = root.string("rawCreatedAt") ?: root.string("created_at"),
                user = root.objectValue("user")?.toUser() ?: User(),
                cid = root.string("cid").orEmpty(),
                channelType = root.string("channelType") ?: root.string("channel_type").orEmpty(),
                channelId = root.string("channelId") ?: root.string("channel_id").orEmpty(),
                member = root.objectValue("member")?.toMember() ?: root.objectValue("user")?.let { memberUser ->
                    Member(memberUser.toUser())
                } ?: Member(User()),
            )
            "member.updated" -> MemberUpdatedEvent(
                type = type,
                createdAt = root.string("createdAt") ?: root.string("created_at"),
                rawCreatedAt = root.string("rawCreatedAt") ?: root.string("created_at"),
                user = root.objectValue("user")?.toUser() ?: User(),
                cid = root.string("cid").orEmpty(),
                channelType = root.string("channelType") ?: root.string("channel_type").orEmpty(),
                channelId = root.string("channelId") ?: root.string("channel_id").orEmpty(),
                member = root.objectValue("member")?.toMember() ?: root.objectValue("user")?.let { memberUser ->
                    Member(memberUser.toUser())
                } ?: Member(User()),
            )
            "channel.deleted" -> ChannelDeletedEvent(
                type = type,
                createdAt = root.string("createdAt") ?: root.string("created_at"),
                rawCreatedAt = root.string("rawCreatedAt") ?: root.string("created_at"),
                cid = root.string("cid").orEmpty(),
                channelType = root.string("channelType") ?: root.string("channel_type").orEmpty(),
                channelId = root.string("channelId") ?: root.string("channel_id").orEmpty(),
                channel = root.objectValue("channel")?.toChannel() ?: Channel(),
                user = root.objectValue("user")?.toUser(),
            )
            "channel.updated" -> {
                val user = root.objectValue("user")?.toUser()
                if (user != null) {
                    ChannelUpdatedByUserEvent(
                        type = type,
                        createdAt = root.string("createdAt") ?: root.string("created_at"),
                        rawCreatedAt = root.string("rawCreatedAt") ?: root.string("created_at"),
                        cid = root.string("cid").orEmpty(),
                        channelType = root.string("channelType") ?: root.string("channel_type").orEmpty(),
                        channelId = root.string("channelId") ?: root.string("channel_id").orEmpty(),
                        user = user,
                        channel = root.objectValue("channel")?.toChannel() ?: Channel(),
                        message = root.objectValue("message")?.toMessage(),
                    )
                } else {
                    ChannelUpdatedEvent(
                        type = type,
                        createdAt = root.string("createdAt") ?: root.string("created_at"),
                        rawCreatedAt = root.string("rawCreatedAt") ?: root.string("created_at"),
                        cid = root.string("cid").orEmpty(),
                        channelType = root.string("channelType") ?: root.string("channel_type").orEmpty(),
                        channelId = root.string("channelId") ?: root.string("channel_id").orEmpty(),
                        channel = root.objectValue("channel")?.toChannel() ?: Channel(),
                        message = root.objectValue("message")?.toMessage(),
                    )
                }
            }
            "notification.message_new" -> NotificationMessageNewEvent(
                type = type,
                createdAt = root.string("createdAt") ?: root.string("created_at"),
                rawCreatedAt = root.string("rawCreatedAt") ?: root.string("created_at"),
                cid = root.string("cid").orEmpty(),
                channelType = root.string("channelType") ?: root.string("channel_type").orEmpty(),
                channelId = root.string("channelId") ?: root.string("channel_id").orEmpty(),
                channel = root.objectValue("channel")?.toChannel() ?: Channel(),
                message = root.objectValue("message")?.toMessage() ?: Message(),
                totalUnreadCount = root.int("totalUnreadCount")
                    ?: root.int("total_unread_count")
                    ?: root.int("unreadCount")
                    ?: 0,
                unreadChannels = root.int("unreadChannels")
                    ?: root.int("unread_channels")
                    ?: root.int("unreadConversations")
                    ?: 0,
            )
            "notification.added_to_channel" -> NotificationAddedToChannelEvent(
                type = type,
                createdAt = root.string("createdAt") ?: root.string("created_at"),
                rawCreatedAt = root.string("rawCreatedAt") ?: root.string("created_at"),
                cid = root.string("cid").orEmpty(),
                channelType = root.string("channelType") ?: root.string("channel_type").orEmpty(),
                channelId = root.string("channelId") ?: root.string("channel_id").orEmpty(),
                channel = root.objectValue("channel")?.toChannel() ?: Channel(),
                member = root.objectValue("member")?.toMember() ?: Member(User()),
                totalUnreadCount = root.int("totalUnreadCount")
                    ?: root.int("total_unread_count")
                    ?: root.int("unreadCount")
                    ?: 0,
                unreadChannels = root.int("unreadChannels")
                    ?: root.int("unread_channels")
                    ?: root.int("unreadConversations")
                    ?: 0,
            )
            "notification.removed_from_channel" -> NotificationRemovedFromChannelEvent(
                type = type,
                createdAt = root.string("createdAt") ?: root.string("created_at"),
                rawCreatedAt = root.string("rawCreatedAt") ?: root.string("created_at"),
                cid = root.string("cid").orEmpty(),
                channelType = root.string("channelType") ?: root.string("channel_type").orEmpty(),
                channelId = root.string("channelId") ?: root.string("channel_id").orEmpty(),
                channel = root.objectValue("channel")?.toChannel() ?: Channel(),
                member = root.objectValue("member")?.toMember() ?: Member(User()),
            )
            "message.read" -> MessageReadEvent(
                type = type,
                createdAt = root.string("createdAt") ?: root.string("created_at"),
                rawCreatedAt = root.string("rawCreatedAt") ?: root.string("created_at"),
                user = root.objectValue("user")?.toUser() ?: User(),
                cid = root.string("cid").orEmpty(),
                channelType = root.string("channelType") ?: root.string("channel_type").orEmpty(),
                channelId = root.string("channelId") ?: root.string("channel_id").orEmpty(),
                lastReadMessageId = root.string("lastReadMessageId") ?: root.string("last_read_message_id"),
            )
            "typing.start" -> TypingStartEvent(
                type = type,
                createdAt = root.string("createdAt") ?: root.string("created_at"),
                rawCreatedAt = root.string("rawCreatedAt") ?: root.string("created_at"),
                user = root.objectValue("user")?.toUser() ?: User(),
                cid = root.string("cid").orEmpty(),
                channelType = root.string("channelType") ?: root.string("channel_type").orEmpty(),
                channelId = root.string("channelId") ?: root.string("channel_id").orEmpty(),
                parentId = root.string("parentId") ?: root.string("parent_id"),
            )
            "typing.stop" -> TypingStopEvent(
                type = type,
                createdAt = root.string("createdAt") ?: root.string("created_at"),
                rawCreatedAt = root.string("rawCreatedAt") ?: root.string("created_at"),
                user = root.objectValue("user")?.toUser() ?: User(),
                cid = root.string("cid").orEmpty(),
                channelType = root.string("channelType") ?: root.string("channel_type").orEmpty(),
                channelId = root.string("channelId") ?: root.string("channel_id").orEmpty(),
                parentId = root.string("parentId") ?: root.string("parent_id"),
            )
            else -> UnknownEvent(
                type = type,
                createdAt = root.string("createdAt"),
                rawCreatedAt = root.string("rawCreatedAt"),
                rawPayload = raw,
            )
        }
    }
}
