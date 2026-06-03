package org.example.project.v2.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.v2.data.repository.V2AuthRepository
import org.example.project.v2.platform.taggedLogger

class LoginViewModel(
    private val authRepository: V2AuthRepository,
) : ViewModel() {
    private val logger by taggedLogger(TAG)

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    fun onAction(action: LoginUiAction) {
        when (action) {
            is LoginUiAction.LoginTyped -> reduce { copy(login = action.value, errorMessage = null) }
            is LoginUiAction.PasswordTyped -> reduce { copy(password = action.value, errorMessage = null) }
            LoginUiAction.Submit -> login()
        }
    }

    private fun login() {
        val current = uiState.value
        if (current.login.isBlank() || current.password.isBlank()) {
            reduce { copy(errorMessage = "Please fill all fields") }
            return
        }

        viewModelScope.launch {
            reduce { copy(isLoading = true, errorMessage = null) }
            authRepository.login(current.login, current.password)
                .onSuccess {
                    logger.i { "[login] success" }
                    reduce { copy(isLoading = false, errorMessage = null) }
                }
                .onFailure { throwable ->
                    logger.e(throwable) { "[login] failed" }
                    reduce {
                        copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Unknown error",
                        )
                    }
                }
        }
    }

    private inline fun reduce(block: LoginUiState.() -> LoginUiState) {
        _uiState.update { it.block() }
    }

    private companion object {
        const val TAG = "Chat:V2LoginViewModel"
    }
}

sealed interface LoginUiAction {
    data class LoginTyped(val value: String) : LoginUiAction
    data class PasswordTyped(val value: String) : LoginUiAction
    data object Submit : LoginUiAction
}
