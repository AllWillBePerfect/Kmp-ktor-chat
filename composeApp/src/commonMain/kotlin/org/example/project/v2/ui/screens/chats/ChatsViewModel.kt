package org.example.project.v2.ui.screens.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.example.project.v2.client.ChatClient
import org.example.project.v2.client.api.state.ChannelsStateData
import org.example.project.v2.client.api.models.QueryChannelsRequest
import org.example.project.v2.core.models.Channel
import org.example.project.v2.core.models.NeutralFilterObject
import org.example.project.v2.platform.taggedLogger

class ChatsViewModel(
    private val chatClient: ChatClient,
) : ViewModel() {
    private val logger by taggedLogger(TAG)
    private var initialRefreshDone = false
    private val queryState by lazy { chatClient.queryChannelsState(NeutralFilterObject) }

    private val _uiState = MutableStateFlow(ChatsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            queryState.channels.collectLatest { channels ->
                logger.d { "[channels] count=${channels.orEmpty().size}" }
                reduce { copy(items = channels.orEmpty()) }
            }
        }
        viewModelScope.launch {
            queryState.channelsStateData.collectLatest { stateData ->
                logger.d { "[channelsStateData] state=$stateData" }
                reduce {
                    copy(
                        isLoading = stateData is ChannelsStateData.Loading,
                    )
                }
            }
        }
        viewModelScope.launch {
            chatClient.clientState.user.collectLatest { user ->
                reduce { copy(currentUserId = user?.id) }
                if (user != null && !initialRefreshDone && queryState.currentRequest.value == null) {
                    initialRefreshDone = true
                    refresh()
                }
            }
        }
        viewModelScope.launch {
            chatClient.clientState.connectionState.collectLatest { state ->
                reduce { copy(connectionState = state) }
            }
        }
    }

    fun onAction(action: ChatsUiAction) {
        when (action) {
            ChatsUiAction.Refresh -> refresh()
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            logger.d { "[refresh] query channels" }
            runCatching {
                chatClient.queryChannels(
                    QueryChannelsRequest(limit = 30)
                        .withMessages(limit = 20)
                        .withMembers(limit = 30),
                )
                reduce { copy(errorMessage = null) }
            }.onFailure { throwable ->
                logger.e(throwable) { "[refresh] query channels failed" }
                reduce { copy(errorMessage = throwable.message) }
            }
        }
    }

    private inline fun reduce(block: ChatsUiState.() -> ChatsUiState) {
        _uiState.update { it.block() }
    }

    private companion object {
        const val TAG = "Chat:V2ChatsViewModel"
    }
}

data class ChatsUiState(
    val isLoading: Boolean = false,
    val items: List<Channel> = emptyList(),
    val currentUserId: String? = null,
    val connectionState: org.example.project.v2.core.models.ConnectionState =
        org.example.project.v2.core.models.ConnectionState.OFFLINE,
    val errorMessage: String? = null,
)

sealed interface ChatsUiAction {
    data object Refresh : ChatsUiAction
}
