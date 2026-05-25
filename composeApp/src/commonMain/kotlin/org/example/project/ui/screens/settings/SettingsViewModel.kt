package org.example.project.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.data.PreferencesDataSource
import org.example.project.data.models.AppSettings
import org.example.project.data.models.ThemeMode
import org.example.project.data.repository.AuthRepository

class SettingsViewModel(
    private val pref: PreferencesDataSource,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<SettingsUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            pref.appSettingsFlow.collect { settings ->
                _uiState.update { current ->
                    val shouldSyncForm = !current.isLoaded || !current.hasUnsavedChanges || current.isSaving

                    current.copy(
                        isLoaded = true,
                        isSaving = false,
                        savedSettings = settings,
                        hostText = if (shouldSyncForm) settings.host else current.hostText,
                        portText = if (shouldSyncForm) settings.port.toString() else current.portText,
                        themeMode = if (shouldSyncForm) settings.themeMode else current.themeMode,
                        error = if (shouldSyncForm) null else current.error
                    )
                }
            }
        }
    }

    fun onAction(action: SettingsUiAction) {
        when (action) {
            is SettingsUiAction.HostTextTyped -> {
                reduce {
                    copy(
                        hostText = action.host,
                        error = null
                    )
                }
            }

            is SettingsUiAction.PortTextTyped -> {
                reduce {
                    copy(
                        portText = action.port,
                        error = null
                    )
                }
            }

            is SettingsUiAction.ThemeModeSelected -> {
                reduce {
                    copy(
                        themeMode = action.themeMode,
                        error = null
                    )
                }
            }

            SettingsUiAction.SaveClicked -> saveSettings()
            SettingsUiAction.OnBackPressed -> sendEvent(SettingsUiEvent.OnBackPressed)
            SettingsUiAction.OnLogoutClicked -> {
                viewModelScope.launch {
                    authRepository.logout()
                    sendEvent(SettingsUiEvent.OnBackPressed)
                }
            }
        }
    }

    private fun saveSettings() {
        val settings = validate() ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSaving = true,
                    error = null
                )
            }

            runCatching {
                pref.saveAppSettings(settings)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        savedSettings = settings,
                        hostText = settings.host,
                        portText = settings.port.toString(),
                        themeMode = settings.themeMode,
                        error = null
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = SettingsUiError.SaveFailed(
                            throwable.message ?: "Failed to save settings"
                        )
                    )
                }
            }
        }
    }

    private fun validate(): AppSettings? {
        val currentState = _uiState.value
        val host = currentState.hostText.trim()
        val portText = currentState.portText.trim()

        val error = when {
            host.isEmpty() -> SettingsUiError.EmptyHost
            portText.isEmpty() -> SettingsUiError.EmptyPort
            else -> {
                val port = portText.toIntOrNull()
                when (port) {
                    null -> SettingsUiError.InvalidPort
                    !in 1..65535 -> SettingsUiError.InvalidPort
                    else -> null
                }
            }
        }

        if (error != null) {
            _uiState.update { it.copy(error = error) }
            return null
        }

        return AppSettings(
            host = host,
            port = portText.toInt(),
            themeMode = currentState.themeMode
        )
    }

    private inline fun reduce(
        crossinline reducer: SettingsUiState.() -> SettingsUiState
    ) {
        _uiState.update { it.reducer() }
    }

    private fun sendEvent(event: SettingsUiEvent) = viewModelScope.launch {
        _uiEvent.emit(event)
    }
}

sealed interface SettingsUiAction {
    data class HostTextTyped(val host: String) : SettingsUiAction
    data class PortTextTyped(val port: String) : SettingsUiAction
    data class ThemeModeSelected(val themeMode: ThemeMode) : SettingsUiAction
    data object SaveClicked : SettingsUiAction
    data object OnBackPressed : SettingsUiAction
    data object OnLogoutClicked : SettingsUiAction
}

sealed interface SettingsUiEvent {
    data object OnBackPressed : SettingsUiEvent
}
