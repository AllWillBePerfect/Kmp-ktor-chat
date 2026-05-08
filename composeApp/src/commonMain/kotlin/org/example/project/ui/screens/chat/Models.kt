package org.example.project.ui.screens.chat
import kotlinx.serialization.Serializable

data class ChatSession(
    val id: String
)

@Serializable
data class User(
    val id: String,
    val name: String
)

@Serializable
data class ChatRoom(
    val id: String,
    val title: String
)

@Serializable
data class ChatMessage(
    val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val createdAt: String
)
