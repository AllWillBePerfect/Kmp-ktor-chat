package org.example.project.ui.screens.login

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.example.project.data.repository.AuthRepository
import org.example.project.testutils.MainDispatcherTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest : MainDispatcherTest() {

    @Test
    fun loginClicked_withBlankFields_setsValidationError() = runTest(dispatcher) {
        val viewModel = LoginViewModel(
            authRepository = FakeAuthRepository()
        )

        viewModel.onAction(LoginUiAction.LoginClicked)
        runCurrent()

        assertIs<LoginUiError.EmptyFields>(viewModel.uiState.value.error)
        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun loginClicked_withSuccessfulResponse_emitsNavigationEvent() = runTest(dispatcher) {
        val viewModel = LoginViewModel(
            authRepository = FakeAuthRepository(
                loginResult = Result.success(Unit)
            )
        )
        val navigationEvent = async(start = CoroutineStart.UNDISPATCHED) {
            viewModel.uiEvent.first()
        }

        viewModel.onAction(LoginUiAction.LoginTyped("alice"))
        viewModel.onAction(LoginUiAction.PasswordTyped("secret"))
        viewModel.onAction(LoginUiAction.LoginClicked)
        advanceUntilIdle()

        assertEquals(LoginUiEvent.NavigateToMain, navigationEvent.await())
    }

    @Test
    fun loginClicked_withErrorResponse_setsErrorState() = runTest(dispatcher) {
        val viewModel = LoginViewModel(
            authRepository = FakeAuthRepository(
                loginResult = Result.failure(Exception("Invalid credentials"))
            )
        )

        viewModel.onAction(LoginUiAction.LoginTyped("alice"))
        viewModel.onAction(LoginUiAction.PasswordTyped("wrong"))
        viewModel.onAction(LoginUiAction.LoginClicked)
        advanceUntilIdle()

        assertEquals(
            LoginUiError.AuthFailed("Invalid credentials"),
            viewModel.uiState.value.error
        )
        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun loginTyped_clearsExistingError() = runTest(dispatcher) {
        val viewModel = LoginViewModel(
            authRepository = FakeAuthRepository()
        )

        viewModel.onAction(LoginUiAction.LoginClicked)
        runCurrent()
        assertIs<LoginUiError.EmptyFields>(viewModel.uiState.value.error)

        viewModel.onAction(LoginUiAction.LoginTyped("alice"))

        assertEquals(null, viewModel.uiState.value.error)
        assertEquals("alice", viewModel.uiState.value.login)
    }

    @Test
    fun passwordTyped_clearsExistingError() = runTest(dispatcher) {
        val viewModel = LoginViewModel(
            authRepository = FakeAuthRepository()
        )

        viewModel.onAction(LoginUiAction.LoginClicked)
        runCurrent()
        assertIs<LoginUiError.EmptyFields>(viewModel.uiState.value.error)

        viewModel.onAction(LoginUiAction.PasswordTyped("secret"))

        assertEquals(null, viewModel.uiState.value.error)
        assertEquals("secret", viewModel.uiState.value.password)
    }

    @Test
    fun isButtonEnabled_reflectsCurrentState() = runTest(dispatcher) {
        val viewModel = LoginViewModel(
            authRepository = FakeAuthRepository()
        )

        assertEquals(false, viewModel.uiState.value.isButtonEnabled)

        viewModel.onAction(LoginUiAction.LoginTyped("alice"))
        assertEquals(false, viewModel.uiState.value.isButtonEnabled)

        viewModel.onAction(LoginUiAction.PasswordTyped("secret"))
        assertEquals(true, viewModel.uiState.value.isButtonEnabled)

        viewModel.onAction(LoginUiAction.LoginClicked)
        runCurrent()

        assertEquals(false, viewModel.uiState.value.isButtonEnabled)
    }
}

private class FakeAuthRepository(
    private val loginResult: Result<Unit> = Result.success(Unit)
) : AuthRepository {

    override suspend fun login(login: String, pass: String): Result<Unit> = loginResult

    override suspend fun logout() = Unit
}
