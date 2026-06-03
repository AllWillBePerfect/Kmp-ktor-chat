package org.example.project.v2.client.socket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.example.project.v2.client.events.ChatEvent
import org.example.project.v2.client.events.ConnectedEvent
import org.example.project.v2.client.events.ConnectingEvent
import org.example.project.v2.client.events.ConnectionErrorEvent
import org.example.project.v2.client.events.DisconnectedEvent
import org.example.project.v2.platform.taggedLogger

internal open class ChatSocket(
    private val socketFactory: SocketFactory,
    private val scope: CoroutineScope,
) {
    private val logger by taggedLogger(TAG)
    private val listeners = mutableSetOf<SocketListener>()
    private val _events = MutableSharedFlow<ChatEvent>(extraBufferCapacity = Int.MAX_VALUE)
    val events: SharedFlow<ChatEvent> = _events.asSharedFlow()

    private var createSocketJob: Job? = null
    private var listenSocketJob: Job? = null
    private var reconnectJob: Job? = null
    private var streamWebSocket: StreamWebSocket? = null
    private var connectionConf: SocketFactory.ConnectionConf? = null
    private var isManualDisconnect: Boolean = false
    private var reconnectAttempt: Int = 0

    fun subscribe(listener: SocketListener) {
        listeners += listener
    }

    fun unsubscribe(listener: SocketListener) {
        listeners -= listener
    }

    fun connect(connectionConf: SocketFactory.ConnectionConf) {
        if (createSocketJob?.isActive == true || listenSocketJob?.isActive == true) return
        this.connectionConf = connectionConf
        isManualDisconnect = false
        reconnectAttempt = 0
        logger.d { "[connect] endpoint=${connectionConf.endpoint}" }
        listeners.forEach { it.onConnecting() }
        startSocket(connectionConf)
    }

    fun disconnect() {
        logger.d { "[disconnect] no args" }
        isManualDisconnect = true
        reconnectJob?.cancel()
        reconnectJob = null
        createSocketJob?.cancel()
        createSocketJob = null
        listenSocketJob?.cancel()
        listenSocketJob = null
        streamWebSocket?.close()
        streamWebSocket = null
    }

    suspend fun send(text: String) {
        streamWebSocket?.send(text)
    }

    private fun startSocket(connectionConf: SocketFactory.ConnectionConf) {
        createSocketJob = scope.launch {
            runCatching {
                socketFactory.createSocket(scope, connectionConf)
            }.onSuccess { socket ->
                reconnectAttempt = 0
                streamWebSocket = socket
                listenSocketJob?.cancel()
                listenSocketJob = socket.listen().onEach { socketEvent ->
                    when (socketEvent) {
                        is StreamWebSocketEvent.Error -> {
                            logger.e(socketEvent.throwable) { "[connect] socket error" }
                            listeners.forEach { it.onError(socketEvent.throwable) }
                            handleConnectionLoss()
                        }
                        is StreamWebSocketEvent.Closed -> {
                            logger.w { "[connect] socket closed" }
                            listeners.forEach { it.onDisconnected("socket_closed") }
                            handleConnectionLoss()
                        }
                        is StreamWebSocketEvent.Message -> {
                            handleEvent(socketEvent.chatEvent)
                        }
                    }
                }.launchIn(scope)
            }.onFailure { throwable ->
                logger.e(throwable) { "[connect] failed to create socket" }
                listeners.forEach { it.onError(throwable) }
                handleConnectionLoss()
            }.also {
                createSocketJob = null
            }
        }
    }

    private fun handleConnectionLoss() {
        listenSocketJob?.cancel()
        listenSocketJob = null
        createSocketJob?.cancel()
        createSocketJob = null
        streamWebSocket?.close()
        streamWebSocket = null
        if (isManualDisconnect) return
        val conf = connectionConf ?: return
        if (reconnectJob?.isActive == true) return
        val delayMs = reconnectDelayForAttempt(reconnectAttempt++)
        reconnectJob = scope.launch {
            logger.d { "[reconnect] scheduled in ${delayMs}ms" }
            delay(delayMs)
            if (isManualDisconnect) return@launch
            listeners.forEach { it.onConnecting() }
            startSocket(conf)
        }
    }

    private suspend fun handleEvent(event: ChatEvent) {
        logger.d { "[handleEvent] event.type=${event.type}" }
        _events.emit(event)

        when (event) {
            is ConnectingEvent -> listeners.forEach { it.onConnecting() }
            is ConnectedEvent -> listeners.forEach { it.onConnected(event) }
            is DisconnectedEvent -> {
                listeners.forEach { it.onDisconnected(event.disconnectCause) }
                handleConnectionLoss()
            }
            is ConnectionErrorEvent -> listeners.forEach {
                it.onError(IllegalStateException(event.errorMessage))
            }
            else -> listeners.forEach { it.onEvent(event) }
        }
    }

    private companion object {
        const val TAG = "Chat:Socket"

        fun reconnectDelayForAttempt(attempt: Int): Long {
            return when (attempt) {
                0 -> 1_000L
                1 -> 2_000L
                else -> 5_000L
            }
        }
    }
}
