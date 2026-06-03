package org.example.project.v2.ui.screens.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.v2.client.ChatClient
import org.example.project.v2.core.models.Channel
import org.example.project.v2.core.models.ConnectionState
import org.example.project.v2.core.models.User
import org.example.project.v2.platform.taggedLogger

class ChatViewModel(
    private val chatClient: ChatClient,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val logger by taggedLogger(TAG)

    private val channelCid: String = requireNotNull(savedStateHandle["channelCid"]) {
        "channelCid is required"
    }
    val cid: String = channelCid
    private val channelState = chatClient.channelState(channelCid)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            channelState.channelData.collectLatest { channel ->
                reduce { copy(channel = channel) }
            }
        }
        viewModelScope.launch {
            channelState.loading.collectLatest { isLoading ->
                reduce { copy(isLoading = isLoading) }
            }
        }
        viewModelScope.launch {
            channelState.members.collectLatest { members ->
                reduce { copy(members = members) }
            }
        }
        viewModelScope.launch {
            chatClient.clientState.connectionState.collectLatest { state ->
                reduce { copy(connectionState = state) }
            }
        }
        watchChannel()
    }

    private fun watchChannel() {
        viewModelScope.launch {
            logger.d { "[watchChannel] cid=$channelCid" }
            runCatching {
                chatClient.channel(channelCid).watch()
            }.onFailure { throwable ->
                logger.e(throwable) { "[watchChannel] failed; cid=$channelCid" }
                reduce { copy(errorMessage = throwable.message) }
            }
        }
    }

    private inline fun reduce(block: ChatUiState.() -> ChatUiState) {
        _uiState.update { it.block() }
    }

    private companion object {
        const val TAG = "Chat:V2ChatViewModel"
    }
}

data class ChatUiState(
    val isLoading: Boolean = false,
    val channel: Channel? = null,
    val members: List<User> = emptyList(),
    val connectionState: ConnectionState = ConnectionState.OFFLINE,
    val errorMessage: String? = null,
)
