package org.example.project.v2.client.parser

import kotlin.reflect.KClass
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.example.project.v2.client.events.ChannelDeletedEvent
import org.example.project.v2.client.events.ChannelUpdatedByUserEvent
import org.example.project.v2.client.events.ChannelUpdatedEvent
import org.example.project.v2.client.events.ChatEvent
import org.example.project.v2.client.events.ConnectedEvent
import org.example.project.v2.client.events.ConnectionErrorEvent
import org.example.project.v2.client.events.ConnectingEvent
import org.example.project.v2.client.events.DisconnectedEvent
import org.example.project.v2.client.events.HealthEvent
import org.example.project.v2.client.events.MessageDeletedEvent
import org.example.project.v2.client.events.MessageReadEvent
import org.example.project.v2.client.events.MessageUpdatedEvent
import org.example.project.v2.client.events.NewMessageEvent
import org.example.project.v2.client.events.TypingStartEvent
import org.example.project.v2.client.events.TypingStopEvent
import org.example.project.v2.client.parser.adapters.EventAdapter

class KotlinxChatParser : ChatParser {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }
    private val eventAdapter = EventAdapter(json)

    override fun toJson(any: Any): String {
        return when (any) {
            is JsonElement -> json.encodeToString(any)
            is ChatEvent -> buildJsonObject {
                put("type", JsonPrimitive(any.type))
                any.createdAt?.let { put("createdAt", JsonPrimitive(it)) }
                any.rawCreatedAt?.let { put("rawCreatedAt", JsonPrimitive(it)) }
            }.toString()
            else -> error("Unsupported type for serialization: ${any::class.simpleName}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> fromJson(raw: String, clazz: KClass<T>): T {
        return when (clazz) {
            ChatEvent::class -> eventAdapter.fromJson(raw) as T
            else -> error("Unsupported type for parsing: ${clazz.simpleName}")
        }
    }
}
