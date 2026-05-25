package org.example.project.ui.components.appbar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import org.example.project.ui.utils.PreviewWrapper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CenterAppBarComponent(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onNavIconPressed: () -> Unit = { }
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        actions = actions,
        title = title,
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            IconButton(
                onClick = onNavIconPressed,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = null
                )
            }
        }
    )
}

@Composable
fun AppBarTextLayout(
    headlineText: String,
    supportingText: String
) {
    Column {
        Text(
            text = headlineText,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = supportingText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
private fun PreviewNight() = PreviewWrapper {
    CenterAppBarComponent(
        title = {
            AppBarTextLayout(
                headlineText = "Super Chat",
                supportingText = "1 user"
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
private fun PreviewLight() = PreviewWrapper(
    isDarkTheme = false
) {
    CenterAppBarComponent(
        title = {
            AppBarTextLayout(
                headlineText = "Super Chat",
                supportingText = "1 user"
            )
        }
    )
}