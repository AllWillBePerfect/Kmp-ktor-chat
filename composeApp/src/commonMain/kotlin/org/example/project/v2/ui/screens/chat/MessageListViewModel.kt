package org.example.project.v2.ui.screens.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import org.example.project.v2.client.ChatClient
import org.example.project.v2.ui.common.feature.messages.list.MessageListController
import org.example.project.v2.ui.common.state.messages.list.MessageListState

class MessageListViewModel(
    chatClient: ChatClient,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val channelCid: String = requireNotNull(savedStateHandle["channelCid"]) {
        "channelCid is required"
    }

    private val controller = MessageListController(
        cid = channelCid,
        chatClient = chatClient,
        channelState = chatClient.channelState(channelCid),
        scope = viewModelScope,
    )

    val state: StateFlow<MessageListState> = controller.state

    override fun onCleared() {
        controller.onCleared()
        super.onCleared()
    }
}
