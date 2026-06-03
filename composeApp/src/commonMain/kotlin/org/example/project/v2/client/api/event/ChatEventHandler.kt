package org.example.project.v2.client.api.event

import org.example.project.v2.client.events.ChannelDeletedEvent
import org.example.project.v2.client.events.ChatEvent
import org.example.project.v2.client.events.CidEvent
import org.example.project.v2.client.events.HasChannel
import org.example.project.v2.core.models.Channel
import org.example.project.v2.core.models.FilterObject

fun interface ChatEventHandler {
    fun handleChatEvent(event: ChatEvent, filter: FilterObject, cachedChannel: Channel?): EventHandlingResult
}

sealed class EventHandlingResult {
    data class Add(val channel: Channel) : EventHandlingResult()
    data class WatchAndAdd(val cid: String) : EventHandlingResult()
    data class Remove(val cid: String) : EventHandlingResult()

    data object Skip : EventHandlingResult() {
        override fun toString(): String = "Skip"
    }
}

abstract class BaseChatEventHandler : ChatEventHandler {
    open fun handleChannelEvent(event: HasChannel, filter: FilterObject): EventHandlingResult {
        return when (event) {
            is ChannelDeletedEvent -> EventHandlingResult.Remove(event.cid)
            else -> EventHandlingResult.Skip
        }
    }

    open fun handleCidEvent(
        event: CidEvent,
        filter: FilterObject,
        cachedChannel: Channel?,
    ): EventHandlingResult = EventHandlingResult.Skip

    override fun handleChatEvent(
        event: ChatEvent,
        filter: FilterObject,
        cachedChannel: Channel?,
    ): EventHandlingResult {
        return when (event) {
            is HasChannel -> handleChannelEvent(event, filter)
            is CidEvent -> handleCidEvent(event, filter, cachedChannel)
            else -> EventHandlingResult.Skip
        }
    }
}
