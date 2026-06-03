package org.example.project.v2.ui.screens.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.example.project.v2.core.models.Message
import org.example.project.v2.ui.common.state.messages.composer.MessageComposerState
import org.example.project.v2.ui.common.state.messages.list.MessageListState
import org.example.project.v2.ui.components.channels.MessageReadStatusIcon
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChatRoute(
    onBackPressed: () -> Unit,
    viewModel: ChatViewModel = koinViewModel(),
    messageListViewModel: MessageListViewModel = koinViewModel(),
    messageComposerViewModel: MessageComposerViewModel = koinViewModel(),
) {
    val chatState by viewModel.uiState.collectAsStateWithLifecycle()
    val composerState by messageComposerViewModel.state.collectAsStateWithLifecycle()
    val messageListState by messageListViewModel.state.collectAsStateWithLifecycle()

    ChatScreen(
        state = ChatScreenState(
            chat = chatState,
            messageList = messageListState,
            composer = composerState,
        ),
        actions = ChatScreenActions(
            onBackPressed = onBackPressed,
            onMessageInputChanged = messageComposerViewModel::setMessageInput,
            onSendMessage = messageComposerViewModel::sendMessage,
        ),
    )
}

@Composable
private fun ChatScreen(
    state: ChatScreenState,
    actions: ChatScreenActions,
) {
    ChatScaffold(
        state = state,
        actions = actions,
        content = { innerPadding ->
            ChatContent(
                state = state,
                actions = actions,
                innerPadding = innerPadding,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScaffold(
    state: ChatScreenState,
    actions: ChatScreenActions,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = actions.onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                title = {
                    Column {
                        Text(state.title)
                        Text(
                            text = state.subtitle,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        content(innerPadding)
    }
}

@Composable
private fun ChatContent(
    state: ChatScreenState,
    actions: ChatScreenActions,
    innerPadding: PaddingValues,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.messageList.messages.size) {
        if (state.messageList.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messageList.messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
        ) {
            items(state.messageList.messages, key = { it.id.ifBlank { it.clientMessageId.orEmpty() } }) { message ->
                MessageItem(
                    message = message,
                    currentUserId = state.messageList.currentUserId,
                    readCount = state.messageList.readCountForMessage(message),
                )
            }
        }

        state.messageList.typingLabel?.let { typingLabel ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = typingLabel,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        listOfNotNull(state.chat.errorMessage, state.composer.errorMessage).lastOrNull()?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = state.composer.inputValue,
                onValueChange = actions.onMessageInputChanged,
                placeholder = { Text("Message") },
            )
            IconButton(
                enabled = state.composer.canSendMessage,
                onClick = actions.onSendMessage,
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
            }
        }
    }
}

@Composable
private fun MessageItem(
    message: Message,
    currentUserId: String?,
    readCount: Int,
) {
    val author = if (message.user.id == currentUserId) "You" else message.user.name.ifBlank { message.user.id }
    ListItem(
        headlineContent = { Text(author) },
        supportingContent = { Text(message.text) },
        trailingContent = {
            Column {
                if (message.user.id == currentUserId && readCount > 0) {
                    Row {
                        MessageReadStatusIcon(
                            message = message,
                            isMessageRead = true,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (readCount == 1) "Read" else "$readCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                } else if (message.user.id == currentUserId) {
                    Row {
                        MessageReadStatusIcon(
                            message = message,
                            isMessageRead = false,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Sent",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                message.createdAt
                    ?.toChatTimeLabel()
                    ?.let { Text(it) }
            }
        },
    )
}

private fun String.toChatTimeLabel(): String? {
    val timePart = substringAfter('T', missingDelimiterValue = "")
    if (timePart.length < 5) return null
    val hours = timePart.substring(0, 2)
    val minutes = timePart.substring(3, 5)
    return if (hours.all(Char::isDigit) && minutes.all(Char::isDigit)) {
        "$hours:$minutes"
    } else {
        null
    }
}

private data class ChatScreenState(
    val chat: ChatUiState,
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

private data class ChatScreenActions(
    val onBackPressed: () -> Unit,
    val onMessageInputChanged: (String) -> Unit,
    val onSendMessage: () -> Unit,
)
