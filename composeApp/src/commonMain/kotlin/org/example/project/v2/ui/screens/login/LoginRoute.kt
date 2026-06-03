package org.example.project.v2.ui.screens.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginRoute(
    viewModel: LoginViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LoginScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
    )
}

@Composable
private fun LoginScreen(
    uiState: LoginUiState,
    onAction: (LoginUiAction) -> Unit,
) {
    LoginScaffold(
        content = { innerPadding ->
            LoginContent(
                uiState = uiState,
                onAction = onAction,
                innerPadding = innerPadding,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginScaffold(
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("V2 Login") },
            )
        },
    ) { innerPadding ->
        content(innerPadding)
    }
}

@Composable
private fun LoginContent(
    uiState: LoginUiState,
    onAction: (LoginUiAction) -> Unit,
    innerPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Sign in",
            style = MaterialTheme.typography.headlineMedium,
        )

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            value = uiState.login,
            onValueChange = { onAction(LoginUiAction.LoginTyped(it)) },
            label = { Text("Login") },
            singleLine = true,
        )

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            value = uiState.password,
            onValueChange = { onAction(LoginUiAction.PasswordTyped(it)) },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
        )

        uiState.errorMessage?.let { errorMessage ->
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            enabled = uiState.isLoginEnabled,
            onClick = { onAction(LoginUiAction.Submit) },
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Login")
            }
        }
    }
}
