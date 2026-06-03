package org.example.project.v2.client.parser.adapters

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.example.project.v2.core.models.Channel
import org.example.project.v2.core.models.ChannelUserRead
import org.example.project.v2.core.models.Member
import org.example.project.v2.core.models.Message
import org.example.project.v2.core.models.SyncStatus
import org.example.project.v2.core.models.User

internal fun JsonObject.toUser(): User {
    return User(
        id = string("id").orEmpty(),
        role = string("role").orEmpty(),
        name = string("name").orEmpty(),
        image = string("image").orEmpty(),
        online = boolean("online") ?: false,
        createdAt = string("createdAt") ?: string("created_at"),
        updatedAt = string("updatedAt") ?: string("updated_at"),
        lastActive = string("lastActive") ?: string("last_active"),
        totalUnreadCount = int("totalUnreadCount") ?: int("total_unread_count") ?: 0,
        unreadChannels = int("unreadChannels") ?: int("unread_channels") ?: 0,
        extraData = emptyMap(),
    )
}

internal fun JsonObject.toChannel(): Channel {
    return Channel(
        id = string("id").orEmpty(),
        type = string("type").orEmpty(),
        name = string("name").orEmpty(),
        image = string("image").orEmpty(),
        watcherCount = int("watcherCount") ?: int("watcher_count") ?: 0,
        frozen = boolean("frozen") ?: false,
        createdAt = string("createdAt") ?: string("created_at"),
        deletedAt = string("deletedAt") ?: string("deleted_at"),
        updatedAt = string("updatedAt") ?: string("updated_at"),
        syncStatus = SyncStatus.COMPLETED,
        memberCount = int("memberCount") ?: int("member_count") ?: 0,
        messages = objectList("messages").map { it.toMessage() },
        members = objectList("members").mapNotNull { it.objectValue("user")?.toUser() ?: it.toMemberUserFallback() },
        reads = objectList("reads").map { it.toChannelUserRead() },
        watchers = objectList("watchers").map { it.toUser() },
        createdBy = objectValue("createdBy")?.toUser() ?: objectValue("created_by")?.toUser() ?: User(),
        unreadCount = int("unreadCount") ?: int("unread_count") ?: 0,
        team = string("team").orEmpty(),
        hidden = boolean("hidden"),
        messageCount = int("messageCount") ?: int("message_count"),
        lastMessageAt = string("lastMessageAt") ?: string("last_message_at"),
        extraData = emptyMap(),
    )
}

internal fun JsonObject.toMessage(): Message {
    return Message(
        id = string("id").orEmpty(),
        cid = string("cid").orEmpty(),
        text = string("text").orEmpty(),
        html = string("html").orEmpty(),
        parentId = string("parentId") ?: string("parent_id"),
        replyCount = int("replyCount") ?: int("reply_count") ?: 0,
        syncStatus = SyncStatus.COMPLETED,
        type = string("type").orEmpty().ifBlank { Message.TYPE_REGULAR },
        createdAt = string("createdAt") ?: string("created_at"),
        updatedAt = string("updatedAt") ?: string("updated_at"),
        deletedAt = string("deletedAt") ?: string("deleted_at"),
        updatedLocallyAt = string("updatedLocallyAt") ?: string("updated_locally_at"),
        createdLocallyAt = string("createdLocallyAt") ?: string("created_locally_at"),
        user = objectValue("user")?.toUser() ?: User(),
        extraData = emptyMap(),
        silent = boolean("silent") ?: false,
        shadowed = boolean("shadowed") ?: false,
        showInChannel = boolean("showInChannel") ?: boolean("show_in_channel") ?: false,
        replyMessageId = string("replyMessageId") ?: string("reply_message_id"),
        pinned = boolean("pinned") ?: false,
        threadParticipants = objectList("threadParticipants").map { it.toUser() },
        clientMessageId = string("clientMessageId") ?: string("client_message_id"),
        deletedForMe = boolean("deletedForMe") ?: boolean("deleted_for_me") ?: false,
    )
}

internal fun JsonObject.toMember(): Member {
    return Member(
        user = objectValue("user")?.toUser() ?: User(),
    )
}

internal fun JsonObject.toChannelUserRead(): ChannelUserRead {
    return ChannelUserRead(
        user = objectValue("user")?.toUser() ?: User(),
        lastReadMessageId = string("lastReadMessageId") ?: string("last_read_message_id"),
        lastReadAt = string("lastReadAt") ?: string("last_read_at"),
    )
}

internal fun JsonObject.toMemberUserFallback(): User? {
    val userObject = objectValue("user") ?: return null
    return userObject.toUser()
}

internal fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

internal fun JsonObject.int(key: String): Int? = this[key]?.jsonPrimitive?.contentOrNull?.toIntOrNull()

internal fun JsonObject.boolean(key: String): Boolean? = this[key]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()

internal fun JsonObject.objectValue(key: String): JsonObject? = this[key]?.jsonObject

internal fun JsonObject.objectList(key: String): List<JsonObject> {
    return this[key]
        ?.let { element -> element as? JsonArray }
        ?.mapNotNull { it as? JsonObject }
        ?: emptyList()
}
