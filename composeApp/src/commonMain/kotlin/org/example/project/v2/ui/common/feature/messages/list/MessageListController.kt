package org.example.project.v2.ui.common.feature.messages.list

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.v2.client.ChatClient
import org.example.project.v2.client.channel.state.ChannelState
import org.example.project.v2.core.models.Message
import org.example.project.v2.core.models.SyncStatus
import org.example.project.v2.platform.taggedLogger
import org.example.project.v2.ui.common.state.messages.list.MessageListState

class MessageListController(
    private val cid: String,
    private val chatClient: ChatClient,
    private val channelState: ChannelState,
    private val scope: CoroutineScope,
) {
    private val logger by taggedLogger("Chat:V2MessageListController")
    private val controllerScope = CoroutineScope(
        scope.coroutineContext + SupervisorJob() + CoroutineExceptionHandler { _, throwable ->
            logger.e(throwable) { "[scope] unhandled exception; cid=$cid" }
        },
    )
    private var lastSeenMessageId: String? = null
    private val _state = MutableStateFlow(MessageListState())
    val state: StateFlow<MessageListState> = _state.asStateFlow()

    init {
        val initialCurrentUserId = chatClient.clientState.user.value?.id
        val initialMessages = channelState.messages.value
        val initialTypingUsers = channelState.typing.value
        val initialReads = channelState.reads.value

        _state.value = MessageListState(
            messages = initialMessages,
            typingUsers = initialTypingUsers,
            reads = initialReads,
            currentUserId = initialCurrentUserId,
        )
        markLastMessageRead(
            messages = initialMessages,
            currentUserId = initialCurrentUserId,
        )

        controllerScope.launch {
            channelState.messages.collectLatest { messages ->
                logger.d {
                    "[messages] cid=$cid size=${messages.size} ids=${
                        messages.map { it.id.ifBlank { it.clientMessageId.orEmpty() } }
                    }"
                }
                _state.update { it.copy(messages = messages) }
                markLastMessageRead(
                    messages = messages,
                    currentUserId = state.value.currentUserId,
                )
            }
        }
        controllerScope.launch {
            channelState.typing.collectLatest { typingUsers ->
                _state.update { it.copy(typingUsers = typingUsers) }
            }
        }
        controllerScope.launch {
            channelState.reads.collectLatest { reads ->
                _state.update { it.copy(reads = reads) }
            }
        }
        controllerScope.launch {
            chatClient.clientState.user.collectLatest { user ->
                _state.update { it.copy(currentUserId = user?.id) }
                markLastMessageRead(
                    messages = state.value.messages,
                    currentUserId = user?.id,
                )
            }
        }
    }

    private fun markLastMessageRead(
        messages: List<Message>,
        currentUserId: String?,
    ) {
        markLastMessageReadInternal(messages, currentUserId)
    }

    private fun markLastMessageReadInternal(
        messages: List<Message>,
        currentUserId: String?,
    ) {
        val lastMessage = messages.lastOrNull() ?: return
        if (
            lastMessage.user.id == currentUserId &&
            lastMessage.syncStatus != SyncStatus.COMPLETED
        ) {
            logger.v { "[markLastMessageRead] skipped; cid=$cid own unsynced message=${lastMessage.id}" }
            return
        }
        if (lastSeenMessageId == lastMessage.id) {
            logger.v { "[markLastMessageRead] skipped; cid=$cid already seen=${lastMessage.id}" }
            return
        }
        logger.d {
            "[markLastMessageRead] cid=$cid lastMessageId=${lastMessage.id} " +
                "clientMessageId=${lastMessage.clientMessageId} currentUserId=$currentUserId " +
                "author=${lastMessage.user.id}"
        }
        lastSeenMessageId = lastMessage.id
        val separatorIndex = cid.indexOf(':')
        if (separatorIndex <= 0 || separatorIndex >= cid.lastIndex) return
        val channelType = cid.substring(0, separatorIndex)
        val channelId = cid.substring(separatorIndex + 1)
        chatClient.markReadAsync(channelType, channelId)
    }

    fun onCleared() {
        controllerScope.cancel()
    }
}
