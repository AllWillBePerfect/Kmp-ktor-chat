package org.example.project.ui.screens.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.data.PreferencesDataSource
import org.example.project.domain.model.ChatRoom
import org.example.project.ktor.WsClientContract
import org.example.project.ktor.isConnected
import org.example.project.ui.screens.chat.ServerEvent

class ChatsViewModel(
    private val wsClient: WsClientContract,
    private val pref: PreferencesDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatsUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<ChatsUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        // Подгружаем ID текущего пользователя для правильного отображения имен в Direct чатах
        viewModelScope.launch {
            pref.userDataFlow.collect { userData ->
                _uiState.update { it.copy(currentUserId = userData.userId) }
            }
        }

        // Реактивно запрашиваем список чатов при каждом (восстановлении) соединения
        viewModelScope.launch {
            wsClient.connectionState.collect { state ->
                if (state.isConnected) {
                    println("ChatsViewModel: Connection restored, fetching chats")
                    wsClient.requestChats()
                }
            }
        }

        viewModelScope.launch {
            wsClient.events.collect { event ->
                when (event) {
                    is ServerEvent.ChatsSnapshotEvent -> {
                        _uiState.update { it.copy(items = event.chats) }
                    }
                    is ServerEvent.ChatCreatedEvent -> {
                        _uiState.update { it.copy(items = (it.items + event.chat).distinctBy { room -> room.id }) }
                        sendEvent(ChatsUiEvent.OnItemClicked(event.chat.id))
                    }
                    else -> Unit
                }
            }
        }
    }

    fun onAction(action: ChatsUiAction) {
        when (action) {
            is ChatsUiAction.TextTyped -> {
                reduce { copy(userText = action.text) }
            }

            ChatsUiAction.SubmitClicked -> {
                if (uiState.value.userText.isNotBlank()) {
                    viewModelScope.launch {
                        wsClient.createChat(uiState.value.userText)
                        reduce { copy(userText = "") }
                    }
                }
            }

            is ChatsUiAction.OnItemClicked -> {
                sendEvent(ChatsUiEvent.OnItemClicked(action.chatId))
            }

            ChatsUiAction.OnSettingsClicked -> {
                sendEvent(ChatsUiEvent.OnSettingsClicked)
            }
        }
    }

    private inline fun reduce(
        reducer: ChatsUiState.() -> ChatsUiState
    ) {
        _uiState.update { it.reducer() }
    }

    private fun sendEvent(
        event: ChatsUiEvent
    ) = viewModelScope.launch {
        _uiEvent.emit(event)
    }
}

data class ChatsUiState(
    val userText: String = "",
    val items: List<ChatRoom> = emptyList(),
    val currentUserId: String? = null
)

sealed interface ChatsUiAction {
    data class TextTyped(val text: String) : ChatsUiAction
    data object SubmitClicked : ChatsUiAction
    data class OnItemClicked(val chatId: String) : ChatsUiAction
    data object OnSettingsClicked : ChatsUiAction
}

sealed interface ChatsUiEvent {
    data class OnItemClicked(val chatId: String) : ChatsUiEvent
    data object OnSettingsClicked : ChatsUiEvent
}
