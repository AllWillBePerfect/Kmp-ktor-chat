package org.example.project.v2.client.socket

import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.example.project.v2.client.events.ChatEvent
import org.example.project.v2.client.parser.ChatParser
import org.example.project.v2.platform.taggedLogger

private const val EVENTS_BUFFER_SIZE = 100

internal class StreamWebSocket(
    private val parser: ChatParser,
    private val session: WebSocketSession,
    scope: CoroutineScope,
) {
    private val logger by taggedLogger("Chat:StreamWebSocket")
    private val eventFlow = MutableSharedFlow<StreamWebSocketEvent>(extraBufferCapacity = EVENTS_BUFFER_SIZE)

    init {
        scope.launch {
            try {
                for (frame in session.incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        logger.v { "[onMessage] payload=$text" }
                        eventFlow.tryEmit(parseMessage(text))
                    }
                }
                eventFlow.tryEmit(StreamWebSocketEvent.Closed)
            } catch (throwable: Throwable) {
                if (throwable !is CancellationException) {
                    eventFlow.tryEmit(StreamWebSocketEvent.Error(throwable))
                }
            }
        }
    }

    fun close(): Boolean = runCatching {
        session.cancel()
        true
    }.getOrDefault(false)

    suspend fun send(text: String) {
        session.send(Frame.Text(text))
    }

    fun listen(): Flow<StreamWebSocketEvent> = eventFlow.asSharedFlow()

    private fun parseMessage(text: String): StreamWebSocketEvent {
        return runCatching {
            parser.fromJson(text, ChatEvent::class)
        }.fold(
            onSuccess = { StreamWebSocketEvent.Message(it) },
            onFailure = { StreamWebSocketEvent.Error(it) },
        )
    }
}

internal sealed class StreamWebSocketEvent {
    data class Error(val throwable: Throwable) : StreamWebSocketEvent()
    data class Message(val chatEvent: ChatEvent) : StreamWebSocketEvent()
    data object Closed : StreamWebSocketEvent()
}
