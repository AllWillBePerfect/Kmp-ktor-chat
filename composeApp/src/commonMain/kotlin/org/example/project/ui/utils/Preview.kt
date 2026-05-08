package org.example.project.ui.utils

import androidx.compose.runtime.Composable
import org.example.project.ui.AppTheme

@Composable
fun PreviewWrapper(
    isDarkTheme: Boolean = true,
    content: @Composable () -> Unit
) = AppTheme(
    isDarkTheme = isDarkTheme
) { content() }