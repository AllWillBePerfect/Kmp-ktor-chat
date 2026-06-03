package org.example.project.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.example.project.data.models.ThemeMode
import org.example.project.ui.utils.PreviewWrapper
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel = koinViewModel(),
    onBackPressed: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                SettingsUiEvent.OnBackPressed -> onBackPressed()
            }
        }
    }

    SettingsScreen(
        uiState = uiState,
        onAction = viewModel::onAction
    )
}

@Composable
private fun SettingsScreen(
    uiState: SettingsUiState,
    onAction: (SettingsUiAction) -> Unit
) {
    SettingsContentWrapper(
        onAction = onAction,
        content = { innerPadding ->
            SettingsContent(
                uiState = uiState,
                onAction = onAction,
                innerPadding = innerPadding
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContentWrapper(
    onAction: (SettingsUiAction) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = "Settings")
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(SettingsUiAction.OnBackPressed) }) {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        content(innerPadding)
    }
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onAction: (SettingsUiAction) -> Unit,
    innerPadding: PaddingValues
) {
    if (!uiState.isLoaded) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp)
    ) {
        item {
            TextField(
                value = uiState.hostText,
                onValueChange = { onAction(SettingsUiAction.HostTextTyped(it)) },
                label = { Text("Server Host") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = uiState.portText,
                onValueChange = { onAction(SettingsUiAction.PortTextTyped(it)) },
                label = { Text("Server Port") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleMedium
            )
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
            ThemeMode.values().forEach { themeMode ->
                ThemeModeOption(
                    themeMode = themeMode,
                    selected = uiState.themeMode == themeMode,
                    onClick = {
                        onAction(SettingsUiAction.ThemeModeSelected(themeMode))
                    }
                )
            }
        }
        if (uiState.error != null) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.error.asText(),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onAction(SettingsUiAction.SaveClicked) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.isSaveEnabled
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save")
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onAction(SettingsUiAction.OnLogoutClicked) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout")
            }
        }
    }
}

@Composable
private fun ThemeModeOption(
    themeMode: ThemeMode,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(
            text = themeMode.label(),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private fun ThemeMode.label(): String {
    return when (this) {
        ThemeMode.SYSTEM -> "System"
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
    }
}

@Composable
@Preview
private fun PreviewNight() = PreviewWrapper {
    SettingsScreen(
        uiState = SettingsUiState(
            isLoaded = true,
            savedSettings = org.example.project.data.models.AppSettings.default(),
            hostText = "192.168.1.237",
            portText = "8080"
        ),
        onAction = {}
    )
}

@Composable
@Preview
private fun PreviewLight() = PreviewWrapper(
    isDarkTheme = false
) {
    SettingsScreen(
        uiState = SettingsUiState(isLoaded = true),
        onAction = {}
    )
}
