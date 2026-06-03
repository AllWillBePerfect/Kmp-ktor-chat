package org.example.project.v2.client.socket

import org.example.project.v2.client.events.ChatEvent
import org.example.project.v2.client.events.ConnectedEvent

open class SocketListener {
    open fun onConnecting() = Unit

    open fun onConnected(event: ConnectedEvent) = Unit

    open fun onDisconnected(cause: String?) = Unit

    open fun onError(error: Throwable) = Unit

    open fun onEvent(event: ChatEvent) = Unit
}
