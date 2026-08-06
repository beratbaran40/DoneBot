package com.todoapp.mobile.data.notification

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PomodoroServiceController
@Inject
constructor(
    @ApplicationContext private val context: Context,
) {
    private val _isForegroundActive = MutableStateFlow(false)

    /**
     * Whether a foreground service is holding this process up **right now**.
     *
     * Reported by the service itself rather than inferred from [start]: starting one is a request,
     * not a guarantee. [start] declines outright without POST_NOTIFICATIONS, and even when it goes
     * through, `startForeground` can still fail inside the service. Anything that depends on the
     * process surviving the app being backgrounded — ambience, above all — has to read what actually
     * happened, not what was asked for.
     */
    val isForegroundActive: StateFlow<Boolean> = _isForegroundActive.asStateFlow()

    fun start() {
        if (!hasNotificationPermission()) {
            Timber.tag(TAG).d("POST_NOTIFICATIONS not granted; skipping live notification.")
            return
        }
        val intent = Intent(context, PomodoroForegroundService::class.java).apply {
            action = PomodoroForegroundService.ACTION_START
        }
        runCatching {
            ContextCompat.startForegroundService(context, intent)
        }.onFailure { Timber.tag(TAG).w(it, "startForegroundService failed") }
    }

    fun stop() {
        // Use stopService (not startService with ACTION_STOP) to avoid an infinite
        // loop: ACTION_STOP in onStartCommand calls engine.finish(), which calls
        // serviceController.stop() again — sending another ACTION_STOP intent. Direct
        // stopService bypasses onStartCommand entirely and goes through onDestroy.
        val intent = Intent(context, PomodoroForegroundService::class.java)
        runCatching { context.stopService(intent) }
            .onFailure { Timber.tag(TAG).w(it, "stopService failed") }
    }

    /** Called by [PomodoroForegroundService] once `startForeground` has actually succeeded. */
    internal fun onForegroundStarted() {
        _isForegroundActive.value = true
    }

    /** Called by [PomodoroForegroundService] on every teardown path. */
    internal fun onForegroundStopped() {
        _isForegroundActive.value = false
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private companion object {
        const val TAG: String = "PomodoroFgController"
    }
}
