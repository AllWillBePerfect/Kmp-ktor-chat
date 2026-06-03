package org.example.project.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.example.project.data.room.MessageDao
import org.example.project.data.room.MessageEntity
import org.example.project.domain.model.LocalChatMessage
import org.example.project.domain.model.SyncStatus

interface LocalMessageRepository {
    fun observeMessages(chatId: String): Flow<List<LocalChatMessage>>
    suspend fun getPendingMessages(): List<LocalChatMessage>
    suspend fun getByLocalId(localId: String): LocalChatMessage?
    suspend fun getByRemoteId(remoteId: String): LocalChatMessage?
    suspend fun getByClientMessageId(clientMessageId: String): LocalChatMessage?
    suspend fun upsert(message: LocalChatMessage)
    suspend fun upsertAll(messages: List<LocalChatMessage>)
    suspend fun markInProgress(localId: String, retryCount: Int)
    suspend fun markSyncNeeded(localId: String, retryCount: Int, error: String?)
    suspend fun markFailedPermanently(localId: String, retryCount: Int, error: String?)
    suspend fun markCompleted(localId: String, remoteId: String, createdAt: String)

    class Impl(
        private val dao: MessageDao
    ) : LocalMessageRepository {
        override fun observeMessages(chatId: String): Flow<List<LocalChatMessage>> {
            return dao.observeMessages(chatId).map { messages ->
                messages.map(MessageEntity::toDomain)
            }
        }

        override suspend fun getPendingMessages(): List<LocalChatMessage> {
            return dao.getMessagesBySyncStatuses(
                statuses = listOf(
                    SyncStatus.SYNC_NEEDED.name,
                    SyncStatus.IN_PROGRESS.name
                )
            ).map(MessageEntity::toDomain)
        }

        override suspend fun getByLocalId(localId: String): LocalChatMessage? {
            return dao.getByLocalId(localId)?.toDomain()
        }

        override suspend fun getByRemoteId(remoteId: String): LocalChatMessage? {
            return dao.getByRemoteId(remoteId)?.toDomain()
        }

        override suspend fun getByClientMessageId(clientMessageId: String): LocalChatMessage? {
            return dao.getByClientMessageId(clientMessageId)?.toDomain()
        }

        override suspend fun upsert(message: LocalChatMessage) {
            dao.upsert(message.toEntity())
        }

        override suspend fun upsertAll(messages: List<LocalChatMessage>) {
            dao.upsertAll(messages.map(LocalChatMessage::toEntity))
        }

        override suspend fun markInProgress(localId: String, retryCount: Int) {
            dao.updateSyncState(
                localId = localId,
                syncStatus = SyncStatus.IN_PROGRESS.name,
                retryCount = retryCount,
                lastSyncError = null
            )
        }

        override suspend fun markSyncNeeded(localId: String, retryCount: Int, error: String?) {
            dao.updateSyncState(
                localId = localId,
                syncStatus = SyncStatus.SYNC_NEEDED.name,
                retryCount = retryCount,
                lastSyncError = error
            )
        }

        override suspend fun markFailedPermanently(localId: String, retryCount: Int, error: String?) {
            dao.updateSyncState(
                localId = localId,
                syncStatus = SyncStatus.FAILED_PERMANENTLY.name,
                retryCount = retryCount,
                lastSyncError = error
            )
        }

        override suspend fun markCompleted(localId: String, remoteId: String, createdAt: String) {
            dao.markCompleted(
                localId = localId,
                remoteId = remoteId,
                createdAt = createdAt
            )
        }
    }
}

private fun MessageEntity.toDomain(): LocalChatMessage {
    return LocalChatMessage(
        localId = localId,
        remoteId = remoteId,
        clientMessageId = clientMessageId,
        chatId = chatId,
        senderId = senderId,
        senderName = senderName,
        text = text,
        createdAt = createdAt,
        syncStatus = syncStatus.toSyncStatus(),
        retryCount = retryCount,
        lastSyncError = lastSyncError
    )
}

private fun LocalChatMessage.toEntity(): MessageEntity {
    return MessageEntity(
        localId = localId,
        remoteId = remoteId,
        clientMessageId = clientMessageId,
        chatId = chatId,
        senderId = senderId,
        senderName = senderName,
        text = text,
        createdAt = createdAt,
        syncStatus = syncStatus.name,
        retryCount = retryCount,
        lastSyncError = lastSyncError
    )
}

private fun String.toSyncStatus(): SyncStatus {
    return runCatching { SyncStatus.valueOf(this) }
        .getOrDefault(SyncStatus.SYNC_NEEDED)
}
