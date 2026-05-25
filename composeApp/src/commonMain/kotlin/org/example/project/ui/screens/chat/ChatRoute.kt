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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
                ListItem(
                    modifier = Modifier.clickable {},
                    headlineContent = {
                        Text(message.author)
                    },
                    supportingContent = {
                        Text(message.content)
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
