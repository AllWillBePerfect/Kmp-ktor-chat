package org.example.project.ktor

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.example.project.ui.screens.chat.ServerEvent

interface WsClientContract {
    val events: SharedFlow<ServerEvent>
    val connectionState: StateFlow<ConnectionState>

    suspend fun connect()
    suspend fun sendMessage(text: String, chatId: String = "general", clientMessageId: String? = null)
    suspend fun renameUser(name: String)
    suspend fun requestUsers()
    suspend fun requestChats()
    suspend fun requestMessages(chatId: String = "general")
    suspend fun createChat(title: String)
    suspend fun addChatMember(chatId: String, userId: String)
    suspend fun createDirectChat(userId: String)
    suspend fun close()
}
