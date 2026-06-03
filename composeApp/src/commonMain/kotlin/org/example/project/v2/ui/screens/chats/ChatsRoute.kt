package org.example.project.v2.ui.screens.chats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.example.project.v2.core.models.Channel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChatsRoute(
    navigateToChat: (String) -> Unit,
    navigateToSettings: () -> Unit,
    viewModel: ChatsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ChatsScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onChannelClick = { navigateToChat(it.cid) },
        navigateToSettings = navigateToSettings
    )
}

@Composable
private fun ChatsScreen(
    uiState: ChatsUiState,
    onAction: (ChatsUiAction) -> Unit,
    onChannelClick: (Channel) -> Unit,
    navigateToSettings: () -> Unit
) {
    ChatsScaffold(
        uiState = uiState,
        navigateToSettings = navigateToSettings,
        content = { innerPadding ->
            when {
                uiState.isLoading && uiState.items.isEmpty() -> LoadingState(innerPadding)
                uiState.items.isEmpty() -> EmptyState(innerPadding, uiState.errorMessage)
                else -> ChatsList(
                    innerPadding = innerPadding,
                    items = uiState.items,
                    currentUserId = uiState.currentUserId,
                    onChannelClick = onChannelClick,
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatsScaffold(
    uiState: ChatsUiState,
    navigateToSettings: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("V2 Chats")
                        Text(
                            text = uiState.connectionState.name,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {navigateToSettings.invoke()}){
                        Icon(
                            Icons.Default.Settings,
                            null
                        )
                    }
                }
            )
        },
    ) { innerPadding ->
        content(innerPadding)
    }
}

@Composable
private fun ChatsList(
    innerPadding: PaddingValues,
    items: List<Channel>,
    currentUserId: String?,
    onChannelClick: (Channel) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
    ) {
        items(items, key = { it.cid }) { channel ->
            ListItem(
                modifier = Modifier.clickable { onChannelClick(channel) },
                headlineContent = {
                    Text(channel.name.ifBlank { channel.id })
                },
                supportingContent = {
                    val memberCount = channel.members.size.takeIf { it > 0 } ?: channel.memberCount
                    val lastAuthor = channel.messages.lastOrNull()?.user?.let { user ->
                        if (user.id == currentUserId) "You" else user.name
                    }.orEmpty()
                    val lastText = channel.messages.lastOrNull()?.text.orEmpty()
                    val line = buildString {
                        if (memberCount > 0) append("$memberCount members")
                        if (lastText.isNotBlank()) {
                            if (isNotEmpty()) append(" • ")
                            if (lastAuthor.isNotBlank()) append("$lastAuthor: ")
                            append(lastText)
                        }
                    }
                    Text(line.ifBlank { "No messages yet" })
                },
                trailingContent = {
                    Column(horizontalAlignment = Alignment.End) {
                        channel.lastMessageAt
                            ?.toChatTimeLabel()
                            ?.let { Text(it) }
                        if (channel.unreadCount > 0) {
                            Badge(
                                modifier = Modifier.padding(top = 6.dp),
                            ) {
                                Text(channel.unreadCount.toString())
                            }
                        }
                    }
                },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun LoadingState(innerPadding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState(innerPadding: PaddingValues, errorMessage: String?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(errorMessage ?: "No channels")
    }
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
