package org.example.project.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val name: String
)

@Serializable
enum class ChatType {
    Direct,
    Group
}

@Serializable
data class ChatRoom(
    val id: String,
    val type: ChatType,
    val title: String,
    val members: List<User> = emptyList()
) {
    /**
     * Возвращает название для отображения.
     * Для групповых чатов - заголовок чата.
     * Для приватных (Direct) - имя собеседника или "Saved messages", если это чат с самим собой.
     */
    fun displayTitle(currentUserId: String?): String {
        return when (type) {
            ChatType.Group -> title
            ChatType.Direct -> {
                if (members.size == 1 && members.firstOrNull()?.id == currentUserId) {
                    "Saved messages"
                } else {
                    members.firstOrNull { it.id != currentUserId }?.name
                        ?: title.ifBlank { "Direct chat" }
                }
            }
        }
    }
}

@Serializable
data class ChatMessage(
    val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val createdAt: String
)
