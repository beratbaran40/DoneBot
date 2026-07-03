package com.todoapp.mobile.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit4 rule that swaps `Dispatchers.Main` for a [TestDispatcher] so ViewModels that launch on
 * `viewModelScope` (which dispatches on Main) can be driven deterministically from unit tests.
 *
 * Share [dispatcher]'s scheduler with `runTest(mainDispatcherRule.dispatcher.scheduler)` so that
 * `advanceUntilIdle()` in the test also advances the coroutines launched inside the ViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
