package org.example.project.v2.ui

import kotlinx.serialization.Serializable

sealed interface V2AppRoutes {
    @Serializable
    data object Login

    @Serializable
    data object Chats

    @Serializable
    data class Chat(val channelCid: String)

    @Serializable
    data object Settings
}
