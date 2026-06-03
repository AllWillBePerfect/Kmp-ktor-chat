package org.example.project.v2.client.parser

import org.example.project.v2.client.events.ChatEvent
import org.example.project.v2.client.events.ConnectedEvent
import org.example.project.v2.client.events.HealthEvent
import org.example.project.v2.client.events.MemberAddedEvent
import org.example.project.v2.client.events.MemberRemovedEvent
import org.example.project.v2.client.events.MemberUpdatedEvent
import org.example.project.v2.client.events.MessageReadEvent
import org.example.project.v2.client.events.NotificationAddedToChannelEvent
import org.example.project.v2.client.events.NotificationMessageNewEvent
import org.example.project.v2.client.events.NotificationRemovedFromChannelEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class KotlinxChatParserTest {

    private val parser = KotlinxChatParser()

    @Test
    fun parseConnectedHealthCheckEvent() {
        val event = parser.fromJson(connectedHealthCheckJson(), ChatEvent::class)

        val parsedEvent = assertIs<ConnectedEvent>(event)
        assertEquals("connection-1", parsedEvent.connectionId)
        assertEquals("alice", parsedEvent.me.id)
    }

    @Test
    fun parseHealthCheckEventWithoutMe() {
        val event = parser.fromJson(healthCheckJson(), ChatEvent::class)

        val parsedEvent = assertIs<HealthEvent>(event)
        assertEquals("connection-1", parsedEvent.connectionId)
    }

    @Test
    fun parseMemberAddedEvent() {
        val event = parser.fromJson(memberEventJson("member.added"), ChatEvent::class)

        val parsedEvent = assertIs<MemberAddedEvent>(event)
        assertEquals("messaging:general", parsedEvent.cid)
        assertEquals("alice", parsedEvent.user.id)
        assertEquals("alice", parsedEvent.member.user.id)
    }

    @Test
    fun parseMemberRemovedEvent() {
        val event = parser.fromJson(memberEventJson("member.removed"), ChatEvent::class)

        val parsedEvent = assertIs<MemberRemovedEvent>(event)
        assertEquals("messaging:general", parsedEvent.cid)
        assertEquals("alice", parsedEvent.user.id)
        assertEquals("alice", parsedEvent.member.user.id)
    }

    @Test
    fun parseMemberUpdatedEvent() {
        val event = parser.fromJson(memberEventJson("member.updated"), ChatEvent::class)

        val parsedEvent = assertIs<MemberUpdatedEvent>(event)
        assertEquals("messaging:general", parsedEvent.cid)
        assertEquals("alice", parsedEvent.user.id)
        assertEquals("alice", parsedEvent.member.user.id)
    }

    @Test
    fun parseNotificationMessageNewEvent() {
        val event = parser.fromJson(notificationMessageNewJson(), ChatEvent::class)

        val parsedEvent = assertIs<NotificationMessageNewEvent>(event)
        assertEquals("messaging:general", parsedEvent.cid)
        assertEquals("general", parsedEvent.channel.id)
        assertEquals("m1", parsedEvent.message.id)
        assertEquals(7, parsedEvent.totalUnreadCount)
        assertEquals(3, parsedEvent.unreadChannels)
    }

    @Test
    fun parseNotificationAddedToChannelEvent() {
        val event = parser.fromJson(notificationAddedToChannelJson(), ChatEvent::class)

        val parsedEvent = assertIs<NotificationAddedToChannelEvent>(event)
        assertEquals("messaging:general", parsedEvent.cid)
        assertEquals("general", parsedEvent.channel.id)
        assertEquals("alice", parsedEvent.member.user.id)
    }

    @Test
    fun parseNotificationRemovedFromChannelEvent() {
        val event = parser.fromJson(notificationRemovedFromChannelJson(), ChatEvent::class)

        val parsedEvent = assertIs<NotificationRemovedFromChannelEvent>(event)
        assertEquals("messaging:general", parsedEvent.cid)
        assertEquals("general", parsedEvent.channel.id)
        assertEquals("alice", parsedEvent.member.user.id)
    }

    @Test
    fun parseMessageReadEvent() {
        val event = parser.fromJson(messageReadJson(), ChatEvent::class)

        val parsedEvent = assertIs<MessageReadEvent>(event)
        assertEquals("messaging:general", parsedEvent.cid)
        assertEquals("alice", parsedEvent.user.id)
        assertEquals("m1", parsedEvent.lastReadMessageId)
    }

    private fun memberEventJson(type: String): String =
        """
            {
              "type": "$type",
              "createdAt": "2026-05-30T12:00:00Z",
              "rawCreatedAt": "2026-05-30T12:00:00Z",
              "cid": "messaging:general",
              "channelType": "messaging",
              "channelId": "general",
              "user": {
                "id": "alice",
                "name": "Alice"
              },
              "member": {
                "user": {
                  "id": "alice",
                  "name": "Alice"
                }
              }
            }
        """.trimIndent()

    private fun connectedHealthCheckJson(): String =
        """
            {
              "type": "health.check",
              "created_at": "2026-05-30T12:00:00Z",
              "connection_id": "connection-1",
              "me": {
                "id": "alice",
                "name": "Alice"
              }
            }
        """.trimIndent()

    private fun healthCheckJson(): String =
        """
            {
              "type": "health.check",
              "created_at": "2026-05-30T12:00:00Z",
              "connection_id": "connection-1"
            }
        """.trimIndent()

    private fun notificationMessageNewJson(): String =
        """
            {
              "type": "notification.message_new",
              "createdAt": "2026-05-30T12:00:00Z",
              "rawCreatedAt": "2026-05-30T12:00:00Z",
              "cid": "messaging:general",
              "channelType": "messaging",
              "channelId": "general",
              "totalUnreadCount": 7,
              "unreadChannels": 3,
              "channel": {
                "id": "general",
                "type": "messaging",
                "name": "General"
              },
              "message": {
                "id": "m1",
                "cid": "messaging:general",
                "text": "hello",
                "user": {
                  "id": "alice",
                  "name": "Alice"
                }
              }
            }
        """.trimIndent()

    private fun notificationAddedToChannelJson(): String =
        """
            {
              "type": "notification.added_to_channel",
              "createdAt": "2026-05-30T12:00:00Z",
              "rawCreatedAt": "2026-05-30T12:00:00Z",
              "cid": "messaging:general",
              "channelType": "messaging",
              "channelId": "general",
              "channel": {
                "id": "general",
                "type": "messaging",
                "name": "General"
              },
              "member": {
                "user": {
                  "id": "alice",
                  "name": "Alice"
                }
              }
            }
        """.trimIndent()

    private fun notificationRemovedFromChannelJson(): String =
        """
            {
              "type": "notification.removed_from_channel",
              "createdAt": "2026-05-30T12:00:00Z",
              "rawCreatedAt": "2026-05-30T12:00:00Z",
              "cid": "messaging:general",
              "channelType": "messaging",
              "channelId": "general",
              "channel": {
                "id": "general",
                "type": "messaging",
                "name": "General"
              },
              "member": {
                "user": {
                  "id": "alice",
                  "name": "Alice"
                }
              }
            }
        """.trimIndent()

    private fun messageReadJson(): String =
        """
            {
              "type": "message.read",
              "created_at": "2026-05-30T12:00:00Z",
              "cid": "messaging:general",
              "channel_type": "messaging",
              "channel_id": "general",
              "user": {
                "id": "alice",
                "name": "Alice"
              },
              "last_read_message_id": "m1"
            }
        """.trimIndent()
}
