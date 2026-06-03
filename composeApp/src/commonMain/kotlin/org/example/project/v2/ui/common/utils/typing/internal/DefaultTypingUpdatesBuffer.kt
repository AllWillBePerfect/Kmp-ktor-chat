package org.example.project.v2.ui.common.utils.typing.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.project.v2.ui.common.utils.typing.TypingUpdatesBuffer

internal class DefaultTypingUpdatesBuffer(
    private val coroutineScope: CoroutineScope,
    private val onTypingStarted: () -> Unit,
    private val onTypingStopped: () -> Unit,
) : TypingUpdatesBuffer {

    private var isTypingTimerJob: Job? = null
    private var sendUpdatesJob: Job? = null

    private var isTyping: Boolean = false
        set(value) {
            field = value
            if (isTyping) {
                handleTypingEvent()
            }
        }

    override fun onKeystroke(inputText: String) {
        isTypingTimerJob?.cancel()
        when (inputText.isEmpty()) {
            true -> {
                isTyping = false
                onTypingStopped()
            }
            false -> {
                if (!isTyping) {
                    isTyping = true
                }
                isTypingTimerJob = coroutineScope.launch {
                    delay(DEFAULT_BUFFER_TYPING_UPDATES_INTERVAL)
                    isTyping = false
                }
            }
        }
    }

    override fun clear() {
        coroutineScope.coroutineContext.cancelChildren()
        if (isTyping) {
            isTyping = false
        }
        onTypingStopped()
    }

    private fun handleTypingEvent() {
        sendUpdatesJob?.cancel()
        sendUpdatesJob = coroutineScope.launch {
            while (isTyping) {
                onTypingStarted()
                delay(DEFAULT_SEND_TYPING_UPDATES_INTERVAL)
            }
            onTypingStopped()
        }
    }

    private companion object {
        private const val DEFAULT_SEND_TYPING_UPDATES_INTERVAL: Long = 3000L
        private const val DEFAULT_BUFFER_TYPING_UPDATES_INTERVAL: Long = 1000L
    }
}
