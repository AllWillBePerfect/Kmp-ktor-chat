package org.example.project.ui.screens.login

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.example.project.ui.utils.PreviewWrapper
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginRoute(
    viewModel: LoginViewModel = koinViewModel(),
    onLoginSuccess: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                LoginUiEvent.NavigateToMain -> onLoginSuccess()
            }
        }
    }

    LoginScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onSettingsClick = onSettingsClick
    )
}

@Composable
private fun LoginScreen(
    uiState: LoginUiState,
    onAction: (LoginUiAction) -> Unit,
    onSettingsClick: () -> Unit
) {
    LoginContentWrapper(
        onSettingsClick = onSettingsClick,
        content = { innerPadding ->
            LoginContent(
                uiState = uiState,
                onAction = onAction,
                innerPadding = innerPadding,
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginContentWrapper(
    onSettingsClick: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = "Login")
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        content(innerPadding)
    }
}

@Composable
private fun LoginContent(
    uiState: LoginUiState,
    onAction: (LoginUiAction) -> Unit,
    innerPadding: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to KmpChat",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        TextField(
            value = uiState.login,
            onValueChange = { onAction(LoginUiAction.LoginTyped(it)) },
            label = { Text("Login") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = uiState.password,
            onValueChange = { onAction(LoginUiAction.PasswordTyped(it)) },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (uiState.error != null) {
            Text(
                text = uiState.error.asText(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onAction(LoginUiAction.LoginClicked) },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.isButtonEnabled
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Login")
            }
        }
    }
}

@Composable
private fun DefaultPreviewValue() = LoginScreen(
    uiState = LoginUiState(),
    onAction = {},
    onSettingsClick = {}
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
