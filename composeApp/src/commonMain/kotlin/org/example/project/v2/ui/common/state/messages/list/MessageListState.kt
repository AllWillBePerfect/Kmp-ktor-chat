package org.example.project.v2.ui.common.state.messages.list

import org.example.project.v2.core.models.Message
import org.example.project.v2.core.models.User

data class MessageListState(
    val messages: List<Message> = emptyList(),
    val typingUsers: List<User> = emptyList(),
    val reads: Map<String, String?> = emptyMap(),
    val currentUserId: String? = null,
) {
    fun readCountForMessage(message: Message): Int {
        val currentUser = currentUserId ?: return 0
        val messageIndex = messages.indexOfFirst { it.id == message.id }
        if (messageIndex < 0) return 0
        return reads
            .filterKeys { it != currentUser }
            .count { (_, lastReadMessageId) ->
                val readIndex = messages.indexOfFirst { it.id == lastReadMessageId }
                readIndex >= messageIndex && readIndex >= 0
            }
    }

    val typingLabel: String?
        get() {
            val names = typingUsers
                .filterNot { it.id == currentUserId }
                .map { it.name.ifBlank { it.id } }
            return when (names.size) {
                0 -> null
                1 -> "${names.first()} is typing..."
                else -> "${names.joinToString()} are typing..."
            }
        }
}
