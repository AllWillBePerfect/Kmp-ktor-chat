package org.example.project.v2.ui.common.feature.messages.composer

import kotlin.random.Random
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.v2.client.ChatClient
import org.example.project.v2.core.models.Message
import org.example.project.v2.platform.taggedLogger
import org.example.project.v2.ui.common.state.messages.composer.MessageComposerState
import org.example.project.v2.ui.common.utils.typing.TypingUpdatesBuffer
import org.example.project.v2.ui.common.utils.typing.internal.DefaultTypingUpdatesBuffer

class MessageComposerController(
    private val channelCid: String,
    private val chatClient: ChatClient,
    private val scope: CoroutineScope,
) {
    private val logger by taggedLogger("Chat:V2MessageComposerController")
    private val channelClient = chatClient.channel(channelCid)
    private val controllerScope = CoroutineScope(
        scope.coroutineContext + SupervisorJob() + CoroutineExceptionHandler { _, throwable ->
            logger.e(throwable) { "[scope] unhandled exception; cid=$channelCid" }
        },
    )

    private val _state = MutableStateFlow(MessageComposerState())
    val state: StateFlow<MessageComposerState> = _state.asStateFlow()

    val typingUpdatesBuffer: TypingUpdatesBuffer = DefaultTypingUpdatesBuffer(
        coroutineScope = controllerScope,
        onTypingStarted = {
            controllerScope.launch {
                channelClient.keystroke()
            }
        },
        onTypingStopped = {
            controllerScope.launch {
                channelClient.stopTyping()
            }
        },
    )

    fun setMessageInput(input: String) {
        _state.update { it.copy(inputValue = input, errorMessage = null) }
        typingUpdatesBuffer.onKeystroke(input)
    }

    fun sendMessage() {
        val text = state.value.inputValue.trim()
        if (text.isEmpty()) return

        controllerScope.launch {
            val clientMessageId = "local-${Random.nextLong().toString().removePrefix("-")}"
            logger.d { "[sendMessage] cid=$channelCid clientMessageId=$clientMessageId" }
            runCatching {
                channelClient.sendMessage(
                    Message(
                        cid = channelCid,
                        text = text,
                        clientMessageId = clientMessageId,
                    ),
                )
            }.onSuccess {
                typingUpdatesBuffer.clear()
                _state.update { it.copy(inputValue = "", errorMessage = null) }
            }.onFailure { throwable ->
                logger.e(throwable) { "[sendMessage] failed; cid=$channelCid" }
                _state.update { it.copy(errorMessage = throwable.message) }
            }
        }
    }

    fun onCleared() {
        typingUpdatesBuffer.clear()
        controllerScope.cancel()
    }
}
