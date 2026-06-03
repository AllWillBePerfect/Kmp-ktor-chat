package org.example.project.v2.core.models

data class User(
    val id: String = "",
    val role: String = "",
    val name: String = "",
    val image: String = "",
    val online: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val lastActive: String? = null,
    val totalUnreadCount: Int = 0,
    val unreadChannels: Int = 0,
    val extraData: Map<String, Any?> = emptyMap(),
)
