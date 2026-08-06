package com.todoapp.mobile.domain.repository

interface FCMTokenPreferences {
    fun getPendingToken(): String?

    fun getDeviceId(): String?

    fun getDeviceName(): String?

    fun setPendingToken(token: String)

    fun clearPendingToken()

    fun getLastSentToken(): String?

    fun setLastSentToken(token: String)

    /**
     * The IANA zone that went up with the last token registration. The token itself does not change
     * when the user travels, so without this the sync would short-circuit and the backend would keep
     * scheduling their due-soon reminders in the zone they left.
     */
    fun getLastSentTimeZone(): String?

    fun setLastSentTimeZone(zoneId: String)

    fun clearAll()
}
