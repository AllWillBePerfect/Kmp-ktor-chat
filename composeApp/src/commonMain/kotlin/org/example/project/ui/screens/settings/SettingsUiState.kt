package org.example.project.ui.screens.settings

import org.example.project.data.models.AppSettings
import org.example.project.data.models.ThemeMode

data class SettingsUiState(
    val isLoaded: Boolean = false,
    val isSaving: Boolean = false,
    val savedSettings: AppSettings? = null,
    val hostText: String = "",
    val portText: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val error: SettingsUiError? = null
) {
    val hasUnsavedChanges: Boolean
        get() = savedSettings?.let {
            hostText != it.host ||
                portText != it.port.toString() ||
                themeMode != it.themeMode
        } ?: false

    val isSaveEnabled: Boolean
        get() = isLoaded && hasUnsavedChanges && error == null && !isSaving
}

sealed interface SettingsUiError {
    data object EmptyHost : SettingsUiError
    data object EmptyPort : SettingsUiError
    data object InvalidPort : SettingsUiError
    data class SaveFailed(val message: String) : SettingsUiError
}
