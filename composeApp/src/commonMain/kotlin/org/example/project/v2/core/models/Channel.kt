package org.example.project.v2.core.models

data class Channel(
    val id: String = "",
    val type: String = "",
    val name: String = "",
    val image: String = "",
    val watcherCount: Int = 0,
    val frozen: Boolean = false,
    val createdAt: String? = null,
    val deletedAt: String? = null,
    val updatedAt: String? = null,
    val syncStatus: SyncStatus = SyncStatus.COMPLETED,
    val memberCount: Int = 0,
    val messages: List<Message> = emptyList(),
    val members: List<User> = emptyList(),
    val reads: List<ChannelUserRead> = emptyList(),
    val watchers: List<User> = emptyList(),
    val createdBy: User = User(),
    val unreadCount: Int = 0,
    val team: String = "",
    val hidden: Boolean? = null,
    val messageCount: Int? = null,
    val lastMessageAt: String? = null,
    val extraData: Map<String, Any?> = emptyMap(),
) {
    val cid: String
        get() = if (id.isEmpty() || type.isEmpty()) "" else "$type:$id"
}
