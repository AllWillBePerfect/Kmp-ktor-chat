package org.example.project.v2.client.api2

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
import org.example.project.v2.client.api.models.QueryChannelsRequest
import org.example.project.v2.client.api.models.SendMessageRequest
import org.example.project.v2.client.api.models.WatchChannelRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class KtorChatApiTest {

    @Test
    fun queryChannels_sendsHeadersAndParsesChannels() = runTest {
        val engine = MockEngine { request ->
            assertEquals("https://example.com/channels/query", request.url.toString())
            assertEquals("test-api-key", request.headers["X-Api-Key"])
            assertEquals("Bearer test-token", request.headers[HttpHeaders.Authorization])

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

        val client = HttpClient(engine) {
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

        val api = KtorChatApi(
            baseUrl = "https://example.com",
            apiKey = "test-api-key",
            tokenProvider = { "test-token" },
            client = client,
        )

        val channels = api.queryChannels(QueryChannelsRequest(limit = 30))

        assertEquals(2, channels.size)
        assertEquals("general", channels.first().id)
        assertEquals("Saved messages", channels.last().name)
    }

    @Test
    fun queryChannel_sendsHeadersAndParsesChannel() = runTest {
        val engine = MockEngine { request ->
            assertEquals("https://example.com/channels/messaging/general/query", request.url.toString())
            assertEquals("test-api-key", request.headers["X-Api-Key"])
            assertEquals("Bearer test-token", request.headers[HttpHeaders.Authorization])

            respond(
                content = """
                    {
                      "channel": {
                        "id": "general",
                        "type": "messaging",
                        "name": "General",
                        "member_count": 1,
                        "message_count": 1,
                        "members": [
                          {
                            "user": {
                              "id": "alice",
                              "name": "Alice"
                            }
                          }
                        ],
                        "messages": [
                          {
                            "id": "m1",
                            "cid": "messaging:general",
                            "text": "hello",
                            "created_at": "2026-05-30T10:00:00Z",
                            "user": {
                              "id": "alice",
                              "name": "Alice"
                            }
                          }
                        ]
                      }
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = io.ktor.http.headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

        val client = HttpClient(engine) {
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

        val api = KtorChatApi(
            baseUrl = "https://example.com",
            apiKey = "test-api-key",
            tokenProvider = { "test-token" },
            client = client,
        )

        val channel = api.queryChannel(
            channelType = "messaging",
            channelId = "general",
            query = WatchChannelRequest(),
        )

        assertEquals("general", channel.id)
        assertEquals("messaging", channel.type)
        assertEquals("General", channel.name)
        assertEquals(1, channel.memberCount)
        assertEquals(1, channel.messages.size)
        assertEquals("hello", channel.messages.first().text)
        assertNotNull(channel.members.firstOrNull())
        assertEquals("alice", channel.members.first().id)
    }

    @Test
    fun sendMessage_sendsHeadersAndParsesMessage() = runTest {
        val engine = MockEngine { request ->
            assertEquals("https://example.com/channels/messaging/general/messages", request.url.toString())
            assertEquals("test-api-key", request.headers["X-Api-Key"])
            assertEquals("Bearer test-token", request.headers[HttpHeaders.Authorization])

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

        val client = HttpClient(engine) {
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

        val api = KtorChatApi(
            baseUrl = "https://example.com",
            apiKey = "test-api-key",
            tokenProvider = { "test-token" },
            client = client,
        )

        val message = api.sendMessage(
            channelType = "messaging",
            channelId = "general",
            request = SendMessageRequest(
                text = "hello",
                clientMessageId = "client-1",
            ),
        )

        assertEquals("m1", message.id)
        assertEquals("hello", message.text)
        assertEquals("client-1", message.clientMessageId)
        assertEquals("alice", message.user.id)
    }

    @Test
    fun markRead_sendsHeadersAndReturnsSuccessfully() = runTest {
        val engine = MockEngine { request ->
            assertEquals("https://example.com/channels/messaging/general/read", request.url.toString())
            assertEquals("test-api-key", request.headers["X-Api-Key"])
            assertEquals("Bearer test-token", request.headers[HttpHeaders.Authorization])

            respond(
                content = "{}",
                status = HttpStatusCode.OK,
                headers = io.ktor.http.headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

        val client = HttpClient(engine) {
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

        val api = KtorChatApi(
            baseUrl = "https://example.com",
            apiKey = "test-api-key",
            tokenProvider = { "test-token" },
            client = client,
        )

        api.markRead(
            channelType = "messaging",
            channelId = "general",
            messageId = "m1",
        )
    }
}
