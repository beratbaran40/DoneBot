package com.todoapp.mobile.data.update

import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import com.todoapp.mobile.BuildConfig
import com.todoapp.mobile.di.IoDispatcher
import com.todoapp.mobile.domain.update.AppUpdateChecker
import com.todoapp.mobile.domain.update.AppUpdateStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Asks Play whether the installed build is behind the one on the store.
 *
 * Nothing here throws. The check talks to Play over a binder and reaches the network, so it fails
 * routinely and for reasons that are none of the user's business: a debug build installed over ADB, a
 * device without Play, a plane. Those are [AppUpdateStatus.Unknown] — worth asking again later — and
 * are deliberately not the same answer as Play saying there is genuinely nothing to install.
 */
@Singleton
class PlayAppUpdateChecker
@Inject
constructor(
    private val appUpdateManager: AppUpdateManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AppUpdateChecker {
    override suspend fun check(): AppUpdateStatus = withContext(ioDispatcher) {
        // Play answers "no update" for anything it did not install, which includes every debug build,
        // so there is otherwise no way to look at the dialog before shipping it. Set
        // `forceUpdateAvailable=true` in local.properties; R8 drops the branch from release.
        if (BuildConfig.DEBUG && BuildConfig.FORCE_UPDATE_AVAILABLE) {
            Timber.tag(TAG).w("forcing Available — local.properties forceUpdateAvailable is on")
            return@withContext AppUpdateStatus.Available
        }
        try {
            withTimeoutOrNull(CHECK_TIMEOUT_MS) {
                val availability = appUpdateManager.requestAppUpdateInfo().updateAvailability()
                availability.toStatus().also { Timber.tag(TAG).w("Play availability=$availability -> $it") }
            } ?: AppUpdateStatus.Unknown.also { Timber.tag(TAG).w("update check timed out after ${CHECK_TIMEOUT_MS}ms") }
        } catch (e: CancellationException) {
            // The caller's scope going away is not a failed check — let it propagate rather than
            // reporting an answer for a question nobody is waiting on any more.
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            // WARN, not DEBUG: CrashlyticsTree drops anything below WARN, so a debug-level line here
            // would not even survive as a breadcrumb on the only builds Play will actually answer for.
            Timber.tag(TAG).w(e, "update check unavailable")
            AppUpdateStatus.Unknown
        }
    }

    private fun Int.toStatus(): AppUpdateStatus = when (this) {
        UpdateAvailability.UPDATE_AVAILABLE -> AppUpdateStatus.Available
        UpdateAvailability.UPDATE_NOT_AVAILABLE -> AppUpdateStatus.NotAvailable
        UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> AppUpdateStatus.InProgress
        // UNKNOWN is what Play returns for a package it has no catalog entry for — a sideloaded or
        // debug build. Not a definitive "nothing to install", so it stays retryable.
        else -> AppUpdateStatus.Unknown
    }

    private companion object {
        const val TAG: String = "AppUpdateChecker"

        // requestAppUpdateInfo binds to the Play Store over a binder (which can cold-start that
        // process) and then makes a network round trip. 5s put a cold launch inside the failure
        // distribution; the check blocks nothing on screen, so a longer ceiling costs the user
        // nothing. It stays bounded so a wedged binder cannot leak a coroutine for the process.
        const val CHECK_TIMEOUT_MS: Long = 10_000L
    }
}
