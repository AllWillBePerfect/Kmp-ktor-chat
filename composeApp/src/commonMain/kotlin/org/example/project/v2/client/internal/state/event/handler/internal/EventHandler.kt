package org.example.project.v2.client.internal.state.event.handler.internal

import org.example.project.v2.client.events.ChatEvent

/**
 * Handles WebSocket and/or synced events to update local v2 state.
 */
internal interface EventHandler {
    fun startListening()
    fun stopListening()
    suspend fun handleEvents(vararg events: ChatEvent)
}
