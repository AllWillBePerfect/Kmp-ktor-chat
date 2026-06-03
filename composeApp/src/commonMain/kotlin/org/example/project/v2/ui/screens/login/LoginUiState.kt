package org.example.project.v2.ui.screens.login

data class LoginUiState(
    val login: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val isLoginEnabled: Boolean =
        login.isNotBlank() && password.isNotBlank() && !isLoading
}
