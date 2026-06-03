package org.example.project.v2.core.models

data class ChannelUserRead(
    val user: User = User(),
    val lastReadMessageId: String? = null,
    val lastReadAt: String? = null,
)
