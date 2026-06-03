package org.example.project.ui.screens.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.example.project.domain.model.SyncStatus
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun ChatRoute(

    viewModel: ChatViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ChatScreen(
        uiState = uiState,
        onAction = viewModel::onAction
    )
}

@Composable
private fun ChatScreen(
    uiState: ChatUiState,
    onAction: (ChatUiAction) -> Unit
) {
    ChatContentWrapper(
        uiState = uiState,
        onAction = onAction,
        content = { innerPadding ->
            ChatContent(
                uiState = uiState,
                onAction = onAction,
                innerPadding = innerPadding,
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatContentWrapper(
    uiState: ChatUiState,
    onAction: (ChatUiAction) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = "Chat")
                }
            )
        }
    ) { innerPadding ->
        content(innerPadding)
    }
}

@Composable
private fun ChatContent(
    uiState: ChatUiState,
    onAction: (ChatUiAction) -> Unit,
    innerPadding: PaddingValues
) {

    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxWidth()
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,

        ) {
            items(uiState.messages) { message ->
                val retryable = message.author == "me" && message.syncStatus != SyncStatus.COMPLETED && message.syncStatus != SyncStatus.IN_PROGRESS
                ListItem(
                    modifier = Modifier.clickable(enabled = retryable) {
                        onAction(ChatUiAction.RetryMessage(message.localId))
                    },
                    headlineContent = {
                        Text(message.author)
                    },
                    supportingContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(message.content)
                            message.syncStatus.statusLabel(message.lastSyncError)?.let { statusText ->
                                Text(
                                    text = statusText,
                                    color = if (message.syncStatus == SyncStatus.SYNC_NEEDED) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        Color.Unspecified
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    },
                    trailingContent = {
                        Text(message.timestamp)
                    }
                )
            }
        }
        Row(
            modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)

        ) {
            OutlinedTextField(
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier.fillMaxWidth(),
                value = uiState.userText,
                onValueChange = { onAction(ChatUiAction.TextTyped(it)) },
                trailingIcon = {
                    IconButton(
                        modifier = Modifier.padding(end = 8.dp),
                        colors = IconButtonDefaults.iconButtonColors()
                            .copy(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        enabled = uiState.isButtonEnabled,
                        onClick = {
                            onAction(ChatUiAction.MessageSent)
                        }) {
                        Icon(
                            Icons.Default.Upload,
                            null
                        )
                    }
                }
            )
        }
    }
}

private fun SyncStatus.statusLabel(lastSyncError: String?): String? {
    return when (this) {
        SyncStatus.COMPLETED -> null
        SyncStatus.IN_PROGRESS -> "Sending..."
        SyncStatus.SYNC_NEEDED -> lastSyncError?.let { "Failed: $it. Tap to retry." } ?: "Pending. Tap to retry."
        SyncStatus.FAILED_PERMANENTLY -> lastSyncError?.let { "Failed permanently: $it" } ?: "Failed permanently"
    }
}
