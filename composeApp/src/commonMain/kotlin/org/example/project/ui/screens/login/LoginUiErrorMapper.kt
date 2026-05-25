package org.example.project.ui.screens.login

fun LoginUiError.asText(): String {
    return when (this) {
        LoginUiError.EmptyFields -> "Please fill all fields"
        is LoginUiError.AuthFailed -> message
    }
}
