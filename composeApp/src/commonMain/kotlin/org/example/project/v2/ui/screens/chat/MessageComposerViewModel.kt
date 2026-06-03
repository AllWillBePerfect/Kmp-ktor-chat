package org.example.project.v2.ui.screens.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import org.example.project.v2.client.ChatClient
import org.example.project.v2.ui.common.feature.messages.composer.MessageComposerController
import org.example.project.v2.ui.common.state.messages.composer.MessageComposerState

class MessageComposerViewModel(
    chatClient: ChatClient,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val channelCid: String = requireNotNull(savedStateHandle["channelCid"]) {
        "channelCid is required"
    }

    private val controller = MessageComposerController(
        channelCid = channelCid,
        chatClient = chatClient,
        scope = viewModelScope,
    )

    val state: StateFlow<MessageComposerState> = controller.state

    fun setMessageInput(input: String) {
        controller.setMessageInput(input)
    }

    fun sendMessage() {
        controller.sendMessage()
    }

    override fun onCleared() {
        controller.onCleared()
        super.onCleared()
    }
}
