package org.example.project.ui.screens.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
            listState.animateScrollToItem(uiState.messages.size)
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
            items(uiState.messages) {
                ListItem(
                    modifier = Modifier.clickable {},
                    headlineContent = {
                        Text("User")
                    },
                    supportingContent = {
                        Text(it)
                    }

                )
            }
        }
        Row(
            modifier = Modifier
                    .padding(horizontal = 16.dp)

        ) {
            OutlinedTextField(
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier.weight(1f),
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






