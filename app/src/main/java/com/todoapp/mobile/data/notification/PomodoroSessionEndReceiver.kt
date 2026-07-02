package com.todoapp.mobile.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.todoapp.mobile.R
import com.todoapp.mobile.common.RingtoneHolder
import kotlin.concurrent.thread
import com.example.uikit.R as UikitR

class PomodoroSessionEndReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // Building a Ringtone (RingtoneManager.getRingtone / getDefaultUri) touches the media
        // provider — synchronous binder/IO. Doing it inline on the receiver's main thread risks an
        // ANR, so hand off to a background thread and keep the receiver alive via goAsync().
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        thread {
            try {
                PomodoroNotificationChannels.ensurePomodoroChannel(appContext)
                RingtoneHolder().play(context = appContext)

                val notification = NotificationCompat
                    .Builder(appContext, PomodoroNotificationChannels.LIVE_CHANNEL_ID)
                    .setSmallIcon(UikitR.drawable.ic_sand_clock)
                    .setContentTitle(appContext.getString(R.string.pomodoro_notification_session_complete))
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(true)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .build()

                runCatching {
                    NotificationManagerCompat.from(appContext)
                        .notify(NOTIFICATION_ID_END, notification)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_SESSION_END: String = "com.todoapp.mobile.pomodoro.action.SESSION_END_BACKUP"
        private const val NOTIFICATION_ID_END: Int = 4243
    }
}
