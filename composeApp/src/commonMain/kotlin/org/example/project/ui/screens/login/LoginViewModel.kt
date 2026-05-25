package org.example.project.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.data.repository.AuthRepository

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<LoginUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun onAction(action: LoginUiAction) {
        when (action) {
            is LoginUiAction.LoginTyped -> {
                _uiState.update { it.copy(login = action.login, error = null) }
            }
            is LoginUiAction.PasswordTyped -> {
                _uiState.update { it.copy(password = action.password, error = null) }
            }
            LoginUiAction.LoginClicked -> {
                login()
            }
        }
    }

    private fun login() {
        val currentState = _uiState.value
        if (currentState.login.isBlank() || currentState.password.isBlank()) {
            _uiState.update { it.copy(error = LoginUiError.EmptyFields) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = authRepository.login(currentState.login, currentState.password)
            
            if (result.isSuccess) {
                _uiEvent.emit(LoginUiEvent.NavigateToMain)
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = LoginUiError.AuthFailed(
                            result.exceptionOrNull()?.message ?: "Unknown error"
                        )
                    )
                }
            }
        }
    }
}

sealed interface LoginUiAction {
    data class LoginTyped(val login: String) : LoginUiAction
    data class PasswordTyped(val password: String) : LoginUiAction
    data object LoginClicked : LoginUiAction
}

sealed interface LoginUiEvent {
    data object NavigateToMain : LoginUiEvent
}
