package org.example.project.v2.client.socket

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CoroutineScope
import org.example.project.v2.client.parser.ChatParser
import org.example.project.v2.core.models.User
import org.example.project.v2.platform.taggedLogger

internal class SocketFactory(
    private val parser: ChatParser,
    private val client: HttpClient,
) {
    private val logger by taggedLogger("Chat:SocketFactory")

    suspend fun createSocket(
        scope: CoroutineScope,
        connectionConf: ConnectionConf,
    ): StreamWebSocket {
        logger.i { "new web socket: ${connectionConf.endpoint}" }
        val session = client.webSocketSession(
            urlString = connectionConf.endpoint,
        ) {
            connectionConf.token?.let {
                header(HttpHeaders.Authorization, "Bearer $it")
            }
        }
        return StreamWebSocket(
            parser = parser,
            session = session,
            scope = scope,
        )
    }

    sealed class ConnectionConf {
        abstract val endpoint: String
        abstract val apiKey: String
        abstract val user: User
        abstract val token: String?

        data class UserConnectionConf(
            override val endpoint: String,
            override val apiKey: String,
            override val user: User,
            override val token: String?,
        ) : ConnectionConf()
    }
}
