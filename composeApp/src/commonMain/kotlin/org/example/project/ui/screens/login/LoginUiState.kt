package org.example.project.ui.screens.login

data class LoginUiState(
    val login: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: LoginUiError? = null
) {
    val isButtonEnabled: Boolean =
        login.isNotBlank() && password.isNotBlank() && error == null && !isLoading
}

sealed interface LoginUiError {
    data object EmptyFields : LoginUiError
    data class AuthFailed(val message: String) : LoginUiError
}
