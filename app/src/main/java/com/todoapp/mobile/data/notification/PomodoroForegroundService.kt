package com.todoapp.mobile.data.notification

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.todoapp.mobile.domain.ambience.AmbiencePlayer
import com.todoapp.mobile.domain.ambience.PomodoroAmbience
import com.todoapp.mobile.domain.engine.PomodoroEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class PomodoroForegroundService : Service() {
    @Inject
    lateinit var engine: PomodoroEngine

    @Inject
    lateinit var ambiencePlayer: AmbiencePlayer

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observerJob: Job? = null
    private var ambienceJob: Job? = null
    private var startedAsForeground: Boolean = false

    /** Types last handed to `startForeground`, so the call is repeated only when they change. */
    private var currentTypeMask: Int = 0

    override fun onCreate() {
        super.onCreate()
        PomodoroNotificationChannels.ensurePomodoroChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Timber.tag(TAG).d("onStartCommand action=%s isRunning=%s", action, engine.state.value.isRunning)
        when (action) {
            ACTION_PAUSE -> engine.pause()
            ACTION_RESUME -> engine.start()
            ACTION_SKIP -> engine.skip(autoStart = engine.state.value.isRunning)
            ACTION_STOP -> {
                engine.finish()
                stopSelfAndNotification()
                return START_NOT_STICKY
            }
        }

        ensureForeground()
        ensureObserver()
        return START_STICKY
    }

    private fun ensureForeground() {
        val desiredMask = foregroundTypeMask()
        if (startedAsForeground && desiredMask == currentTypeMask) return

        val notification = PomodoroNotificationBuilder.build(this, engine.state.value)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(PomodoroNotificationBuilder.NOTIFICATION_ID, notification, desiredMask)
            } else {
                startForeground(PomodoroNotificationBuilder.NOTIFICATION_ID, notification)
            }
            startedAsForeground = true
            currentTypeMask = desiredMask
        }.onFailure { Timber.tag(TAG).w(it, "startForeground failed") }
    }

    /**
     * The countdown is always specialUse; mediaPlayback is added **only** while a soundscape is
     * actually loaded.
     *
     * Declaring mediaPlayback unconditionally would have every session — including the default,
     * silent one — claim to be playing media while nothing is, which is precisely the mismatch
     * Android 14's type enforcement and Play's foreground-service review look for.
     *
     * The signal is "a soundscape is loaded" rather than "audio is audible right now": it is set
     * before playback begins (so the type is never declared late) and survives the brief pauses
     * that come with pausing the timer or ducking for a call, which keeps the service from
     * re-declaring its types every few seconds.
     */
    private fun foregroundTypeMask(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return 0
        val mask = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        return if (ambiencePlayer.state.value.current != PomodoroAmbience.None) {
            mask or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            mask
        }
    }

    private fun ensureObserver() {
        if (observerJob?.isActive == true) return
        observerJob = engine.state
            .onEach { snapshot ->
                val manager = androidx.core.app.NotificationManagerCompat.from(this)
                runCatching {
                    manager.notify(
                        PomodoroNotificationBuilder.NOTIFICATION_ID,
                        PomodoroNotificationBuilder.build(this, snapshot),
                    )
                }.onFailure { Timber.tag(TAG).w(it, "notify failed") }

                if (!snapshot.isRunning && !snapshot.isOvertime && snapshot.totalSessions == 0) {
                    stopSelfAndNotification()
                }
            }
            .launchIn(scope)

        // Picking a soundscape mid-session (or switching back to Silence) changes what this
        // service is actually doing, so its declared types have to follow.
        ambienceJob = ambiencePlayer.state
            .map { it.current != PomodoroAmbience.None }
            .distinctUntilChanged()
            .onEach { ensureForeground() }
            .launchIn(scope)
    }

    private fun stopSelfAndNotification() {
        observerJob?.cancel()
        observerJob = null
        // Cancel before tearing down, or a late ambience emission would call startForeground on a
        // service that is on its way out.
        ambienceJob?.cancel()
        ambienceJob = null
        startedAsForeground = false
        currentTypeMask = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        observerJob?.cancel()
        ambienceJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START: String = "com.todoapp.mobile.pomodoro.action.START"
        const val ACTION_PAUSE: String = "com.todoapp.mobile.pomodoro.action.PAUSE"
        const val ACTION_RESUME: String = "com.todoapp.mobile.pomodoro.action.RESUME"
        const val ACTION_SKIP: String = "com.todoapp.mobile.pomodoro.action.SKIP"
        const val ACTION_STOP: String = "com.todoapp.mobile.pomodoro.action.STOP"
        private const val TAG: String = "PomodoroFgService"
    }
}
