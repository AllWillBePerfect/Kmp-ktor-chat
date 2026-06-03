package org.example.project.v2.client

import org.example.project.v2.client.events.ChatEvent

fun interface ChatEventListener<EventT : ChatEvent> {
    fun onEvent(event: EventT)
}
