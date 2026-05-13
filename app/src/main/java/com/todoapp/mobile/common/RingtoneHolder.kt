package com.todoapp.mobile.common

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Handler
import android.os.Looper
import timber.log.Timber

class RingtoneHolder {
    private var ringtone: Ringtone? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var stopRunnable: Runnable? = null

    @Suppress("MagicNumber")
    private companion object {
        const val AUTO_STOP_MILLIS: Long = 2_000L

        // Used only for fallback tone duration.
        const val FALLBACK_TONE_MILLIS: Int = 800
        const val TONE_VOLUME: Int = 100
    }

    /**
     * Plays the given ringtone URI for [autoStopMillis] then stops. Pass `null` for [explicitUri]
     * to fall back to the system default alarm/notification sound (legacy behaviour).
     */
    fun play(
        context: Context,
        explicitUri: Uri? = null,
        autoStopMillis: Long = AUTO_STOP_MILLIS,
    ) {
        stop()

        // Cancel any previously scheduled stop.
        stopRunnable?.let(mainHandler::removeCallbacks)
        stopRunnable = null

        // Respect device silent / vibrate-only ringer modes: post the visual
        // notification but suppress alarm sound. USAGE_ALARM otherwise bypasses
        // silent mode on most Android versions.
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager?.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
            Timber.tag("RingtoneHolder").d("Ringer mode != NORMAL; suppressing alarm sound")
            return
        }

        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val uri = explicitUri ?: alarmUri ?: notificationUri

        if (uri == null) {
            // Last-resort fallback: a short tone.
            ToneGenerator(AudioManager.STREAM_ALARM, TONE_VOLUME)
                .startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, FALLBACK_TONE_MILLIS)
            return
        }

        ringtone = RingtoneManager.getRingtone(context, uri)?.apply {
            // On newer APIs you can set usage; safe-guard with runCatching.
            runCatching {
                audioAttributes =
                    AudioAttributes
                        .Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
            }
            play()
            // Auto-stop after a short duration (some devices loop indefinitely).
            val autoStop = Runnable { stop() }
            stopRunnable = autoStop
            mainHandler.postDelayed(autoStop, autoStopMillis)
        } ?: run {
            // Fallback tone if ringtone couldn't be created.
            ToneGenerator(AudioManager.STREAM_ALARM, TONE_VOLUME)
                .startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, FALLBACK_TONE_MILLIS)
            null
        }
    }

    fun stop() {
        stopRunnable?.let(mainHandler::removeCallbacks)
        stopRunnable = null

        runCatching { ringtone?.stop() }
        ringtone = null
    }
}
