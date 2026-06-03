package org.example.project.v2.client.di

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import org.example.project.v2.client.api.ChatApi
import org.example.project.v2.client.api.internal.TokenProvider
import org.example.project.v2.client.api2.KtorChatApi
import org.example.project.v2.client.network.NetworkStateProvider
import org.example.project.v2.client.parser.KotlinxChatParser
import org.example.project.v2.client.socket.ChatSocket
import org.example.project.v2.client.socket.SocketFactory

internal class ChatModule(
    private val apiKey: String,
    private val baseUrl: String,
    private val clientScope: CoroutineScope,
    private val networkStateProvider: NetworkStateProvider,
    private val customHttpClient: HttpClient? = null,
) {
    private val tokenProvider: TokenProvider = TokenProvider()

    private val parser: KotlinxChatParser by lazy { KotlinxChatParser() }

    private val httpClient: HttpClient by lazy {
        customHttpClient ?: HttpClient {
            install(WebSockets)
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
    }

    val socketFactory: SocketFactory by lazy {
        SocketFactory(
            parser = parser,
            client = httpClient,
        )
    }

    val chatApi: ChatApi by lazy {
        KtorChatApi(
            baseUrl = baseUrl,
            apiKey = apiKey,
            tokenProvider = { tokenProvider.token },
            client = httpClient,
        )
    }

    val chatSocket: ChatSocket by lazy {
        ChatSocket(
            socketFactory = socketFactory,
            scope = clientScope,
        )
    }

    fun networkStateProvider(): NetworkStateProvider = networkStateProvider

    fun tokenProvider(): TokenProvider = tokenProvider
}
