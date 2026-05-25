package org.example.project.ui.screens.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.example.project.domain.model.ChatMessage
import org.example.project.domain.model.ChatRoom
import org.example.project.domain.model.User

@Serializable
sealed interface ClientCommand {
    val type: String

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
    @SerialName("request_chats")
    data class RequestChatsCommand(
        override val type: String = "request_chats"
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
    @SerialName("add_chat_member")
    data class AddChatMemberCommand(
        override val type: String = "add_chat_member",
        val chatId: String,
        val userId: String
    ) : ClientCommand

    @Serializable
    @SerialName("create_direct_chat")
    data class CreateDirectChatCommand(
        override val type: String = "create_direct_chat",
        val userId: String
    ) : ClientCommand

    @Serializable
    @SerialName("login")
    data class LoginCommand(
        override val type: String = "login",
        val login: String,
        val password: String
    ) : ClientCommand

    @Serializable
    @SerialName("register")
    data class RegisterCommand(
        override val type: String = "register",
        val login: String,
        val password: String,
        val name: String
    ) : ClientCommand
}

@Serializable
sealed interface ServerEvent {
    val type: String

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
    @SerialName("login_succeeded")
    data class LoginSucceededEvent(
        override val type: String = "login_succeeded",
        val user: User
    ) : ServerEvent

    @Serializable
    @SerialName("auth_success")
    data class AuthSuccessEvent(
        override val type: String = "auth_success",
        val token: String,
        val userId: String,
        val userName: String
    ) : ServerEvent

    @Serializable
    @SerialName("users_snapshot")
    data class UsersSnapshotEvent(
        override val type: String = "users_snapshot",
        val users: List<User>
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

    @Serializable
    @SerialName("chat_member_added")
    data class ChatMemberAddedEvent(
        override val type: String = "chat_member_added",
        val chatId: String,
        val user: User
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
}

object ClientProtocolJson {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encodeClientCommand(command: ClientCommand): String =
        when (command) {
            is ClientCommand.SendMessageCommand -> json.encodeToString(command)
            is ClientCommand.RenameUserCommand -> json.encodeToString(command)
            is ClientCommand.RequestUsersCommand -> json.encodeToString(command)
            is ClientCommand.RequestChatsCommand -> json.encodeToString(command)
            is ClientCommand.RequestMessagesCommand -> json.encodeToString(command)
            is ClientCommand.CreateChatCommand -> json.encodeToString(command)
            is ClientCommand.AddChatMemberCommand -> json.encodeToString(command)
            is ClientCommand.CreateDirectChatCommand -> json.encodeToString(command)
            is ClientCommand.LoginCommand -> json.encodeToString(command)
            is ClientCommand.RegisterCommand -> json.encodeToString(command)
        }

    fun decodeServerEvent(text: String): ServerEvent {
        val type = json.parseToJsonElement(text)
            .jsonObject["type"]
            ?.jsonPrimitive
            ?.content

        return when (type) {
            "message_created" -> json.decodeFromString<ServerEvent.MessageCreatedEvent>(text)
            "user_joined" -> json.decodeFromString<ServerEvent.UserJoinedEvent>(text)
            "user_left" -> json.decodeFromString<ServerEvent.UserLeftEvent>(text)
            "user_renamed" -> json.decodeFromString<ServerEvent.UserRenamedEvent>(text)
            "login_succeeded" -> json.decodeFromString<ServerEvent.LoginSucceededEvent>(text)
            "auth_success" -> json.decodeFromString<ServerEvent.AuthSuccessEvent>(text)
            "users_snapshot" -> json.decodeFromString<ServerEvent.UsersSnapshotEvent>(text)
            "chats_snapshot" -> json.decodeFromString<ServerEvent.ChatsSnapshotEvent>(text)
            "chat_created" -> json.decodeFromString<ServerEvent.ChatCreatedEvent>(text)
            "chat_member_added" -> json.decodeFromString<ServerEvent.ChatMemberAddedEvent>(text)
            "messages_snapshot" -> json.decodeFromString<ServerEvent.MessagesSnapshotEvent>(text)
            "error" -> json.decodeFromString<ServerEvent.ErrorEvent>(text)
            else -> throw IllegalArgumentException("Unknown server event type: $type")
        }
    }
}
