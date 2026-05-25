package org.example.project.ui.screens.conversation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.example.project.ui.components.appbar.ConversationAppBarComponent
import org.example.project.ui.components.message.MessagesComponent
import org.example.project.ui.components.message.defaultMessageList
import org.example.project.ui.components.userinput.ConversationUserInputComponent
import org.example.project.ui.screens.chat.ChatUiAction
import org.example.project.ui.screens.chat.ChatUiState
import org.example.project.ui.screens.chat.ChatViewModel
import org.example.project.ui.utils.PreviewWrapper
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ConversationRoute(
    viewModel: ConversationViewModel = koinViewModel(),
    chatViewModel: ChatViewModel = koinViewModel(),
    onBackPressed: () -> Unit
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val chatUiState by chatViewModel.uiState.collectAsStateWithLifecycle()

    // Добавленная логика управления соединением
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        chatViewModel.onAction(ChatUiAction.Connect)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        chatViewModel.onAction(ChatUiAction.Disconnect)
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                ConversationUiEvent.OnBackPressed -> onBackPressed()
            }
        }
    }

    ConversationScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        chatUiState = chatUiState,
        chatOnAction = chatViewModel::onAction
    )
}

@Composable
private fun ConversationScreen(
    uiState: ConversationUiState,
    onAction: (ConversationUiAction) -> Unit,
    chatUiState: ChatUiState,
    chatOnAction: (ChatUiAction) -> Unit,
) {
    ConversationContentWrapper(
        uiState = uiState,
        onAction = onAction,
        chatUiState = chatUiState, // Прокидываем стейт для шапки
        content = { innerPadding ->
            ConversationContent(
                uiState = uiState,
                onAction = onAction,
                chatUiState = chatUiState,
                chatOnAction = chatOnAction,
                innerPadding = innerPadding,
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationContentWrapper(
    uiState: ConversationUiState,
    onAction: (ConversationUiAction) -> Unit,
    chatUiState: ChatUiState, // Добавлено для динамической шапки
    content: @Composable (PaddingValues) -> Unit
) {

    val topBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topBarState)

    Scaffold(
        topBar = {
            ConversationAppBarComponent(
                headlineText = "General chat",
                supportingText = chatUiState.onlineCountText, // Используем реальный онлайн
                scrollBehavior = scrollBehavior,
                onNavIconPressed = { onAction(ConversationUiAction.OnBackPressed) }
            )
        },
        contentWindowInsets = ScaffoldDefaults
            .contentWindowInsets
            .exclude(WindowInsets.navigationBars)
            .exclude(WindowInsets.ime),
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        content(innerPadding)
    }
}

@Composable
private fun ConversationContent(
    uiState: ConversationUiState,
    onAction: (ConversationUiAction) -> Unit,
    chatUiState: ChatUiState,
    chatOnAction: (ChatUiAction) -> Unit,
    innerPadding: PaddingValues
) {

    val scope = rememberCoroutineScope()

    val scrollState = rememberLazyListState()

    val messages = remember {
        defaultMessageList.toMutableStateList()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        MessagesComponent(
            modifier = Modifier.weight(1f),
            messages = chatUiState.getReversedMessages,
            scrollState = scrollState
        )

        ConversationUserInputComponent(
            onMessageSent = { content ->
                chatOnAction(ChatUiAction.MessageSentWithText(content))
                /*messages.addFirst(
                    Message(
                        author = "me",
                        content = content,
                        timestamp = "",
                    )
                )*/
            },
            resetScroll = {
                scope.launch {
                    scrollState.scrollToItem(0)
                }
            },
            // let this element handle the padding so that the elevation is shown behind the
            // navigation bar
            modifier = Modifier
                .navigationBarsPadding().imePadding(),
        )

    }
}

@Composable
@Preview
private fun PreviewNight() = PreviewWrapper {
    ConversationScreen(
        uiState = ConversationUiState(),
        onAction = {},
        chatUiState = ChatUiState(),
        chatOnAction = {}
    )
}

@Composable
@Preview
private fun PreviewLight() = PreviewWrapper(
    isDarkTheme = false
) {
    ConversationScreen(
        uiState = ConversationUiState(),
        onAction = {},
        chatUiState = ChatUiState(),
        chatOnAction = {}
    )
}
