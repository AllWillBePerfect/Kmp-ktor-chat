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
import org.example.project.platform.AppLogger
import org.example.project.ui.screens.chat.ClientCommand
import org.example.project.ui.screens.chat.ClientProtocolJson
import org.example.project.ui.screens.chat.ServerEvent

class WsClient(
    private val client: HttpClient,
    private val pref: PreferencesDataSource,
    private val scope: CoroutineScope,
    private val logger: AppLogger,
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
        logger.i(TAG, "Connecting; host=${connectionPref.host} port=${connectionPref.port}")
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
            logger.i(TAG, "Connected; host=${settings.host} port=${settings.port}")
            startMessageCollection()
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Disconnected(e.message)
            logger.e(TAG, "Connection failed; host=${settings.host} port=${settings.port}", e)
            _events.emit(ServerEvent.ErrorEvent(message = "Connection failed: ${e.message}"))
            throw e
        }

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            pref.connectionSettingsFlow
                .drop(1)
                .collect { newSettings ->
                    if (newSettings != settings) {
                        logger.i(TAG, "Connection settings changed; old=${settings.host}:${settings.port} new=${newSettings.host}:${newSettings.port}")
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
                            logger.d(TAG, "Received event; type=${event.type}; payload=$event")
                            _events.emit(event)
                        } catch (e: Exception) {
                            logger.e(TAG, "Protocol error. Could not parse incoming frame; raw=$raw", e)
                        }
                    }
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    logger.w(TAG, "Connection closed unexpectedly; reason=${e.message}")
                    _events.emit(ServerEvent.ErrorEvent(message = "Connection closed: ${e.message}"))
                    _connectionState.value = ConnectionState.Disconnected(e.message)
                }
            } finally {
                if (_connectionState.value !is ConnectionState.Disconnected) {
                    _connectionState.value = ConnectionState.Disconnected()
                }
                logger.i(TAG, "Disconnected")
                session = null
            }
        }
    }

    private suspend fun reconnect(newSettings: ConnectionPrefModel) {
        logger.i(TAG, "Reconnecting; host=${newSettings.host} port=${newSettings.port}")
        closeInternal(cancelReconnectObserver = false)
        connect(newSettings)
    }

    override suspend fun sendMessage(text: String, chatId: String, clientMessageId: String?) {
        send(
            ClientCommand.SendMessageCommand(
                chatId = chatId,
                clientMessageId = clientMessageId,
                text = text
            )
        )
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
        logger.d(TAG, "Sending command; type=${command.type}")
        currentSession.send(Frame.Text(text))
    }

    override suspend fun close() {
        closeInternal(cancelReconnectObserver = true)
    }

    private suspend fun closeInternal(cancelReconnectObserver: Boolean) {
        _connectionState.value = ConnectionState.Idle
        logger.i(TAG, "Closing socket; cancelReconnectObserver=$cancelReconnectObserver")
        if (cancelReconnectObserver) {
            reconnectJob?.cancel()
            reconnectJob = null
        }
        messageCollectionJob?.cancel()
        messageCollectionJob = null
        session?.close()
        session = null
    }

    private companion object { const val TAG = "Chat:WsClient" }
}
