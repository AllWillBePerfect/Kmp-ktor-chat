package org.example.project.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.example.project.data.models.AppSettings
import org.example.project.data.models.ConnectionPrefModel
import org.example.project.data.models.ThemeMode
import org.example.project.data.models.UserPrefModel
import org.example.project.utils.AppDispatchers

class PreferencesDataSource(
    private val dataStore: DataStore<Preferences>,
    private val dispatchers: AppDispatchers
) {

    val userDataFlow: Flow<UserPrefModel> = dataStore.data
        .map(::preferencesToUserData)
        .distinctUntilChanged()

    suspend fun saveUserData(userData: UserPrefModel) {
        edit { preferences ->
            userData.token?.let { preferences[TOKEN_KEY] = it }
            userData.userId?.let { preferences[USER_ID_KEY] = it }
            userData.userName?.let { preferences[USER_NAME_KEY] = it }
        }
    }

    suspend fun clearAuthData() {
        edit { preferences ->
            preferences.remove(TOKEN_KEY)
            preferences.remove(USER_ID_KEY)
            preferences.remove(USER_NAME_KEY)
        }
    }

    val connectionSettingsFlow: Flow<ConnectionPrefModel> = dataStore.data
        .map(::preferencesToConnectionSettings)
        .distinctUntilChanged()

    val appSettingsFlow: Flow<AppSettings> = dataStore.data
        .map(::preferencesToAppSettings)
        .distinctUntilChanged()

    suspend fun saveAppSettings(settings: AppSettings) {
        edit { preferences ->
            preferences[CONNECTION_PREF_MODEL_HOST_KEY] = settings.host
            preferences[CONNECTION_PREF_MODEL_PORT_KEY] = settings.port
            preferences[THEME_MODE_KEY] = settings.themeMode.name
        }
    }

    @Deprecated("Use appSettingsFlow instead")
    val settingsFlow: Flow<AppSettings> = appSettingsFlow

    @Deprecated("Use connectionSettingsFlow instead")
    val connectionDModelFlow: Flow<ConnectionPrefModel> = connectionSettingsFlow

    @Deprecated("Use saveUserData instead")
    suspend fun updateUserData(userData: UserPrefModel) = saveUserData(userData)

    @Deprecated("Use clearAuthData instead")
    suspend fun clearAuth() = clearAuthData()

    @Deprecated("Use saveAppSettings instead")
    suspend fun saveSettings(settings: AppSettings) = saveAppSettings(settings)

    private fun preferencesToUserData(preferences: Preferences): UserPrefModel {
        return UserPrefModel(
            token = preferences[TOKEN_KEY],
            userId = preferences[USER_ID_KEY],
            userName = preferences[USER_NAME_KEY]
        )
    }

    private fun preferencesToConnectionSettings(preferences: Preferences): ConnectionPrefModel {
        val default = ConnectionPrefModel.default()

        return ConnectionPrefModel(
            host = preferences[CONNECTION_PREF_MODEL_HOST_KEY] ?: default.host,
            port = preferences[CONNECTION_PREF_MODEL_PORT_KEY] ?: default.port
        )
    }

    private fun preferencesToAppSettings(preferences: Preferences): AppSettings {
        val default = AppSettings.default()

        return AppSettings(
            host = preferences[CONNECTION_PREF_MODEL_HOST_KEY] ?: default.host,
            port = preferences[CONNECTION_PREF_MODEL_PORT_KEY] ?: default.port,
            themeMode = preferences[THEME_MODE_KEY]?.toThemeModeOrNull() ?: default.themeMode
        )
    }

    private suspend inline fun edit(noinline block: (MutablePreferences) -> Unit) {
        withContext(dispatchers.io) {
            dataStore.edit(block)
        }
    }

    companion object {
        private val CONNECTION_PREF_MODEL_HOST_KEY = stringPreferencesKey("connection.host")
        private val CONNECTION_PREF_MODEL_PORT_KEY = intPreferencesKey("connection.port")
        private val THEME_MODE_KEY = stringPreferencesKey("theme.mode")
        private val TOKEN_KEY = stringPreferencesKey("auth.token")
        private val USER_ID_KEY = stringPreferencesKey("auth.user_id")
        private val USER_NAME_KEY = stringPreferencesKey("auth.user_name")
    }
}

private fun String.toThemeModeOrNull(): ThemeMode? {
    return runCatching { ThemeMode.valueOf(this) }.getOrNull()
}
