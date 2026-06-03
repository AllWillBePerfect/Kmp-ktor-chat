package org.example.project.v2.client.network

interface NetworkStateProvider {
    fun isConnected(): Boolean

    fun subscribe(listener: NetworkStateListener) = Unit

    fun unsubscribe(listener: NetworkStateListener) = Unit

    interface NetworkStateListener {
        suspend fun onConnected()

        suspend fun onDisconnected()
    }
}
