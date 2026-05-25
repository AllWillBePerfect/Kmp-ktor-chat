package org.example.project.ui.components.appbar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.More
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.example.project.ui.utils.PreviewWrapper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationAppBarComponent(
    modifier: Modifier = Modifier,
    headlineText: String,
    supportingText: String,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onNavIconPressed: () -> Unit = { }
) {
    CenterAppBarComponent(
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        onNavIconPressed = onNavIconPressed,
        title = {
            AppBarTextLayout(
                headlineText = headlineText,
                supportingText = supportingText
            )
        },
        actions = {
            Icon(
                imageVector = Icons.Default.MoreVert,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable {}
                    .padding(horizontal = 12.dp, vertical = 16.dp)
                    .height(24.dp),
                contentDescription = null,
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
private fun PreviewNight() = PreviewWrapper {
    ConversationAppBarComponent(
        headlineText = "Super chat",
        supportingText = "1 user"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
private fun PreviewLight() = PreviewWrapper(
    isDarkTheme = false
) {
    ConversationAppBarComponent(
        headlineText = "Super chat",
        supportingText = "1 user"
    )
}