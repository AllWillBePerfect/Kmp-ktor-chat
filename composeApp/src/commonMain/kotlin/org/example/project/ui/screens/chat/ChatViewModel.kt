package org.example.project.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSocketException
import io.ktor.client.plugins.websocket.WebSockets
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.ktor.WsClient

class ChatViewModel : ViewModel() {

    private val wsClient = WsClient(HttpClient {
        install(WebSockets)
    })

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()


    init {
        viewModelScope.launch { initConnection(wsClient) }
    }

    fun onAction(action: ChatUiAction) {
        when (action) {
            is ChatUiAction.TextTyped -> {
                reduce { copy(userText = action.text) }
            }

            ChatUiAction.MessageSent -> {
                viewModelScope.launch {
                    if (uiState.value.userText.startsWith("/user")) {
                        wsClient.renameUser("desktop")
                    } else {
                        wsClient.sendMessage(text = uiState.value.userText)
                    }
                }
//                reduce { copy(messages = messages + userText, userText = "") }
            }

        }
    }

    private inline fun reduce(
        reducer: ChatUiState.() -> ChatUiState
    ) {
        _uiState.update { it.reducer() }
    }

    suspend fun initConnection(wsClient: WsClient) {
        try {
            wsClient.connect()
            wsClient.receive(::writeMessage)
        } catch (e: Exception) {
            when (e) {
                is ClosedReceiveChannelException -> writeMessage(ErrorEvent(message = "Disconnected. ${e.message}"))
                is WebSocketException -> writeMessage(ErrorEvent(message = "Unable to connect."))
                else -> writeMessage(ErrorEvent(message = "Unexpected error: ${e.message}"))
            }
            wsClient.close()
            delay(5000)
            viewModelScope.launch { initConnection(wsClient) }
        }
    }

    private fun writeMessage(serverEvent: ServerEvent) {
        reduce { copy(messages = messages + parseEvent(serverEvent), userText = "") }
    }

    override fun onCleared() {
        super.onCleared()
        println("ChatViewModel onCleared")
    }

    private fun parseEvent(event: ServerEvent): String {
       return when (event) {
            is MessageCreatedEvent -> {
                "${event.message.senderName}: ${event.message.text}"
            }

            is UserJoinedEvent -> {
                "${event.user.name} joined"
            }

            is UserLeftEvent -> {
                "${event.user.name} left"
            }

            is UserRenamedEvent -> {
                "${event.oldName} renamed to ${event.user.name}"
            }

            is UsersSnapshotEvent -> {
                "Users: ${event.users}"
            }

            is MessagesSnapshotEvent -> {
//                reduce { copy(messages = messages + event.messages.map { it.text }, userText = "") }
                "History: ${event.messages}"
            }

            is ErrorEvent -> {
                "Error: ${event.message}"
            }

           is ChatsSnapshotEvent -> {
               "Chats: ${event.chats}"
           }

           is ChatCreatedEvent -> {
               "Chat Created: ${event.chat}"
           }
       }
    }

}

data class ChatUiState(
    val userText: String = "",
    val messages: List<String> = emptyList()
) {
    val isButtonEnabled: Boolean = userText.isNotEmpty()
}

sealed interface ChatUiAction {
    data class TextTyped(val text: String) : ChatUiAction
    data object MessageSent : ChatUiAction
}


