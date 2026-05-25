package org.example.project.testutils

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@OptIn(ExperimentalCoroutinesApi::class)
abstract class MainDispatcherTest {
    protected val dispatcher: TestDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUpMainDispatcher() {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDownMainDispatcher() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }
}
