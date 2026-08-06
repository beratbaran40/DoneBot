package com.todoapp.mobile.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.edit
import com.todoapp.mobile.domain.repository.FCMTokenPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject

class FCMTokenPreferencesImpl
@Inject
constructor(
    @ApplicationContext private val context: Context,
) : FCMTokenPreferences {
    // Lazy so the SharedPreferences file open (disk I/O) runs on first use, not during Hilt singleton
    // construction on the main thread at startup — StrictMode flagged the eager read as an ~82 ms
    // main-thread DiskReadViolation. First access is off the main thread (FCM token ops). §3.5
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    override fun getPendingToken(): String? = prefs.getString(KEY_PENDING_TOKEN, null)

    override fun getDeviceId(): String {
        val stored = prefs.getString(KEY_DEVICE_ID, null)
        if (stored != null) return stored

        val newId = UUID.randomUUID().toString()
        prefs.edit { putString(KEY_DEVICE_ID, newId) }
        return newId
    }

    override fun getDeviceName(): String = "${Build.MANUFACTURER} ${Build.MODEL}"

    override fun setPendingToken(token: String) {
        prefs.edit { putString(KEY_PENDING_TOKEN, token) }
    }

    override fun clearPendingToken() {
        prefs.edit { remove(KEY_PENDING_TOKEN) }
    }

    override fun getLastSentToken(): String? = prefs.getString(KEY_LAST_SENT_TOKEN, null)

    override fun setLastSentToken(token: String) {
        prefs.edit { putString(KEY_LAST_SENT_TOKEN, token) }
    }

    override fun getLastSentTimeZone(): String? = prefs.getString(KEY_LAST_SENT_TIME_ZONE, null)

    override fun setLastSentTimeZone(zoneId: String) {
        prefs.edit { putString(KEY_LAST_SENT_TIME_ZONE, zoneId) }
    }

    override fun clearAll() {
        prefs.edit { clear() }
    }

    companion object {
        private const val PREF_NAME = "fcm_token_prefs"
        private const val KEY_PENDING_TOKEN = "key_pending_token"
        private const val KEY_DEVICE_ID = "key_device_id"
        private const val KEY_LAST_SENT_TOKEN = "key_last_sent_token"
        private const val KEY_LAST_SENT_TIME_ZONE = "key_last_sent_time_zone"
    }
}
