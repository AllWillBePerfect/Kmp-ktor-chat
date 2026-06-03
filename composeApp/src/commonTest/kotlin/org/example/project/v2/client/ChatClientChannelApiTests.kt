package org.example.project.v2.client

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
import org.example.project.v2.client.api.models.QueryChannelRequest
import org.example.project.v2.client.api.models.QueryChannelsRequest
import org.example.project.v2.core.models.Message
import kotlin.test.Test
import kotlin.test.assertEquals

class ChatClientChannelApiTests {

    @Test
    fun queryChannelsSuccess() = runTest {
        val engine = MockEngine { request ->
            assertEquals("https://example.com/channels/query", request.url.toString())
            assertEquals("test-api-key", request.headers["X-Api-Key"])
            assertEquals(null, request.headers[HttpHeaders.Authorization])

            respond(
                content = """
                    {
                      "channels": [
                        {
                          "id": "general",
                          "type": "messaging",
                          "name": "General"
                        },
                        {
                          "id": "saved",
                          "type": "messaging",
                          "name": "Saved messages"
                        }
                      ]
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

        val client = ChatClient.Builder(
            apiKey = "test-api-key",
            wssUrl = "wss://example.com/ws",
        )
            .baseUrl("https://example.com")
            .httpClient(httpClient)
            .clientScope(backgroundScope)
            .build()

        val channels = client.queryChannels(QueryChannelsRequest(limit = 30))

        assertEquals(2, channels.size)
        assertEquals("general", channels.first().id)
        assertEquals("Saved messages", channels.last().name)
    }

    @Test
    fun queryChannelSuccess() = runTest {
        val engine = MockEngine { request ->
            assertEquals("https://example.com/channels/messaging/general/query", request.url.toString())
            assertEquals("test-api-key", request.headers["X-Api-Key"])
            assertEquals(null, request.headers[HttpHeaders.Authorization])

            respond(
                content = """
                    {
                      "channel": {
                        "id": "general",
                        "type": "messaging",
                        "name": "General",
                        "messages": [],
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

        val client = ChatClient.Builder(
            apiKey = "test-api-key",
            wssUrl = "wss://example.com/ws",
        )
            .baseUrl("https://example.com")
            .httpClient(httpClient)
            .clientScope(backgroundScope)
            .build()

        val channel = client.queryChannel(
            channelType = "messaging",
            channelId = "general",
            request = QueryChannelRequest(),
        )

        assertEquals("general", channel?.id)
        assertEquals("messaging", channel?.type)
        assertEquals("General", channel?.name)
    }

    @Test
    fun sendMessageSuccess() = runTest {
        val engine = MockEngine { request ->
            assertEquals("https://example.com/channels/messaging/general/messages", request.url.toString())
            assertEquals("test-api-key", request.headers["X-Api-Key"])
            assertEquals(null, request.headers[HttpHeaders.Authorization])

            respond(
                content = """
                    {
                      "message": {
                        "id": "m1",
                        "cid": "messaging:general",
                        "text": "hello",
                        "client_message_id": "client-1",
                        "created_at": "2026-05-30T10:00:00Z",
                        "user": {
                          "id": "alice",
                          "name": "Alice"
                        }
                      }
                    }
                """.trimIndent(),
                status = HttpStatusCode.Created,
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

        val client = ChatClient.Builder(
            apiKey = "test-api-key",
            wssUrl = "wss://example.com/ws",
        )
            .baseUrl("https://example.com")
            .httpClient(httpClient)
            .clientScope(backgroundScope)
            .build()

        val message = client.channel("messaging", "general").sendMessage(
            Message(text = "hello", clientMessageId = "client-1"),
        )

        assertEquals("m1", message.id)
        assertEquals("hello", message.text)
        assertEquals("client-1", message.clientMessageId)
    }

    @Test
    fun markReadSuccess() = runTest {
        val engine = MockEngine { request ->
            assertEquals("https://example.com/channels/messaging/general/read", request.url.toString())
            assertEquals("test-api-key", request.headers["X-Api-Key"])
            assertEquals(null, request.headers[HttpHeaders.Authorization])

            respond(
                content = "{}",
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

        val client = ChatClient.Builder(
            apiKey = "test-api-key",
            wssUrl = "wss://example.com/ws",
        )
            .baseUrl("https://example.com")
            .httpClient(httpClient)
            .clientScope(backgroundScope)
            .build()

        client.channel("messaging", "general").markRead()
    }
}
