package org.example.project.v2.client.internal.state.plugin.logic.querychannels.internal

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.example.project.v2.client.ChatClient
import org.example.project.v2.client.api.event.ChatEventHandler
import org.example.project.v2.client.api.event.EventHandlingResult
import org.example.project.v2.client.api.state.StateRegistry
import org.example.project.v2.client.events.NewMessageEvent
import org.example.project.v2.client.internal.state.plugin.state.querychannels.internal.QueryChannelsMutableState
import org.example.project.v2.core.models.Channel
import org.example.project.v2.core.models.Message
import org.example.project.v2.core.models.NeutralFilterObject
import org.example.project.v2.core.models.User
import kotlin.test.Test
import kotlin.test.assertEquals

class QueryChannelsLogicTest {

    @Test
    fun parseChatEventResults_resolvesChannelsFromStateRegistry() = runTest {
        val stateRegistry = StateRegistry(scope = backgroundScope)
        val channel = channel(id = "ch1")
        stateRegistry.upsertChannel(channel)

        var resolvedChannel: Channel? = null
        val eventHandler = ChatEventHandler { _, _, cachedChannel ->
            resolvedChannel = cachedChannel
            EventHandlingResult.Skip
        }

        val logic = QueryChannelsLogic(
            chatClient = clientWithChannelResponse(channel, backgroundScope),
            stateRegistry = stateRegistry,
            queryChannelsStateLogic = QueryChannelsStateLogic(
                QueryChannelsMutableState(
                    identifier = org.example.project.v2.client.internal.state.plugin.QueryChannelsIdentifier.Standard(NeutralFilterObject),
                    filter = NeutralFilterObject,
                    scope = backgroundScope,
                ),
            ),
            eventHandler = eventHandler,
            filter = NeutralFilterObject,
        )

        val results = logic.parseChatEventResults(listOf(newMessageEvent(channel)))

        assertEquals(listOf(EventHandlingResult.Skip), results)
        assertEquals(channel, resolvedChannel)
    }

    @Test
    fun watchAndAddChannel_fetchesAndStoresChannel() = runTest {
        val channel = channel(
            id = "general",
            name = "General",
            messages = listOf(
                Message(
                    id = "m1",
                    cid = "messaging:general",
                    text = "hello",
                    createdAt = "2026-05-30T10:00:00Z",
                    user = User(id = "alice", name = "Alice"),
                ),
            ),
        )
        val logic = QueryChannelsLogic(
            chatClient = clientWithChannelResponse(channel, backgroundScope),
            stateRegistry = StateRegistry(scope = backgroundScope),
            queryChannelsStateLogic = QueryChannelsStateLogic(
                QueryChannelsMutableState(
                    identifier = org.example.project.v2.client.internal.state.plugin.QueryChannelsIdentifier.Standard(NeutralFilterObject),
                    filter = NeutralFilterObject,
                    scope = backgroundScope,
                ),
            ),
            eventHandler = ChatEventHandler { _, _, _ -> EventHandlingResult.Skip },
            filter = NeutralFilterObject,
        )

        logic.watchAndAddChannel(channel.cid)

        assertEquals(channel, logic.channels.value?.get(channel.cid))
        assertEquals(channel.messages, logic.channels.value?.get(channel.cid)?.messages)
    }

    private fun clientWithChannelResponse(
        channel: Channel,
        scope: kotlinx.coroutines.CoroutineScope,
    ): ChatClient {
        val engine = MockEngine { request ->
            respond(
                content = """
                    {
                      "channel": {
                        "id": "${channel.id}",
                        "type": "${channel.type}",
                        "name": "${channel.name}",
                        "messages": [
                          {
                            "id": "m1",
                            "cid": "${channel.cid}",
                            "text": "hello",
                            "created_at": "2026-05-30T10:00:00Z",
                            "user": {
                              "id": "alice",
                              "name": "Alice"
                            }
                          }
                        ],
                        "members": []
                      }
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = io.ktor.http.headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        encodeDefaults = true
                    },
                )
            }
        }

        return ChatClient.Builder(
            apiKey = "test-api-key",
            wssUrl = "wss://example.com/ws",
        )
            .baseUrl("https://example.com")
            .httpClient(httpClient)
            .clientScope(scope)
            .build()
    }

    private fun channel(
        id: String,
        name: String = "Channel",
        type: String = "messaging",
        messages: List<Message> = emptyList(),
    ): Channel = Channel(
        id = id,
        type = type,
        name = name,
        messages = messages,
    )

    private fun newMessageEvent(channel: Channel): NewMessageEvent {
        val user = User(id = "alice", name = "Alice")
        return NewMessageEvent(
            type = "message.new",
            createdAt = "2026-05-30T10:00:00Z",
            rawCreatedAt = "2026-05-30T10:00:00Z",
            user = user,
            cid = channel.cid,
            channelType = channel.type,
            channelId = channel.id,
            message = Message(
                id = "m1",
                cid = channel.cid,
                text = "hello",
                createdAt = "2026-05-30T10:00:00Z",
                user = user,
            ),
        )
    }
}
