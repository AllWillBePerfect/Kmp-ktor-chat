package org.example.project.v2.client.channel

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
import kotlin.test.Test
import kotlin.test.assertEquals

class ChannelClientTest {

    @Test
    fun watch_withoutParams_returnsChannelFromChatClientQueryPath() = runTest {
        val engine = MockEngine { request ->
            assertEquals("https://example.com/channels/messaging/general/query", request.url.toString())
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

        val channel = client.channel("messaging", "general").watch()

        assertEquals("general", channel?.id)
        assertEquals("messaging", channel?.type)
        assertEquals("General", channel?.name)
    }
}
