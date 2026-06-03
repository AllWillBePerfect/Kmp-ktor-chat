package org.example.project.v2.client.setup.state.internal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.example.project.v2.client.network.NetworkStateProvider
import org.example.project.v2.client.setup.state.ClientState
import org.example.project.v2.core.models.ConnectionState
import org.example.project.v2.core.models.InitializationState
import org.example.project.v2.core.models.User
import org.example.project.v2.platform.taggedLogger

internal class MutableClientState(
    private val networkStateProvider: NetworkStateProvider,
) : ClientState {

    private val logger by taggedLogger("Chat:ClientState")

    private val _initializationState = MutableStateFlow(InitializationState.NOT_INITIALIZED)
    private val _connectionState = MutableStateFlow(ConnectionState.OFFLINE)
    private val _user = MutableStateFlow<User?>(null)

    override val initializationState: StateFlow<InitializationState> = _initializationState
    override val user: StateFlow<User?> = _user
    override val connectionState: StateFlow<ConnectionState> = _connectionState

    override val isOnline: Boolean
        get() = _connectionState.value == ConnectionState.CONNECTED

    override val isOffline: Boolean
        get() = _connectionState.value == ConnectionState.OFFLINE

    override val isConnecting: Boolean
        get() = _connectionState.value == ConnectionState.CONNECTING

    override val isNetworkAvailable: Boolean
        get() = networkStateProvider.isConnected()

    fun clearState() {
        logger.d { "[clearState] no args" }
        _initializationState.value = InitializationState.NOT_INITIALIZED
        _connectionState.value = ConnectionState.OFFLINE
        _user.value = null
    }

    fun setConnectionState(connectionState: ConnectionState) {
        logger.d { "[setConnectionState] state: $connectionState" }
        _connectionState.value = connectionState
    }

    fun setInitializationState(state: InitializationState) {
        _initializationState.value = state
    }

    fun setUser(user: User) {
        _user.value = user
    }
}
