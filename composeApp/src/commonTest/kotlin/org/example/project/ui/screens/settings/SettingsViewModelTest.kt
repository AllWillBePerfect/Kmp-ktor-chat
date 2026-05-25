package org.example.project.ui.screens.settings

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.example.project.data.PreferencesDataSource
import org.example.project.data.models.AppSettings
import org.example.project.data.models.ThemeMode
import org.example.project.data.repository.AuthRepository
import org.example.project.testutils.InMemoryPreferencesDataStore
import org.example.project.testutils.MainDispatcherTest
import org.example.project.testutils.TestAppDispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest : MainDispatcherTest() {

    @Test
    fun init_loadsSettingsIntoUiState() = runTest(dispatcher) {
        val preferences = createPreferencesDataSource(
            AppSettings(
                host = "10.0.0.5",
                port = 9090,
                themeMode = ThemeMode.DARK
            )
        )

        val viewModel = SettingsViewModel(
            pref = preferences,
            authRepository = FakeAuthRepository()
        )
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.isLoaded)
        assertEquals("10.0.0.5", viewModel.uiState.value.hostText)
        assertEquals("9090", viewModel.uiState.value.portText)
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
        assertFalse(viewModel.uiState.value.hasUnsavedChanges)
        assertFalse(viewModel.uiState.value.isSaveEnabled)
    }

    @Test
    fun hostTyped_updatesStateWithoutSavingImmediately() = runTest(dispatcher) {
        val preferences = createPreferencesDataSource()
        val initialSettings = preferences.appSettingsFlow.first()
        val viewModel = SettingsViewModel(
            pref = preferences,
            authRepository = FakeAuthRepository()
        )
        advanceUntilIdle()

        viewModel.onAction(SettingsUiAction.HostTextTyped("server.local"))

        assertEquals("server.local", viewModel.uiState.value.hostText)
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)
        assertTrue(viewModel.uiState.value.isSaveEnabled)
        assertEquals(initialSettings, preferences.appSettingsFlow.first())
    }

    @Test
    fun saveClicked_withInvalidPort_setsValidationError() = runTest(dispatcher) {
        val viewModel = SettingsViewModel(
            pref = createPreferencesDataSource(),
            authRepository = FakeAuthRepository()
        )
        advanceUntilIdle()

        viewModel.onAction(SettingsUiAction.PortTextTyped("abc"))
        viewModel.onAction(SettingsUiAction.SaveClicked)
        runCurrent()

        assertIs<SettingsUiError.InvalidPort>(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun saveClicked_withValidChanges_persistsSettings() = runTest(dispatcher) {
        val preferences = createPreferencesDataSource()
        val viewModel = SettingsViewModel(
            pref = preferences,
            authRepository = FakeAuthRepository()
        )
        advanceUntilIdle()

        viewModel.onAction(SettingsUiAction.HostTextTyped("chat.local"))
        viewModel.onAction(SettingsUiAction.PortTextTyped("8181"))
        viewModel.onAction(SettingsUiAction.ThemeModeSelected(ThemeMode.LIGHT))
        viewModel.onAction(SettingsUiAction.SaveClicked)
        advanceUntilIdle()

        val saved = preferences.appSettingsFlow.first()
        assertEquals("chat.local", saved.host)
        assertEquals(8181, saved.port)
        assertEquals(ThemeMode.LIGHT, saved.themeMode)
        assertFalse(viewModel.uiState.value.hasUnsavedChanges)
        assertFalse(viewModel.uiState.value.isSaveEnabled)
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun logoutClicked_logsOutAndEmitsBackEvent() = runTest(dispatcher) {
        val authRepository = FakeAuthRepository()
        val viewModel = SettingsViewModel(
            pref = createPreferencesDataSource(),
            authRepository = authRepository
        )
        advanceUntilIdle()
        val backEvent = async(start = CoroutineStart.UNDISPATCHED) {
            viewModel.uiEvent.first()
        }

        viewModel.onAction(SettingsUiAction.OnLogoutClicked)
        advanceUntilIdle()

        assertTrue(authRepository.logoutCalled)
        assertEquals(SettingsUiEvent.OnBackPressed, backEvent.await())
    }

    @Test
    fun backPressed_emitsBackEvent() = runTest(dispatcher) {
        val viewModel = SettingsViewModel(
            pref = createPreferencesDataSource(),
            authRepository = FakeAuthRepository()
        )
        advanceUntilIdle()
        val backEvent = async(start = CoroutineStart.UNDISPATCHED) {
            viewModel.uiEvent.first()
        }

        viewModel.onAction(SettingsUiAction.OnBackPressed)
        advanceUntilIdle()

        assertEquals(SettingsUiEvent.OnBackPressed, backEvent.await())
    }

    private suspend fun createPreferencesDataSource(
        initialSettings: AppSettings = AppSettings.default()
    ): PreferencesDataSource {
        val preferences = PreferencesDataSource(
            dataStore = InMemoryPreferencesDataStore(),
            dispatchers = TestAppDispatchers(dispatcher)
        )
        preferences.saveAppSettings(initialSettings)
        return preferences
    }
}

private class FakeAuthRepository : AuthRepository {
    var logoutCalled: Boolean = false

    override suspend fun login(login: String, pass: String): Result<Unit> = Result.success(Unit)

    override suspend fun logout() {
        logoutCalled = true
    }
}
