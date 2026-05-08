package org.example.project.ui.screens.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.ui.screens.chat.ChatUiAction

class ChatsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChatsUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<ChatsUiEvent>(replay = 1)
    val uiEvent = _uiEvent.asSharedFlow()

    fun onAction(action: ChatsUiAction) {
        when (action) {
            is ChatsUiAction.TextTyped -> {
                reduce { copy(userText = action.text) }
            }

            ChatsUiAction.SubmitClicked -> {
                reduce {
                    copy(
                        items = items + userText,
                        userText = ""
                    )
                }
            }

            ChatsUiAction.OnItemClicked -> {
                sendEvent(ChatsUiEvent.OnItemClicked)
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
    val items: List<String> = emptyList()
)

sealed interface ChatsUiAction {

    data class TextTyped(
        val text: String
    ) : ChatsUiAction

    data object SubmitClicked : ChatsUiAction

    data object OnItemClicked : ChatsUiAction
}

sealed interface ChatsUiEvent {

    data object OnItemClicked : ChatsUiEvent
}