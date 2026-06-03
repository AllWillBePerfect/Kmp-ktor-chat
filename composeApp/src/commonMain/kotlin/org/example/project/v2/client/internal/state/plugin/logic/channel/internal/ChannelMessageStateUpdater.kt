package org.example.project.v2.client.internal.state.plugin.logic.channel.internal

import java.time.Instant
import java.util.UUID
import org.example.project.v2.client.api.models.SendMessageRequest
import org.example.project.v2.client.api.state.StateRegistry
import org.example.project.v2.core.models.Channel
import org.example.project.v2.core.models.Message
import org.example.project.v2.core.models.SyncStatus
import org.example.project.v2.core.models.User
import org.example.project.v2.platform.taggedLogger

internal class ChannelMessageStateUpdater(
    private val cid: String,
    private val stateRegistry: StateRegistry,
    private val currentUserIdProvider: () -> String?,
) {
    private val logger by taggedLogger("Chat:V2ChannelMessageUpdater")

    fun prepareLocalMessage(request: SendMessageRequest): Message {
        val now = Instant.now().toString()
        val clientMessageId = request.clientMessageId?.takeIf { it.isNotBlank() } ?: "local-${UUID.randomUUID()}"
        return Message(
            id = clientMessageId,
            cid = cid,
            text = request.text,
            syncStatus = SyncStatus.IN_PROGRESS,
            createdAt = now,
            createdLocallyAt = now,
            user = User(
                id = currentUserIdProvider().orEmpty(),
                name = currentUserIdProvider().orEmpty(),
            ),
            clientMessageId = clientMessageId,
        ).also { message ->
            logger.d { "[prepareLocalMessage] cid=$cid id=${message.id} clientMessageId=${message.clientMessageId} text=${message.text}" }
        }
    }

    fun applyLocalSend(message: Message) {
        logger.d { "[applyLocalSend] cid=$cid before=${stateRegistry.getMessages(cid).size} id=${message.id} clientMessageId=${message.clientMessageId}" }
        stateRegistry.upsertMessage(cid, message)
        logger.d { "[applyLocalSend] cid=$cid after=${stateRegistry.getMessages(cid).size}" }
    }

    fun applySendSuccess(message: Message) {
        logger.d { "[applySendSuccess] cid=$cid before=${stateRegistry.getMessages(cid).size} id=${message.id} clientMessageId=${message.clientMessageId}" }
        stateRegistry.upsertMessage(cid, message.copy(syncStatus = SyncStatus.COMPLETED))
        logger.d { "[applySendSuccess] cid=$cid after=${stateRegistry.getMessages(cid).size}" }
    }

    fun applySendFailure(localMessage: Message) {
        logger.d { "[applySendFailure] cid=$cid before=${stateRegistry.getMessages(cid).size} id=${localMessage.id} clientMessageId=${localMessage.clientMessageId}" }
        stateRegistry.upsertMessage(
            cid = cid,
            message = localMessage.copy(
                syncStatus = SyncStatus.FAILED_PERMANENTLY,
                updatedLocallyAt = Instant.now().toString(),
            ),
        )
        logger.d { "[applySendFailure] cid=$cid after=${stateRegistry.getMessages(cid).size}" }
    }

    fun applyNewMessage(message: Message) {
        logger.d { "[applyNewMessage] cid=$cid before=${stateRegistry.getMessages(cid).size} id=${message.id} clientMessageId=${message.clientMessageId}" }
        stateRegistry.upsertMessage(cid, message.copy(syncStatus = SyncStatus.COMPLETED))
        logger.d { "[applyNewMessage] cid=$cid after=${stateRegistry.getMessages(cid).size}" }
    }

    fun applyUpdatedMessage(message: Message) {
        logger.d { "[applyUpdatedMessage] cid=$cid before=${stateRegistry.getMessages(cid).size} id=${message.id} clientMessageId=${message.clientMessageId}" }
        stateRegistry.upsertMessage(cid, message.copy(syncStatus = SyncStatus.COMPLETED))
        logger.d { "[applyUpdatedMessage] cid=$cid after=${stateRegistry.getMessages(cid).size}" }
    }

    fun applyDeletedMessage(messageId: String) {
        logger.d { "[applyDeletedMessage] cid=$cid before=${stateRegistry.getMessages(cid).size} id=$messageId" }
        stateRegistry.deleteMessage(cid, messageId)
        logger.d { "[applyDeletedMessage] cid=$cid after=${stateRegistry.getMessages(cid).size}" }
    }

    fun applySnapshot(channel: Channel) {
        val remoteMessages = channel.messages.sortedBy { it.createdAt.orEmpty() }
        val localTransientMessages = stateRegistry.getMessages(cid).filter { localMessage ->
            localMessage.syncStatus != SyncStatus.COMPLETED &&
                remoteMessages.none { remoteMessage -> remoteMessage.matches(localMessage) }
        }
        logger.d {
            "[applySnapshot] cid=$cid remote=${remoteMessages.size} localTransient=${localTransientMessages.size} " +
                "remoteIds=${remoteMessages.map { it.id.ifBlank { it.clientMessageId.orEmpty() } }} " +
                "localTransientIds=${localTransientMessages.map { it.id.ifBlank { it.clientMessageId.orEmpty() } }}"
        }
        stateRegistry.setMessages(
            cid = cid,
            messages = (remoteMessages + localTransientMessages)
                .sortedBy { it.createdAt ?: it.createdLocallyAt ?: it.updatedLocallyAt.orEmpty() },
        )
        logger.d { "[applySnapshot] cid=$cid after=${stateRegistry.getMessages(cid).size}" }
    }

    private fun Message.matches(other: Message): Boolean {
        return when {
            id.isNotBlank() && other.id.isNotBlank() && id == other.id -> true
            clientMessageId != null && other.clientMessageId != null && clientMessageId == other.clientMessageId -> true
            else -> false
        }
    }
}
