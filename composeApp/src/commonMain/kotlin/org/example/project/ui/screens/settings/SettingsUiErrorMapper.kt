package org.example.project.ui.screens.settings

fun SettingsUiError.asText(): String {
    return when (this) {
        SettingsUiError.EmptyHost -> "Host is required"
        SettingsUiError.EmptyPort -> "Port is required"
        SettingsUiError.InvalidPort -> "Port must be a number between 1 and 65535"
        is SettingsUiError.SaveFailed -> message
    }
}
