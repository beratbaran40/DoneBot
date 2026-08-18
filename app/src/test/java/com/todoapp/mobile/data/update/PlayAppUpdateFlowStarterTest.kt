package com.todoapp.mobile.data.update

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.todoapp.mobile.util.MainDispatcherRule
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Covers the half of the feature that used to live as a private function inside a Compose file, where
 * nothing could reach it: starting Play's flow, and re-entering one it already began.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class PlayAppUpdateFlowStarterTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context: Context = RuntimeEnvironment.getApplication()
    private val launcher = mockk<ActivityResultLauncher<IntentSenderRequest>>(relaxed = true)

    private fun starter(fake: FakeAppUpdateManager) = PlayAppUpdateFlowStarter(fake, mainDispatcherRule.dispatcher, mainDispatcherRule.dispatcher)

    @Test
    fun `an available update starts the immediate flow`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val fake = FakeAppUpdateManager(context).apply { setUpdateAvailable(11) }

        assertTrue(starter(fake).startImmediateUpdate(launcher))
        assertTrue(fake.isImmediateFlowVisible)
    }

    @Test
    fun `no available update declines so the caller can fall back to the listing`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val fake = FakeAppUpdateManager(context).apply { setUpdateNotAvailable() }

        assertFalse(starter(fake).startImmediateUpdate(launcher))
        assertFalse(fake.isImmediateFlowVisible)
    }

    @Test
    fun `an update interrupted by process death is resumed`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val fake = FakeAppUpdateManager(context).apply { setUpdateAvailable(11) }
        starter(fake).startImmediateUpdate(launcher)
        fake.userAcceptsUpdate()

        // A fresh starter stands in for the new process: nothing is carried over in memory, which is
        // exactly the situation Play expects the app to recover from.
        assertTrue(starter(fake).resumeInProgressUpdate(launcher))
    }

    @Test
    fun `nothing in progress means nothing to resume`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val fake = FakeAppUpdateManager(context).apply { setUpdateNotAvailable() }

        assertFalse(starter(fake).resumeInProgressUpdate(launcher))
    }
}
