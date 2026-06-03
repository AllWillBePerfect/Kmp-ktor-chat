package org.example.project.v2.client.socket

import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketExtension
import io.ktor.websocket.WebSocketSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.example.project.v2.client.events.ConnectedEvent
import org.example.project.v2.client.events.ChatEvent
import org.example.project.v2.client.parser.ChatParser
import org.example.project.v2.core.models.User
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class StreamWebSocketTest {

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun incomingTextFrame_isParsedAndEmittedAsMessageEvent() = runTest {
        val session = FakeWebSocketSession()
        val expectedEvent = ConnectedEvent(
            type = "health.check",
            createdAt = "2026-05-30T12:00:00Z",
            rawCreatedAt = "2026-05-30T12:00:00Z",
            me = User(id = "alice", name = "Alice"),
            connectionId = "connection-1",
        )
        val parser = object : ChatParser {
            override fun toJson(any: Any): String = error("Not needed")

            @Suppress("UNCHECKED_CAST")
            override fun <T : Any> fromJson(raw: String, clazz: KClass<T>): T {
                assertEquals("""{"type":"health.check"}""", raw)
                assertTrue(clazz == ChatEvent::class)
                return expectedEvent as T
            }
        }
        val streamWebSocket = StreamWebSocket(
            parser = parser,
            session = session,
            scope = backgroundScope,
        )
        val result = backgroundScope.async { streamWebSocket.listen().first() }
        runCurrent()

        session.incomingChannel.send(Frame.Text("""{"type":"health.check"}"""))
        runCurrent()

        val event = result.await()
        assertEquals(StreamWebSocketEvent.Message(expectedEvent), event)
        streamWebSocket.close()
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun parserFailure_isEmittedAsErrorEvent() = runTest {
        val session = FakeWebSocketSession()
        val failure = IllegalStateException("bad payload")
        val parser = object : ChatParser {
            override fun toJson(any: Any): String = error("Not needed")

            override fun <T : Any> fromJson(raw: String, clazz: KClass<T>): T {
                throw failure
            }
        }
        val streamWebSocket = StreamWebSocket(
            parser = parser,
            session = session,
            scope = backgroundScope,
        )
        val result = backgroundScope.async { streamWebSocket.listen().first() }
        runCurrent()

        session.incomingChannel.send(Frame.Text("""{"type":"broken"}"""))
        runCurrent()

        val event = result.await()
        val error = assertIs<StreamWebSocketEvent.Error>(event)
        assertEquals(failure, error.throwable)
        streamWebSocket.close()
    }

    @Test
    fun close_cancelsSessionAndReturnsTrue() = runTest {
        val session = FakeWebSocketSession()
        val streamWebSocket = StreamWebSocket(
            parser = object : ChatParser {
                override fun toJson(any: Any): String = error("Not needed")
                override fun <T : Any> fromJson(raw: String, clazz: KClass<T>): T = error("Not needed")
            },
            session = session,
            scope = backgroundScope,
        )

        val result = streamWebSocket.close()

        assertTrue(result)
        assertTrue(session.wasCancelled)
    }

    private class FakeWebSocketSession : WebSocketSession {
        private val job = Job()
        private val scope = CoroutineScope(Dispatchers.Default + job)

        val incomingChannel = Channel<Frame>(Channel.UNLIMITED)
        private val outgoingChannel = Channel<Frame>(Channel.UNLIMITED)
        val wasCancelled: Boolean
            get() = !job.isActive

        override val coroutineContext: CoroutineContext = scope.coroutineContext
        override var masking: Boolean = false
        override var maxFrameSize: Long = Long.MAX_VALUE
        override val incoming: ReceiveChannel<Frame> = incomingChannel
        override val outgoing: SendChannel<Frame> = outgoingChannel
        override val extensions: List<WebSocketExtension<*>> = emptyList()

        override suspend fun flush() = Unit

        @Deprecated("Implements the WebSocketSession contract for tests only.")
        override fun terminate() {
            incomingChannel.cancel()
            outgoingChannel.cancel()
            scope.cancel()
        }
    }
}
