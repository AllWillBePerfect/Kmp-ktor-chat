package org.example.project.domain.model

data class LocalChatMessage(
    val localId: String,
    val remoteId: String?,
    val clientMessageId: String?,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val createdAt: String,
    val syncStatus: SyncStatus,
    val retryCount: Int = 0,
    val lastSyncError: String? = null,
)
