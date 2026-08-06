package com.todoapp.mobile.data.update

import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import com.todoapp.mobile.BuildConfig
import com.todoapp.mobile.di.IoDispatcher
import com.todoapp.mobile.domain.update.AppUpdateChecker
import dagger.hilt.android.qualifiers.ApplicationContext
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
 * Every failure resolves to "no update". The check talks to Play services over a binder and reaches
 * the network, so it fails routinely and for reasons that are none of the user's business: a debug
 * build installed over ADB, a device without Play, a plane. None of those are worth a dialog, and
 * none of them should keep the app from starting — hence the timeout as well as the catch.
 */
@Singleton
class PlayAppUpdateChecker
@Inject
constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AppUpdateChecker {
    override suspend fun isUpdateAvailable(): Boolean = withContext(ioDispatcher) {
        // Play answers "no update" for anything it did not install, which includes every debug build
        // — so there is otherwise no way to look at the dialog before shipping it. Flip the constant
        // locally to see it; R8 drops the whole branch from release.
        if (BuildConfig.DEBUG && FORCE_UPDATE_AVAILABLE_IN_DEBUG) return@withContext true
        try {
            withTimeoutOrNull(CHECK_TIMEOUT_MS) {
                val info = AppUpdateManagerFactory.create(context).requestAppUpdateInfo()
                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            } ?: false
        } catch (e: CancellationException) {
            // The caller's scope going away is not a failed check — let it propagate rather than
            // reporting "no update" for a question nobody is waiting on any more.
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).d(e, "update check unavailable")
            false
        }
    }

    private companion object {
        const val TAG: String = "AppUpdateChecker"
        const val CHECK_TIMEOUT_MS: Long = 5_000L

        /** Debug-only escape hatch — see [isUpdateAvailable]. Must be false on every commit. */
        const val FORCE_UPDATE_AVAILABLE_IN_DEBUG: Boolean = false
    }
}
