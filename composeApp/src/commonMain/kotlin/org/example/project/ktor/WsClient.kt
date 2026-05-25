package org.example.project.ktor

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.example.project.data.PreferencesDataSource
import org.example.project.data.models.ConnectionPrefModel
import org.example.project.ui.screens.chat.ClientCommand
import org.example.project.ui.screens.chat.ClientProtocolJson
import org.example.project.ui.screens.chat.ServerEvent

class WsClient(
    private val client: HttpClient,
    private val pref: PreferencesDataSource,
    private val scope: CoroutineScope
) : WsClientContract {
    private var session: WebSocketSession? = null
    private var reconnectJob: Job? = null
    private var messageCollectionJob: Job? = null

    private val _events = MutableSharedFlow<ServerEvent>(replay = 1)
    override val events: SharedFlow<ServerEvent> = _events.asSharedFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    override suspend fun connect() {
        if (session != null) return
        _connectionState.value = ConnectionState.Connecting
        val connectionPref = pref.connectionSettingsFlow.first()
        connect(connectionPref)
    }

    private suspend fun connect(settings: ConnectionPrefModel) {
        val userData = pref.userDataFlow.first()
        val token = userData.token

        try {
            session = client.webSocketSession(
                method = HttpMethod.Get,
                host = settings.host,
                port = settings.port,
                path = "/ws"
            ) {
                token?.let {
                    header(HttpHeaders.Authorization, "Bearer $it")
                }
            }
            _connectionState.value = ConnectionState.Connected
            startMessageCollection()
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Disconnected(e.message)
            _events.emit(ServerEvent.ErrorEvent(message = "Connection failed: ${e.message}"))
            throw e
        }

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            pref.connectionSettingsFlow
                .drop(1)
                .collect { newSettings ->
                    if (newSettings != settings) {
                        reconnect(newSettings)
                    }
                }
        }
    }

    private fun startMessageCollection() {
        messageCollectionJob?.cancel()
        messageCollectionJob = scope.launch {
            val currentSession = session ?: return@launch
            try {
                for (frame in currentSession.incoming) {
                    if (frame is Frame.Text) {
                        val raw = frame.readText()
                        if (raw.isBlank()) continue
                        try {
                            val event = ClientProtocolJson.decodeServerEvent(raw)
                            _events.emit(event)
                        } catch (e: Exception) {
                            println("WsClient: Protocol Error. Could not parse: '$raw'. Error: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    _events.emit(ServerEvent.ErrorEvent(message = "Connection closed: ${e.message}"))
                    _connectionState.value = ConnectionState.Disconnected(e.message)
                }
            } finally {
                if (_connectionState.value !is ConnectionState.Disconnected) {
                    _connectionState.value = ConnectionState.Disconnected()
                }
                session = null
            }
        }
    }

    private suspend fun reconnect(newSettings: ConnectionPrefModel) {
        closeInternal(cancelReconnectObserver = false)
        connect(newSettings)
    }

    override suspend fun sendMessage(text: String, chatId: String) {
        send(ClientCommand.SendMessageCommand(chatId = chatId, text = text))
    }

    override suspend fun renameUser(name: String) {
        send(ClientCommand.RenameUserCommand(name = name))
    }

    override suspend fun requestUsers() {
        send(ClientCommand.RequestUsersCommand())
    }

    override suspend fun requestChats() {
        send(ClientCommand.RequestChatsCommand())
    }

    override suspend fun requestMessages(chatId: String) {
        send(ClientCommand.RequestMessagesCommand(chatId = chatId))
    }

    override suspend fun createChat(title: String) {
        send(ClientCommand.CreateChatCommand(title = title))
    }

    override suspend fun addChatMember(chatId: String, userId: String) {
        send(ClientCommand.AddChatMemberCommand(chatId = chatId, userId = userId))
    }

    override suspend fun createDirectChat(userId: String) {
        send(ClientCommand.CreateDirectChatCommand(userId = userId))
    }

    private suspend fun send(command: ClientCommand) {
        val currentSession = session ?: throw IllegalStateException("WebSocket is not connected")
        val text = ClientProtocolJson.encodeClientCommand(command)
        currentSession.send(Frame.Text(text))
    }

    override suspend fun close() {
        closeInternal(cancelReconnectObserver = true)
    }

    private suspend fun closeInternal(cancelReconnectObserver: Boolean) {
        _connectionState.value = ConnectionState.Idle
        if (cancelReconnectObserver) {
            reconnectJob?.cancel()
            reconnectJob = null
        }
        messageCollectionJob?.cancel()
        messageCollectionJob = null
        session?.close()
        session = null
    }
}
