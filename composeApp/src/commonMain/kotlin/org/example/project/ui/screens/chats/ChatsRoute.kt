package org.example.project.ui.screens.chats

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.example.project.ui.screens.chats.components.ChatListItemComponent
import org.example.project.ui.utils.PreviewWrapper
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChatsRoute(
    viewModel: ChatsViewModel = koinViewModel(),
    navigateToChatScreen: () -> Unit
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                ChatsUiEvent.OnItemClicked -> navigateToChatScreen()
            }
        }
    }

    ChatsScreen(
        uiState = uiState,
        onAction = viewModel::onAction
    )
}

@Composable
private fun ChatsScreen(
    uiState: ChatsUiState,
    onAction: (ChatsUiAction) -> Unit
) {
    ChatsContentWrapper(
        uiState = uiState,
        onAction = onAction,
        content = { innerPadding ->
            ChatsContent(
                uiState = uiState,
                onAction = onAction,
                innerPadding = innerPadding,
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatsContentWrapper(
    uiState: ChatsUiState,
    onAction: (ChatsUiAction) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = "Chats")
                }
            )
        }
    ) { innerPadding ->
        content(innerPadding)
    }
}

@Composable
private fun ChatsContent(
    uiState: ChatsUiState,
    onAction: (ChatsUiAction) -> Unit,
    innerPadding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier.padding(innerPadding)
    ) {
        items((1..10).toList()) {
            ChatListItemComponent(
                onAction = onAction
            )
        }
    }
}

@Composable
private fun DefaultPreviewValue() = ChatsScreen(
    uiState = ChatsUiState(),
    onAction = {}
)

@Composable
@Preview
private fun PreviewNight() = PreviewWrapper {
    DefaultPreviewValue()
}

@Composable
@Preview
private fun PreviewLight() = PreviewWrapper(
    isDarkTheme = false
) {
    DefaultPreviewValue()
}