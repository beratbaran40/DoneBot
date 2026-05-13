package com.todoapp.mobile.data.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.uikit.R
import com.todoapp.mobile.MainActivity
import com.todoapp.mobile.common.RingtoneHolder
import com.todoapp.mobile.di.IoDispatcher
import com.todoapp.mobile.domain.repository.AlarmSoundPreferences
import com.todoapp.mobile.ui.overlay.OverlayServiceChannel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationService : Service() {
    @Inject lateinit var alarmSoundPreferences: AlarmSoundPreferences

    @Inject @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }
    private val ringtone = RingtoneHolder()
    private val scope by lazy { CoroutineScope(SupervisorJob() + ioDispatcher) }

    fun sendNotification(
        contentText: String,
        remindMinutesBefore: Int,
    ) {
        val activityIntent = Intent(this, MainActivity::class.java)
        val activityPendingIntent =
            PendingIntent.getActivity(
                this,
                NOTIFICATION_ID,
                activityIntent,
                PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            NotificationCompat
                .Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_clock)
                .setContentTitle(
                    "You have a task to do! In ${if (remindMinutesBefore == 0) "now" else remindMinutesBefore} minutes.",
                ).setContentText(contentText)
                .setContentIntent(activityPendingIntent)
                .setAutoCancel(true)
                .build()
        notificationManager.notify(
            NOTIFICATION_ID,
            notification,
        )
        scope.launch {
            val uri = runCatching { alarmSoundPreferences.currentAlarmSoundUri() }.getOrNull()
            ringtone.play(context = this@NotificationService, explicitUri = uri)
        }
    }

    override fun onCreate() {
        super.onCreate()
        OverlayServiceChannel.ensure(this)
    }

    override fun onStartCommand(
        intent: Intent,
        flags: Int,
        startId: Int,
    ): Int {
        promoteToForeground()
        val message = intent.getStringExtra(INTENT_EXTRA_MESSAGE)
        val time = intent.getLongExtra(INTENT_EXTRA_LONG, 0L)
        if (!message.isNullOrBlank()) {
            sendNotification(message, time.toInt())
        }
        // Detach so the user-visible task reminder persists after the FG placeholder is gone.
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf(startId)
        return START_NOT_STICKY
    }

    private fun promoteToForeground() {
        val placeholder: Notification = NotificationCompat
            .Builder(this, OverlayServiceChannel.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_clock)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    OverlayServiceChannel.FOREGROUND_NOTIFICATION_ID,
                    placeholder,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            } else {
                startForeground(OverlayServiceChannel.FOREGROUND_NOTIFICATION_ID, placeholder)
            }
        }
    }

    override fun onDestroy() {
        ringtone.stop()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "notification_channel"
        const val INTENT_EXTRA_MESSAGE = "extra_message"
        const val INTENT_EXTRA_LONG = "extra_time"
        private const val NOTIFICATION_ID = 1
    }
}
