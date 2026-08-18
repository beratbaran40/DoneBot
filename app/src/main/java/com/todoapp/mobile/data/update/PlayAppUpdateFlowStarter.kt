package com.todoapp.mobile.data.update

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import com.todoapp.mobile.di.IoDispatcher
import com.todoapp.mobile.di.MainDispatcher
import com.todoapp.mobile.domain.update.AppUpdateFlowStarter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Starts and resumes Play's immediate update flow.
 *
 * Both entry points re-request the `AppUpdateInfo` rather than reusing the one the availability check
 * saw: an info object is good for exactly one `startUpdateFlowForResult` call, so passing the old one
 * along would fail. The fetch runs on IO; handing the intent sender to Play runs on Main.
 */
@Singleton
class PlayAppUpdateFlowStarter
@Inject
constructor(
    private val appUpdateManager: AppUpdateManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
) : AppUpdateFlowStarter {
    override suspend fun startImmediateUpdate(
        launcher: ActivityResultLauncher<IntentSenderRequest>,
    ): Boolean = runFlow(launcher, "start") { info ->
        info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
            info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
    }

    override suspend fun resumeInProgressUpdate(
        launcher: ActivityResultLauncher<IntentSenderRequest>,
    ): Boolean = runFlow(launcher, "resume") { info ->
        // Deliberately no isUpdateTypeAllowed check: resuming is not a fresh decision, and Play's own
        // guidance re-enters the flow on this availability alone.
        info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
    }

    private suspend fun runFlow(
        launcher: ActivityResultLauncher<IntentSenderRequest>,
        what: String,
        shouldStart: (AppUpdateInfo) -> Boolean,
    ): Boolean = try {
        val info = withContext(ioDispatcher) { appUpdateManager.requestAppUpdateInfo() }
        val canStart = shouldStart(info)
        if (canStart) {
            withContext(mainDispatcher) {
                appUpdateManager.startUpdateFlowForResult(
                    info,
                    launcher,
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                )
            }
        }
        canStart
    } catch (e: CancellationException) {
        throw e
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        Timber.tag(TAG).w(e, "could not $what the in-app update flow")
        false
    }

    private companion object {
        const val TAG: String = "AppUpdate"
    }
}
