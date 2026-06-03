package org.example.project.v2.client.utils.observable

import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.example.project.v2.client.events.ChatEvent
import org.example.project.v2.client.events.ConnectedEvent
import org.example.project.v2.client.events.ConnectionErrorEvent
import org.example.project.v2.client.events.ConnectingEvent
import org.example.project.v2.client.events.DisconnectedEvent
import org.example.project.v2.client.events.UnknownEvent
import org.example.project.v2.client.socket.FakeChatSocket
import org.example.project.v2.core.models.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ChatEventsObservableTest {

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun filtering() = runTest {
        val result = mutableListOf<ChatEvent>()
        val socket = FakeChatSocket(backgroundScope)
        val observable = ChatEventsObservable(backgroundScope, socket)
        val eventA = UnknownEvent(type = "a", rawPayload = "{}")
        val eventB = UnknownEvent(type = "b", rawPayload = """{"cid":"myCid"}""")
        val eventC = UnknownEvent(type = "c", rawPayload = "{}")

        observable.subscribe(filter = { it.type == "b" }) { result.add(it) }
        runCurrent()

        socket.mockEventReceived(eventA)
        socket.mockEventReceived(eventB)
        socket.mockEventReceived(eventC)
        runCurrent()

        assertEquals(listOf<ChatEvent>(eventB), result)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun unsubscription() = runTest {
        val result = mutableListOf<ChatEvent>()
        val socket = FakeChatSocket(backgroundScope)
        val observable = ChatEventsObservable(backgroundScope, socket)
        val eventA = UnknownEvent(type = "a", rawPayload = "{}")
        val eventB = UnknownEvent(type = "b", rawPayload = "{}")
        val eventC = UnknownEvent(type = "c", rawPayload = "{}")

        val subscription = observable.subscribe { result.add(it) }
        runCurrent()

        socket.mockEventReceived(eventA)
        socket.mockEventReceived(eventB)
        runCurrent()
        subscription.dispose()
        socket.mockEventReceived(eventC)
        runCurrent()

        assertEquals(listOf<ChatEvent>(eventA, eventB), result)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun socketLifecycleCallbacks_areMappedToConnectionEvents() = runTest {
        val result = mutableListOf<ChatEvent>()
        val socket = FakeChatSocket(backgroundScope)
        val observable = ChatEventsObservable(backgroundScope, socket)
        val connected = ConnectedEvent(
            type = "health.check",
            createdAt = "2026-05-30T12:00:00Z",
            rawCreatedAt = "2026-05-30T12:00:00Z",
            me = User(id = "alice", name = "Alice"),
            connectionId = "connection-1",
        )

        observable.subscribe { result.add(it) }
        runCurrent()

        socket.mockConnecting()
        socket.mockConnected(connected)
        socket.mockDisconnected("closed")
        socket.mockError(IllegalStateException("boom"))
        runCurrent()

        assertIs<ConnectingEvent>(result[0])
        assertEquals(connected, result[1])
        assertEquals("closed", assertIs<DisconnectedEvent>(result[2]).disconnectCause)
        assertEquals("boom", assertIs<ConnectionErrorEvent>(result[3]).errorMessage)
    }
}
