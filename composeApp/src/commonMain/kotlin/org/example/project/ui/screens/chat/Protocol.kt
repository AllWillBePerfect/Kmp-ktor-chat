package org.example.project.ui.screens.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
sealed interface ClientCommand {
    val type: String
}

@Serializable
@SerialName("send_message")
data class SendMessageCommand(
    override val type: String = "send_message",
    val chatId: String = "general",
    val text: String
) : ClientCommand

@Serializable
@SerialName("rename_user")
data class RenameUserCommand(
    override val type: String = "rename_user",
    val name: String
) : ClientCommand

@Serializable
@SerialName("request_users")
data class RequestUsersCommand(
    override val type: String = "request_users"
) : ClientCommand

@Serializable
@SerialName("request_messages")
data class RequestMessagesCommand(
    override val type: String = "request_messages",
    val chatId: String = "general"
) : ClientCommand

@Serializable
@SerialName("create_chat")
data class CreateChatCommand(
    override val type: String = "create_chat",
    val title: String
) : ClientCommand


@Serializable
sealed interface ServerEvent {
    val type: String
}

@Serializable
@SerialName("message_created")
data class MessageCreatedEvent(
    override val type: String = "message_created",
    val message: ChatMessage
) : ServerEvent

@Serializable
@SerialName("user_joined")
data class UserJoinedEvent(
    override val type: String = "user_joined",
    val user: User
) : ServerEvent

@Serializable
@SerialName("user_left")
data class UserLeftEvent(
    override val type: String = "user_left",
    val user: User
) : ServerEvent

@Serializable
@SerialName("user_renamed")
data class UserRenamedEvent(
    override val type: String = "user_renamed",
    val user: User,
    val oldName: String
) : ServerEvent

@Serializable
@SerialName("users_snapshot")
data class UsersSnapshotEvent(
    override val type: String = "users_snapshot",
    val users: List<User>
) : ServerEvent

@Serializable
@SerialName("messages_snapshot")
data class MessagesSnapshotEvent(
    override val type: String = "messages_snapshot",
    val messages: List<ChatMessage>
) : ServerEvent

@Serializable
@SerialName("error")
data class ErrorEvent(
    override val type: String = "error",
    val message: String
) : ServerEvent

@Serializable
@SerialName("chats_snapshot")
data class ChatsSnapshotEvent(
    override val type: String = "chats_snapshot",
    val chats: List<ChatRoom>
) : ServerEvent

@Serializable
@SerialName("chat_created")
data class ChatCreatedEvent(
    override val type: String = "chat_created",
    val chat: ChatRoom
) : ServerEvent


object ClientProtocolJson {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encodeClientCommand(command: ClientCommand): String =
        /*when (command) {
            is SendMessageCommand -> json.encodeToString(command)
            is RenameUserCommand -> json.encodeToString(command)
            is RequestUsersCommand -> json.encodeToString(command)
            is RequestMessagesCommand -> json.encodeToString(command)
            is CreateChatCommand -> json.encodeToString(command)
        }*/
        json.encodeToString(command)

    fun decodeServerEvent(text: String): ServerEvent {
        val type = json.parseToJsonElement(text)
            .jsonObject["type"]
            ?.jsonPrimitive
            ?.content

        return when (type) {
            "message_created" -> json.decodeFromString<MessageCreatedEvent>(text)
            "user_joined" -> json.decodeFromString<UserJoinedEvent>(text)
            "user_left" -> json.decodeFromString<UserLeftEvent>(text)
            "user_renamed" -> json.decodeFromString<UserRenamedEvent>(text)
            "users_snapshot" -> json.decodeFromString<UsersSnapshotEvent>(text)
            "messages_snapshot" -> json.decodeFromString<MessagesSnapshotEvent>(text)
            "chats_snapshot" -> json.decodeFromString<ChatsSnapshotEvent>(text)
            "chat_created" -> json.decodeFromString<ChatCreatedEvent>(text)
            "error" -> json.decodeFromString<ErrorEvent>(text)
            else -> throw IllegalArgumentException("Unknown event type: $type")
        }
    }
}
