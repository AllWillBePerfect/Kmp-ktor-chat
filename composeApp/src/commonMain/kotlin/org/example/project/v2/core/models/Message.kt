package org.example.project.v2.core.models

data class Message(
    val id: String = "",
    val cid: String = "",
    val text: String = "",
    val html: String = "",
    val parentId: String? = null,
    val replyCount: Int = 0,
    val syncStatus: SyncStatus = SyncStatus.COMPLETED,
    val type: String = TYPE_REGULAR,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val deletedAt: String? = null,
    val updatedLocallyAt: String? = null,
    val createdLocallyAt: String? = null,
    val user: User = User(),
    val extraData: Map<String, Any?> = emptyMap(),
    val silent: Boolean = false,
    val shadowed: Boolean = false,
    val showInChannel: Boolean = false,
    val replyMessageId: String? = null,
    val pinned: Boolean = false,
    val threadParticipants: List<User> = emptyList(),
    val clientMessageId: String? = null,
    val deletedForMe: Boolean = false,
) {
    companion object {
        const val TYPE_REGULAR: String = "regular"
        const val TYPE_EPHEMERAL: String = "ephemeral"
        const val TYPE_ERROR: String = "error"
        const val TYPE_REPLY: String = "reply"
        const val TYPE_SYSTEM: String = "system"
        const val TYPE_DELETED: String = "deleted"
    }
}
