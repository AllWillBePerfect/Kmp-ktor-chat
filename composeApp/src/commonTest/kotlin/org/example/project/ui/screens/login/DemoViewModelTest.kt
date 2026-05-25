package org.example.project.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.example.project.testutils.MainDispatcherTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class DemoViewModelTest : MainDispatcherTest() {

    @Test
    fun testLoadMessage() = runTest(dispatcher) {
        val viewModel = DemoViewModel()
        viewModel.loadMessage()
        runCurrent()
        assertEquals("Greetings!", viewModel.message.value)
    }


}

private class DemoViewModel : ViewModel() {
    private val _message = MutableStateFlow("")
    val message =  _message.asStateFlow()

    fun loadMessage() {
        viewModelScope.launch {
            _message.value = "Greetings!"
        }
    }
}
