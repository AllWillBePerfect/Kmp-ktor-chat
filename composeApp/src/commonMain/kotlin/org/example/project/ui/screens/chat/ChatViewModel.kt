package org.example.project.ui.screens.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.data.PreferencesDataSource
import org.example.project.domain.model.ChatMessage
import org.example.project.domain.model.User
import org.example.project.ktor.WsClientContract
import org.example.project.ktor.isConnected
import org.example.project.ui.components.message.Message
import org.example.project.ui.screens.chat.ServerEvent.*

class ChatViewModel(
    private val wsClient: WsClientContract,
    private val pref: PreferencesDataSource,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        println("ChatViewModel: Caught exception: ${throwable.message}")
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    private var currentUserId: String? = null
    
    private val currentChatId: String = requireNotNull(savedStateHandle.get<String>("chatId")) {
        "chatId is required but was not provided to ChatViewModel"
    }

    init {
        viewModelScope.launch(exceptionHandler) {
            pref.userDataFlow.collect { userData ->
                currentUserId = userData.userId
            }
        }
        
        viewModelScope.launch(exceptionHandler) {
            wsClient.events.collect { event ->
                handleEvent(event)
            }
        }

        // Реактивное обновление данных при восстановлении связи
        viewModelScope.launch(exceptionHandler) {
            wsClient.connectionState.collect { state ->
                if (state.isConnected) {
                    println("ChatViewModel: Connection restored, re-syncing data for $currentChatId")
                    wsClient.requestUsers()
                    wsClient.requestChats()
                    wsClient.requestMessages(currentChatId)
                } else {
                    // Если связь потеряна, можно очистить список онлайн-пользователей
                    reduce { copy(onlineUsers = emptyList()) }
                }
            }
        }
    }

    fun onAction(action: ChatUiAction) {
        when (action) {
            is ChatUiAction.TextTyped -> reduce { copy(userText = action.text) }
            ChatUiAction.MessageSent -> sendMessage(uiState.value.userText)
            is ChatUiAction.MessageSentWithText -> sendMessage(action.text)
            ChatUiAction.Connect -> Unit 
            ChatUiAction.Disconnect -> Unit
        }
    }

    private fun sendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch(exceptionHandler) {
            wsClient.sendMessage(text = text, chatId = currentChatId)
        }
        reduce { copy(userText = "") }
    }

    private inline fun reduce(reducer: ChatUiState.() -> ChatUiState) {
        _uiState.update { it.reducer() }
    }

    private fun handleEvent(event: ServerEvent) {
        when (event) {
            is MessageCreatedEvent -> {
                if (event.message.chatId == currentChatId) {
                    val message = mapToUiMessage(event.message)
                    reduce { copy(messages = messages + message) }
                }
            }
            is MessagesSnapshotEvent -> {
                val snapshotMessages = event.messages.map { mapToUiMessage(it) }
                reduce { copy(messages = snapshotMessages) }
            }
            is UsersSnapshotEvent -> {
                reduce { copy(onlineUsers = event.users) }
            }
            is UserJoinedEvent -> {
                reduce { copy(onlineUsers = (onlineUsers + event.user).distinctBy { it.id }) }
            }
            is UserLeftEvent -> {
                reduce { copy(onlineUsers = onlineUsers.filter { it.id != event.user.id }) }
            }
            is ErrorEvent -> {
                val systemMessage = Message(
                    author = "System",
                    content = "Error: ${event.message}",
                    timestamp = ""
                )
                reduce { copy(messages = messages + systemMessage) }
            }
            else -> Unit
        }
    }

    private fun mapToUiMessage(chatMessage: ChatMessage): Message {
        val isMe = chatMessage.senderId == currentUserId
        return Message(
            author = if (isMe) "me" else chatMessage.senderName,
            content = chatMessage.text,
            timestamp = chatMessage.createdAt.takeLast(5)
        )
    }
}

data class ChatUiState(
    val userText: String = "",
    val messages: List<Message> = emptyList(),
    val onlineUsers: List<User> = emptyList()
) {
    val isButtonEnabled: Boolean = userText.isNotBlank()
    val getReversedMessages = messages.asReversed()
    val onlineCountText: String = "${onlineUsers.size} users online"
}

sealed interface ChatUiAction {
    data class TextTyped(val text: String) : ChatUiAction
    data object MessageSent : ChatUiAction
    data class MessageSentWithText(val text: String) : ChatUiAction
    data object Connect : ChatUiAction
    data object Disconnect : ChatUiAction
}
