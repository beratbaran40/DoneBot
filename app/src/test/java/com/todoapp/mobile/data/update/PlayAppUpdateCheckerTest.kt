package com.todoapp.mobile.data.update

import android.content.Context
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.model.InstallErrorCode
import com.todoapp.mobile.domain.update.AppUpdateStatus
import com.todoapp.mobile.util.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * These are the tests the feature never had. The ViewModel's tests mock [AppUpdateChecker] and so only
 * ever proved "if Play says yes, the dialog opens" — never that we ask Play the right question or read
 * its answer correctly, which is the half that decides whether anyone ever sees the dialog.
 *
 * `FakeAppUpdateManager` ships inside `app-update` itself (there is no separate testing artifact) and
 * returns already-completed Tasks, which the ktx `requestAppUpdateInfo` resumes inline — so no main
 * looper needs idling here.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class PlayAppUpdateCheckerTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context: Context = RuntimeEnvironment.getApplication()

    private fun checker(manager: AppUpdateManager) = PlayAppUpdateChecker(manager, mainDispatcherRule.dispatcher)

    @Test
    fun `a newer build on Play is reported as available`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val fake = FakeAppUpdateManager(context).apply { setUpdateAvailable(11) }

        assertEquals(AppUpdateStatus.Available, checker(fake).check())
    }

    @Test
    fun `nothing to install is a definitive no, not an unknown`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val fake = FakeAppUpdateManager(context).apply { setUpdateNotAvailable() }

        // The distinction is the whole point of the type: NotAvailable stops the resume retry,
        // Unknown keeps it going.
        assertEquals(AppUpdateStatus.NotAvailable, checker(fake).check())
    }

    @Test
    fun `a Play failure is unknown rather than an error or a no`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val fake = FakeAppUpdateManager(context).apply {
            setInstallErrorCode(InstallErrorCode.ERROR_APP_NOT_OWNED)
        }

        // This is what a sideloaded or debug build actually hits. Reporting it as NotAvailable would
        // silently retire the retry for every user whose check simply could not run.
        assertEquals(AppUpdateStatus.Unknown, checker(fake).check())
    }

    @Test
    fun `a Play call that never answers times out to unknown`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val neverCompletes = TaskCompletionSource<AppUpdateInfo>().task
        val manager = mockk<AppUpdateManager>()
        every { manager.appUpdateInfo } returns neverCompletes

        val checked = async { checker(manager).check() }
        advanceTimeBy(CHECK_TIMEOUT_MS + 1)

        assertEquals(AppUpdateStatus.Unknown, checked.await())
    }

    private companion object {
        /** Mirrors PlayAppUpdateChecker.CHECK_TIMEOUT_MS — raising one without the other fails here. */
        const val CHECK_TIMEOUT_MS: Long = 10_000L
    }
}
