package com.todoapp.mobile.data.notification

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.todoapp.mobile.domain.engine.PomodoroEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class PomodoroForegroundService : Service() {
    @Inject
    lateinit var engine: PomodoroEngine

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observerJob: Job? = null
    private var startedAsForeground: Boolean = false

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
        val notification = PomodoroNotificationBuilder.build(this, engine.state.value)
        if (startedAsForeground) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // MEDIA_PLAYBACK rides along with SPECIAL_USE because this service also hosts the
                // ambience loop when the user enables background playback. Declaring both keeps the
                // runtime type consistent with the manifest whichever of the two is doing work.
                startForeground(
                    PomodoroNotificationBuilder.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
                )
            } else {
                startForeground(PomodoroNotificationBuilder.NOTIFICATION_ID, notification)
            }
            startedAsForeground = true
        }.onFailure { Timber.tag(TAG).w(it, "startForeground failed") }
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
    }

    private fun stopSelfAndNotification() {
        observerJob?.cancel()
        observerJob = null
        startedAsForeground = false
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
