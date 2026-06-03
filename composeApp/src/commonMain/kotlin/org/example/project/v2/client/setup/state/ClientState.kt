package org.example.project.v2.client.setup.state

import kotlinx.coroutines.flow.StateFlow
import org.example.project.v2.core.models.ConnectionState
import org.example.project.v2.core.models.InitializationState
import org.example.project.v2.core.models.User

interface ClientState {
    val initializationState: StateFlow<InitializationState>
    val user: StateFlow<User?>
    val connectionState: StateFlow<ConnectionState>
    val isOnline: Boolean
    val isOffline: Boolean
    val isConnecting: Boolean
    val isNetworkAvailable: Boolean
}
