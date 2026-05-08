package org.example.project.ktor

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.http.HttpMethod
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import org.example.project.ui.screens.chat.ClientCommand
import org.example.project.ui.screens.chat.ClientProtocolJson
import org.example.project.ui.screens.chat.RenameUserCommand
import org.example.project.ui.screens.chat.RequestUsersCommand
import org.example.project.ui.screens.chat.SendMessageCommand
import org.example.project.ui.screens.chat.ServerEvent

class WsClient(private val client: HttpClient) {
    var session: WebSocketSession? = null

    suspend fun connect() {
        session = client.webSocketSession(
            method = HttpMethod.Get,
            host = "192.168.1.237",
            port = 8080,
            path = "/ws"
        )
    }

    suspend fun sendMessage(text: String, chatId: String = "general") {
        send(
            SendMessageCommand(
                chatId = chatId,
                text = text
            )
        )
    }

    suspend fun renameUser(name: String) {
        send(
            RenameUserCommand(
                name = name
            )
        )
    }

    suspend fun requestUsers() {
        send(RequestUsersCommand())
    }

    private suspend fun send(command: ClientCommand) {
        val text = ClientProtocolJson.encodeClientCommand(command)
        session?.send(Frame.Text(text))
    }

    suspend fun receive(onReceive: (event: ServerEvent) -> Unit) {
        val currentSession = session ?: return

        for (frame in currentSession.incoming) {
            if (frame is Frame.Text) {
                val raw = frame.readText()
                println("raw: $raw")
                val event = ClientProtocolJson.decodeServerEvent(raw)
                onReceive(event)
            }
        }
    }

    suspend fun close() {
        session?.close()
        session = null
    }

}
