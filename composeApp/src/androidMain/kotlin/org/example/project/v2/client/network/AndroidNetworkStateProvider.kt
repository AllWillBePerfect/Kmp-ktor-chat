package org.example.project.v2.client.network

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.example.project.v2.platform.taggedLogger
import java.util.concurrent.atomic.AtomicBoolean

class AndroidNetworkStateProvider(
    private val scope: CoroutineScope,
    private val connectivityManager: ConnectivityManager,
) : NetworkStateProvider {
    private val logger by taggedLogger("Chat:NetworkStateProvider")
    private val lock: Any = Any()
    private val availableNetworks: MutableSet<Network> = mutableSetOf()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            availableNetworks.add(network)
            notifyListenersIfNetworkStateChanged()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            notifyListenersIfNetworkStateChanged()
        }

        override fun onLost(network: Network) {
            availableNetworks.remove(network)
            notifyListenersIfNetworkStateChanged()
            if (availableNetworks.isEmpty()) {
                listeners.onDisconnected()
            }
        }
    }

    @Volatile
    private var connected: Boolean = isConnected()

    @Volatile
    private var listeners: Set<NetworkStateProvider.NetworkStateListener> = setOf()

    private val isRegistered = AtomicBoolean(false)

    override fun isConnected(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                connectivityManager.run {
                    getNetworkCapabilities(activeNetwork)?.run {
                        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    }
                }
            }.getOrNull() ?: false
        } else {
            connectivityManager.activeNetworkInfo?.isConnected ?: false
        }
    }

    override fun subscribe(listener: NetworkStateProvider.NetworkStateListener) {
        synchronized(lock) {
            listeners = listeners + listener
            if (isRegistered.compareAndSet(false, true)) {
                connectivityManager.registerNetworkCallback(NetworkRequest.Builder().build(), callback)
            }
        }
    }

    override fun unsubscribe(listener: NetworkStateProvider.NetworkStateListener) {
        synchronized(lock) {
            listeners = (listeners - listener).also {
                if (it.isEmpty() && isRegistered.compareAndSet(true, false)) {
                    connectivityManager.unregisterNetworkCallback(callback)
                }
            }
        }
    }

    private fun notifyListenersIfNetworkStateChanged() {
        val isNowConnected = isConnected()
        if (!connected && isNowConnected) {
            logger.i { "Network connected." }
            connected = true
            listeners.onConnected()
        } else if (connected && !isNowConnected) {
            logger.i { "Network disconnected." }
            connected = false
            listeners.onDisconnected()
        }
    }

    private fun Set<NetworkStateProvider.NetworkStateListener>.onConnected() {
        scope.launch {
            forEach { it.onConnected() }
        }
    }

    private fun Set<NetworkStateProvider.NetworkStateListener>.onDisconnected() {
        scope.launch {
            forEach { it.onDisconnected() }
        }
    }
}
