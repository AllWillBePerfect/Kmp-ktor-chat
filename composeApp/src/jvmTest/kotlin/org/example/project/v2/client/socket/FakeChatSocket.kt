package org.example.project.v2.client.socket

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.CoroutineScope
import org.example.project.v2.client.events.ChatEvent
import org.example.project.v2.client.events.ConnectedEvent
import org.example.project.v2.client.parser.ChatParser
import kotlin.reflect.KClass

internal class FakeChatSocket(
    scope: CoroutineScope,
) : ChatSocket(
    socketFactory = SocketFactory(
        parser = object : ChatParser {
            override fun toJson(any: Any): String = error("Not needed in tests")

            override fun <T : Any> fromJson(raw: String, clazz: KClass<T>): T {
                error("Not needed in tests")
            }
        },
        client = HttpClient(MockEngine) {
            engine {
                addHandler { error("HTTP should not be used in FakeChatSocket tests") }
            }
        },
    ),
    scope = scope,
) {
    fun mockEventReceived(event: ChatEvent) {
        listeners().forEach { it.onEvent(event) }
    }

    fun mockConnecting() {
        listeners().forEach { it.onConnecting() }
    }

    fun mockConnected(event: ConnectedEvent) {
        listeners().forEach { it.onConnected(event) }
    }

    fun mockDisconnected(cause: String?) {
        listeners().forEach { it.onDisconnected(cause) }
    }

    fun mockError(error: Throwable) {
        listeners().forEach { it.onError(error) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun listeners(): Set<SocketListener> {
        val field = ChatSocket::class.java.getDeclaredField("listeners")
        field.isAccessible = true
        return (field.get(this) as MutableSet<SocketListener>).toSet()
    }
}
