package org.example.project.ktor

val ConnectionState.isIdle: Boolean
    get() = this is ConnectionState.Idle

val ConnectionState.isConnecting: Boolean
    get() = this is ConnectionState.Connecting

val ConnectionState.isConnected: Boolean
    get() = this is ConnectionState.Connected

val ConnectionState.isDisconnected: Boolean
    get() = this is ConnectionState.Disconnected
