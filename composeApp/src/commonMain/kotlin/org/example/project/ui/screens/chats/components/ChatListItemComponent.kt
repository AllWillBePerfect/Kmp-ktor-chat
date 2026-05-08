package org.example.project.ui.screens.chats.components

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.example.project.ui.screens.chats.ChatsUiAction
import org.example.project.ui.utils.PreviewWrapper

@Composable
fun ChatListItemComponent(
    onAction: (ChatsUiAction) -> Unit
) {
    ListItem(
        modifier = Modifier
            .clickable { onAction(ChatsUiAction.OnItemClicked) },
        leadingContent = {
            Icon(
                Icons.Default.VerifiedUser,
                null
            )
        },
        headlineContent = {
            Text("Chat name")
        },
        supportingContent = {
            Text("last message")
        },
        trailingContent = {
            RoundBadgeComponent(
                count = 50000
            )
        }
    )
}

@Composable
@Preview
private fun PreviewNight() = PreviewWrapper {
    ChatListItemComponent(
        onAction = {}
    )
}


@Composable
@Preview
private fun PreviewLight() = PreviewWrapper(
    isDarkTheme = false
) {
    ChatListItemComponent(
        onAction = {}
    )
}

