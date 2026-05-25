package org.example.project.ui

import kotlinx.serialization.Serializable

sealed interface AppRoutes {

    @Serializable
    data object Chats

    @Serializable
    data object Login

    @Serializable
    data class Chat(val chatId: String)

    @Serializable
    data object Settings
}
