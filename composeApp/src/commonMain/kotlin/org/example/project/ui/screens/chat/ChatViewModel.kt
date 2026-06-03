package org.example.project.ui.screens.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random
import org.example.project.data.PreferencesDataSource
import org.example.project.data.repository.LocalMessageRepository
import org.example.project.domain.model.ChatMessage
import org.example.project.domain.model.LocalChatMessage
import org.example.project.domain.model.SyncStatus
import org.example.project.domain.model.User
import org.example.project.ktor.WsClientContract
import org.example.project.ktor.isConnected
import org.example.project.platform.AppLogger
import org.example.project.ui.components.message.Message
import org.example.project.ui.screens.chat.ServerEvent.*

class ChatViewModel(
    private val wsClient: WsClientContract,
    private val pref: PreferencesDataSource,
    private val localMessageRepository: LocalMessageRepository,
    private val logger: AppLogger,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        logger.e(TAG, "Unhandled coroutine exception for chatId=$currentChatId", throwable)
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    private lateinit var currentUserId: String
    private lateinit var currentUserName: String
    
    private val currentChatId: String = requireNotNull(savedStateHandle.get<String>("chatId")) {
        "chatId is required but was not provided to ChatViewModel"
    }

    init {
        viewModelScope.launch(exceptionHandler) {
            val userData = pref.userDataFlow.first { !it.userId.isNullOrBlank() }
            currentUserId = requireNotNull(userData.userId)
            currentUserName = userData.userName.orEmpty()
            logger.i(TAG, "Initialized; chatId=$currentChatId currentUserId=$currentUserId")

            launch {
                localMessageRepository.observeMessages(currentChatId).collect { messages ->
                    logger.d(TAG, "Observed local messages; chatId=$currentChatId count=${messages.size}")
                    reduce {
                        copy(
                            messages = messages.map(::mapToUiMessage)
                        )
                    }
                }
            }

            launch {
                wsClient.events.collect { event ->
                    handleEvent(event)
                }
            }

            launch {
                wsClient.connectionState.collect { state ->
                    if (state.isConnected) {
                        logger.i(TAG, "Connection restored; re-syncing chatId=$currentChatId")
                        wsClient.requestUsers()
                        wsClient.requestChats()
                        wsClient.requestMessages(currentChatId)
                    } else {
                        logger.d(TAG, "Connection inactive; clearing online users for chatId=$currentChatId")
                        reduce { copy(onlineUsers = emptyList()) }
                    }
                }
            }
        }
    }

    fun onAction(action: ChatUiAction) {
        when (action) {
            is ChatUiAction.TextTyped -> reduce { copy(userText = action.text) }
            ChatUiAction.MessageSent -> sendMessage(uiState.value.userText)
            is ChatUiAction.MessageSentWithText -> sendMessage(action.text)
            is ChatUiAction.RetryMessage -> retryMessage(action.localId)
            ChatUiAction.Connect -> Unit 
            ChatUiAction.Disconnect -> Unit
        }
    }

    private fun sendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch(exceptionHandler) {
            val clientMessageId = generateClientMessageId()
            val localMessage = LocalChatMessage(
                localId = clientMessageId,
                remoteId = null,
                clientMessageId = clientMessageId,
                chatId = currentChatId,
                senderId = currentUserId,
                senderName = currentUserName.ifBlank { "me" },
                text = text,
                createdAt = "",
                syncStatus = SyncStatus.SYNC_NEEDED
            )

            logger.d(TAG, "Pending created; chatId=$currentChatId localId=${localMessage.localId} clientMessageId=${localMessage.clientMessageId}")
            localMessageRepository.upsert(localMessage)
            sendPendingMessage(localMessage)
        }
        reduce { copy(userText = "") }
    }

    private fun retryMessage(localId: String) {
        viewModelScope.launch(exceptionHandler) {
            val message = localMessageRepository.getByLocalId(localId) ?: return@launch
            if (message.chatId != currentChatId || message.remoteId != null) return@launch
            logger.i(TAG, "Retrying pending message; chatId=$currentChatId localId=$localId")
            sendPendingMessage(message)
        }
    }

    private suspend fun sendPendingMessage(message: LocalChatMessage) {
        logger.d(TAG, "Sending pending message; chatId=${message.chatId} localId=${message.localId} clientMessageId=${message.clientMessageId} retryCount=${message.retryCount}")
        localMessageRepository.markInProgress(
            localId = message.localId,
            retryCount = message.retryCount
        )

        runCatching {
            wsClient.sendMessage(
                text = message.text,
                chatId = message.chatId,
                clientMessageId = message.clientMessageId
            )
        }.onFailure { throwable ->
            logger.e(TAG, "Failed to send pending message; chatId=${message.chatId} localId=${message.localId} clientMessageId=${message.clientMessageId}", throwable)
            localMessageRepository.markSyncNeeded(
                localId = message.localId,
                retryCount = message.retryCount + 1,
                error = throwable.message
            )
        }
    }

    private inline fun reduce(reducer: ChatUiState.() -> ChatUiState) {
        _uiState.update { it.reducer() }
    }

    private fun handleEvent(event: ServerEvent) {
        when (event) {
            is MessageCreatedEvent -> {
                if (event.message.chatId == currentChatId) {
                    viewModelScope.launch(exceptionHandler) {
                        logger.d(TAG, "MessageCreated received; chatId=${event.message.chatId} remoteId=${event.message.id} clientMessageId=${event.message.clientMessageId}")
                        val existingRemoteMessage = localMessageRepository.getByRemoteId(event.message.id)
                        if (existingRemoteMessage != null) {
                            logger.d(TAG, "Updating existing remote message; remoteId=${event.message.id} localId=${existingRemoteMessage.localId}")
                            localMessageRepository.upsert(
                                event.message.toLocalMessage(localId = existingRemoteMessage.localId)
                            )
                            return@launch
                        }

                        val pendingMessage = event.message.clientMessageId?.let {
                            localMessageRepository.getByClientMessageId(it)
                        }

                        if (pendingMessage != null) {
                            logger.d(TAG, "Completing pending message via clientMessageId; remoteId=${event.message.id} localId=${pendingMessage.localId} clientMessageId=${event.message.clientMessageId}")
                            localMessageRepository.upsert(
                                event.message.toLocalMessage(localId = pendingMessage.localId)
                            )
                        } else {
                            logger.d(TAG, "Inserting new remote message; remoteId=${event.message.id}")
                            localMessageRepository.upsert(
                                event.message.toLocalMessage(localId = "remote:${event.message.id}")
                            )
                        }
                    }
                }
            }
            is MessagesSnapshotEvent -> {
                viewModelScope.launch(exceptionHandler) {
                    logger.d(TAG, "MessagesSnapshot received; chatId=$currentChatId count=${event.messages.count { it.chatId == currentChatId }}")
                    localMessageRepository.upsertAll(
                        event.messages
                            .filter { it.chatId == currentChatId }
                            .map { serverMessage ->
                                val localId = localMessageRepository.getByRemoteId(serverMessage.id)?.localId
                                    ?: serverMessage.clientMessageId?.let { clientMessageId ->
                                        localMessageRepository.getByClientMessageId(clientMessageId)?.localId
                                    }
                                    ?: "remote:${serverMessage.id}"

                                logger.d(TAG, "Snapshot merge; remoteId=${serverMessage.id} clientMessageId=${serverMessage.clientMessageId} resolvedLocalId=$localId")
                                serverMessage.toLocalMessage(localId = localId)
                            }
                    )
                }
            }
            is UsersSnapshotEvent -> {
                reduce { copy(onlineUsers = event.users) }
            }
            is UserJoinedEvent -> {
                reduce { copy(onlineUsers = (onlineUsers + event.user).distinctBy { it.id }) }
            }
            is UserLeftEvent -> {
                reduce { copy(onlineUsers = onlineUsers.filter { it.id != event.user.id }) }
            }
            is ErrorEvent -> {
                logger.w(TAG, "Server error event; message=${event.message}")
                val systemMessage = Message(
                    localId = "system-${generateClientMessageId()}",
                    author = "System",
                    content = "Error: ${event.message}",
                    timestamp = ""
                )
                reduce { copy(messages = messages + systemMessage) }
            }
            else -> Unit
        }
    }

    private fun mapToUiMessage(chatMessage: LocalChatMessage): Message {
        val isMe = chatMessage.senderId == currentUserId
        return Message(
            localId = chatMessage.localId,
            author = if (isMe) "me" else chatMessage.senderName,
            content = chatMessage.text,
            timestamp = chatMessage.createdAt.takeLast(5).ifBlank { "..." },
            syncStatus = chatMessage.syncStatus,
            lastSyncError = chatMessage.lastSyncError
        )
    }

    private fun ChatMessage.toLocalMessage(localId: String): LocalChatMessage {
        return LocalChatMessage(
            localId = localId,
            remoteId = id,
            clientMessageId = clientMessageId,
            chatId = chatId,
            senderId = senderId,
            senderName = senderName,
            text = text,
            createdAt = createdAt,
            syncStatus = SyncStatus.COMPLETED
        )
    }

    private fun generateClientMessageId(): String {
        return "local-${Random.nextLong().toString().removePrefix("-")}"
    }

    private companion object {
        const val TAG = "Chat:ChatViewModel"
    }
}

data class ChatUiState(
    val userText: String = "",
    val messages: List<Message> = emptyList(),
    val onlineUsers: List<User> = emptyList()
) {
    val isButtonEnabled: Boolean = userText.isNotBlank()
    val getReversedMessages = messages.asReversed()
    val onlineCountText: String = "${onlineUsers.size} users online"
}

sealed interface ChatUiAction {
    data class TextTyped(val text: String) : ChatUiAction
    data object MessageSent : ChatUiAction
    data class MessageSentWithText(val text: String) : ChatUiAction
    data class RetryMessage(val localId: String) : ChatUiAction
    data object Connect : ChatUiAction
    data object Disconnect : ChatUiAction
}
