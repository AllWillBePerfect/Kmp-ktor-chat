package org.example.project.ui.screens.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ConversationViewModel : ViewModel() {



    private val _uiState = MutableStateFlow(ConversationUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<ConversationUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun onAction(action: ConversationUiAction) {
        when (action) {
            is ConversationUiAction.TextTyped -> {
                reduce { copy(userText = action.text) }
            }

            ConversationUiAction.SubmitClicked -> {
                reduce {
                    copy(
                        userText = ""
                    )
                }
            }

            ConversationUiAction.OnItemClicked -> {

            }

            ConversationUiAction.OnBackPressed -> sendEvent(ConversationUiEvent.OnBackPressed)
        }
    }

    private inline fun reduce(
        reducer: ConversationUiState.() -> ConversationUiState
    ) {
        _uiState.update { it.reducer() }
    }

    private fun sendEvent(
        event: ConversationUiEvent
    ) = viewModelScope.launch {
        _uiEvent.emit(event)
    }
}

data class ConversationUiState(
    val userText: String = "",
)

sealed interface ConversationUiAction {

    data class TextTyped(
        val text: String
    ) : ConversationUiAction

    data object SubmitClicked : ConversationUiAction

    data object OnItemClicked : ConversationUiAction
    data object OnBackPressed : ConversationUiAction

}

sealed interface ConversationUiEvent {

    data object OnBackPressed : ConversationUiEvent
}
