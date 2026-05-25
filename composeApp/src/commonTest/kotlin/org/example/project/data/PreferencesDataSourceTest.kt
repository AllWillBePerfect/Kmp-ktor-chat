package org.example.project.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.example.project.data.models.AppSettings
import org.example.project.data.models.ConnectionPrefModel
import org.example.project.data.models.ThemeMode
import org.example.project.data.models.UserPrefModel
import org.example.project.testutils.InMemoryPreferencesDataStore
import org.example.project.testutils.TestAppDispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesDataSourceTest {

    private val dispatcher = StandardTestDispatcher()

    @Test
    fun connectionSettingsFlow_returnsDefaultsForEmptyStore() = runTest(dispatcher) {
        val dataSource = createDataSource()

        assertEquals(ConnectionPrefModel.default(), dataSource.connectionSettingsFlow.first())
    }

    @Test
    fun appSettingsFlow_returnsDefaultsForEmptyStore() = runTest(dispatcher) {
        val dataSource = createDataSource()

        assertEquals(AppSettings.default(), dataSource.appSettingsFlow.first())
    }

    @Test
    fun saveAppSettings_updatesAppAndConnectionFlows() = runTest(dispatcher) {
        val dataSource = createDataSource()
        val settings = AppSettings(
            host = "chat.local",
            port = 8181,
            themeMode = ThemeMode.DARK
        )

        dataSource.saveAppSettings(settings)
        advanceUntilIdle()

        assertEquals(settings, dataSource.appSettingsFlow.first())
        assertEquals(
            ConnectionPrefModel(
                host = "chat.local",
                port = 8181
            ),
            dataSource.connectionSettingsFlow.first()
        )
    }

    @Test
    fun saveUserData_persistsUserFields() = runTest(dispatcher) {
        val dataSource = createDataSource()
        val user = UserPrefModel(
            token = "token-123",
            userId = "42",
            userName = "alice"
        )

        dataSource.saveUserData(user)
        advanceUntilIdle()

        assertEquals(user, dataSource.userDataFlow.first())
        assertTrue(dataSource.userDataFlow.first().isAuthenticated)
    }

    @Test
    fun clearAuthData_removesStoredUserFields() = runTest(dispatcher) {
        val dataSource = createDataSource()
        dataSource.saveUserData(
            UserPrefModel(
                token = "token-123",
                userId = "42",
                userName = "alice"
            )
        )
        advanceUntilIdle()

        dataSource.clearAuthData()
        advanceUntilIdle()

        val user = dataSource.userDataFlow.first()
        assertEquals(UserPrefModel(), user)
        assertFalse(user.isAuthenticated)
    }

    @Test
    fun appSettingsFlow_fallsBackToSystemThemeForInvalidStoredValue() = runTest(dispatcher) {
        val store = InMemoryPreferencesDataStore()
        store.applyEdit { preferences ->
            preferences[stringPreferencesKey("theme.mode")] = "BROKEN_THEME"
        }
        val dataSource = createDataSource(store)

        assertEquals(ThemeMode.SYSTEM, dataSource.appSettingsFlow.first().themeMode)
    }

    private fun createDataSource(
        dataStore: InMemoryPreferencesDataStore = InMemoryPreferencesDataStore()
    ): PreferencesDataSource {
        return PreferencesDataSource(
            dataStore = dataStore,
            dispatchers = TestAppDispatchers(dispatcher)
        )
    }
}
