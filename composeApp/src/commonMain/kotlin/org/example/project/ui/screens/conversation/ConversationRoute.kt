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
import org.example.project.ui.utils.PreviewWrapper
import org.example.project.v2.ui.common.state.messages.composer.MessageComposerState
import org.example.project.v2.ui.common.state.messages.list.MessageListState
import org.example.project.v2.ui.screens.chat.MessageComposerViewModel
import org.example.project.v2.ui.screens.chat.MessageListViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ConversationRoute(
    viewModel: ConversationViewModel = koinViewModel(),
    messageListViewModel: MessageListViewModel = koinViewModel(),
    messageComposerViewModel: MessageComposerViewModel = koinViewModel(),
    onBackPressed: () -> Unit
) {
    val chatState by viewModel.uiState.collectAsStateWithLifecycle()
    val composerState by messageComposerViewModel.state.collectAsStateWithLifecycle()
    val messageListState by messageListViewModel.state.collectAsStateWithLifecycle()


    ConversationScreen(
        conversationState = ConversationRouteState(
            chat = chatState,
            messageList = messageListState,
            composer = composerState,
        ),
        conversationActions = ConversationRouteActions(
            onBackPressed = onBackPressed,
            onMessageInputChanged = messageComposerViewModel::setMessageInput,
            onSendMessage = messageComposerViewModel::sendMessage
        )
    )
}

@Composable
private fun ConversationScreen(
    conversationState: ConversationRouteState,
    conversationActions: ConversationRouteActions,
) {
    ConversationContentWrapper(
        conversationState = conversationState,
        conversationActions = conversationActions,
        content = { innerPadding ->
            ConversationContent(
                conversationState = conversationState,
                conversationActions = conversationActions,
                innerPadding = innerPadding,
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationContentWrapper(
    conversationState: ConversationRouteState,
    conversationActions: ConversationRouteActions,
    content: @Composable (PaddingValues) -> Unit
) {

    val topBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topBarState)

    Scaffold(
        topBar = {
            ConversationAppBarComponent(
                headlineText = conversationState.title,
                supportingText = conversationState.subtitle,
                scrollBehavior = scrollBehavior,
                onNavIconPressed = {
                    conversationActions.onBackPressed()
                }
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
    conversationState: ConversationRouteState,
    conversationActions: ConversationRouteActions,
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
            messages = conversationState.messageList.messages.asReversed(),
            messagesState = conversationState.messageList,
            scrollState = scrollState
        )

        ConversationUserInputComponent(
            onMessageSent = { content ->
                conversationActions.onMessageInputChanged(content)
                conversationActions.onSendMessage()
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

private data class ConversationRouteState(
    val chat: ConversationUiState,
    val messageList: MessageListState,
    val composer: MessageComposerState,
) {
    val title: String
        get() = chat.channel?.name?.ifBlank { chat.channel.id }.orEmpty()

    val subtitle: String
        get() = buildString {
            append(chat.connectionState.name)
            val memberCount = chat.members.size.takeIf { it > 0 } ?: chat.channel?.memberCount
            if ((memberCount ?: 0) > 0) {
                append(" • ")
                append("$memberCount members")
            }
        }
}

private data class ConversationRouteActions(
    val onBackPressed: () -> Unit,
    val onMessageInputChanged: (String) -> Unit,
    val onSendMessage: () -> Unit,
)


@Composable
@Preview
private fun PreviewNight() = PreviewWrapper {
    ConversationScreen(
        conversationState = ConversationRouteState(
            ConversationUiState(),
            messageList = MessageListState(),
            composer = MessageComposerState()
        ),
        conversationActions = ConversationRouteActions(
            onBackPressed = {},
            onMessageInputChanged = {},
            onSendMessage = {},
        )
    )
}

@Composable
@Preview
private fun PreviewLight() = PreviewWrapper(
    isDarkTheme = false
) {
    ConversationScreen(
        conversationState = ConversationRouteState(
            ConversationUiState(),
            messageList = MessageListState(),
            composer = MessageComposerState()
        ),
        conversationActions = ConversationRouteActions(
            onBackPressed = {},
            onMessageInputChanged = {},
            onSendMessage = {},
        )
    )
}
