package com.todoapp.mobile.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.todoapp.mobile.data.model.network.data.UserData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject

class DataStoreHelper
@Inject
constructor(
    private val dataStore: DataStore<Preferences>,
) {
    fun getString(
        key: String,
        defaultValue: String = "",
    ): Flow<String> {
        val prefKey = stringPreferencesKey(key)
        return dataStore.data.map { preferences ->
            preferences[prefKey] ?: defaultValue
        }
    }

    fun observeOptionalString(key: String): Flow<String?> {
        val prefKey = stringPreferencesKey(key)
        return dataStore.data.map { preferences ->
            preferences[prefKey]
        }
    }

    suspend fun saveString(
        key: String,
        value: String,
    ) {
        val prefKey = stringPreferencesKey(key)
        dataStore.edit { preferences ->
            preferences[prefKey] = value
        }
    }

    fun observeUser(): Flow<UserData?> = dataStore.data.map { preferences ->
        preferences[USER_KEY]?.let { rawJson ->
            runCatching { json.decodeFromString<UserData>(rawJson) }
                .getOrNull()
        }
    }

    suspend fun setUser(userData: UserData) {
        val rawJson = json.encodeToString(userData)
        dataStore.edit { preferences ->
            preferences[USER_KEY] = rawJson
        }
    }

    suspend fun clearUser() {
        dataStore.edit { preferences ->
            preferences.remove(USER_KEY)
        }
    }

    /**
     * Monotonic cache-bust token for the current user's avatar. Bumped exactly once on a successful
     * avatar upload so every surface (top bar, profile, settings) can append `?v=<token>` and force
     * Coil to refetch. Kept separate from [UserData] so unrelated user writes (token rotation, name
     * change) don't trigger a wasteful avatar refetch, and so the write always re-emits to live
     * collectors even when the backend returns an unchanged avatar path.
     */
    fun observeAvatarVersion(): Flow<Long> = dataStore.data.map { preferences ->
        preferences[AVATAR_VERSION] ?: 0L
    }

    suspend fun bumpAvatarVersion() {
        dataStore.edit { preferences ->
            preferences[AVATAR_VERSION] = System.currentTimeMillis()
        }
    }

    val isLoggedIn: Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[IS_LOGGED_IN] ?: false
        }

    suspend fun setLoggedIn(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = value
        }
    }

    fun observeFirstLoginPermissionPromptPending(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[FIRST_LOGIN_PERMISSION_PROMPT_PENDING] ?: false
    }

    suspend fun setFirstLoginPermissionPromptPending(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[FIRST_LOGIN_PERMISSION_PROMPT_PENDING] = value
        }
    }

    /**
     * Last reminder offset (minutes) the user picked when creating a task.
     * Used as a smart default for the next AddTaskSheet open. Negative
     * sentinel (-1) means "no reminder"; null means never set.
     */
    fun observeLastUsedReminderOffset(): Flow<Long?> = dataStore.data.map { preferences ->
        preferences[LAST_USED_REMINDER_OFFSET]
    }

    suspend fun setLastUsedReminderOffset(value: Long?) {
        dataStore.edit { preferences ->
            if (value == null) preferences.remove(LAST_USED_REMINDER_OFFSET)
            else preferences[LAST_USED_REMINDER_OFFSET] = value
        }
    }

    /**
     * Epoch day on which the user last dismissed the home suggest card.
     * Card is hidden for that day only; reappears the next day.
     */
    fun observeSuggestCardDismissedDay(): Flow<Long?> = dataStore.data.map { preferences ->
        preferences[SUGGEST_CARD_DISMISSED_DAY]
    }

    suspend fun setSuggestCardDismissedDay(epochDay: Long) {
        dataStore.edit { preferences ->
            preferences[SUGGEST_CARD_DISMISSED_DAY] = epochDay
        }
    }

    fun observeChatDraft(): Flow<String> = dataStore.data.map { preferences ->
        preferences[CHAT_DRAFT] ?: ""
    }

    suspend fun setChatDraft(value: String) {
        dataStore.edit { preferences ->
            if (value.isEmpty()) {
                preferences.remove(CHAT_DRAFT)
            } else {
                preferences[CHAT_DRAFT] = value
            }
        }
    }

    /**
     * The chat prompt a signed-out user was blocked on. Persisted (not just held in the
     * ViewModel) so it survives the login round-trip + ViewModel recreation, letting the
     * chat auto-resend it once the user signs in. One-shot read.
     */
    suspend fun getPendingChatPrompt(): String = dataStore.data.map { preferences ->
        preferences[PENDING_CHAT_PROMPT] ?: ""
    }.first()

    suspend fun setPendingChatPrompt(value: String) {
        dataStore.edit { preferences ->
            if (value.isEmpty()) {
                preferences.remove(PENDING_CHAT_PROMPT)
            } else {
                preferences[PENDING_CHAT_PROMPT] = value
            }
        }
    }

    fun observeReduceMotion(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[REDUCE_MOTION] ?: false
    }

    suspend fun setReduceMotion(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[REDUCE_MOTION] = value
        }
    }

    fun observeJournalBiometricProtected(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[JOURNAL_BIOMETRIC_PROTECTED] ?: false
    }

    suspend fun setJournalBiometricProtected(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[JOURNAL_BIOMETRIC_PROTECTED] = value
        }
    }

    /**
     * Whether the user opted in to sharing anonymous performance diagnostics (§3.10). Default false
     * (opt-in): perf collection stays off until the user flips the Settings toggle. Single source of
     * truth that [com.todoapp.mobile.domain.repository.TelemetryPreferences] exposes and the app pushes
     * to FirebasePerformance on every change.
     */
    fun observePerfCollectionEnabled(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PERF_COLLECTION_ENABLED] ?: false
    }

    suspend fun setPerfCollectionEnabled(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[PERF_COLLECTION_ENABLED] = value
        }
    }

    /**
     * Whether the user allows anonymous crash reporting + product analytics (§7.3). Default true
     * (opt-out): unlike perf ([observePerfCollectionEnabled], opt-in), crash/usage telemetry stays on for
     * users who never open Settings and is suppressed only when they flip it off. The app pushes this to
     * the FirebaseCrashlytics + FirebaseAnalytics collection flags on every change.
     */
    fun observeCrashAnalyticsEnabled(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[CRASH_ANALYTICS_ENABLED] ?: true
    }

    suspend fun setCrashAnalyticsEnabled(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[CRASH_ANALYTICS_ENABLED] = value
        }
    }

    /**
     * One-shot guard for the pre-v20 journal orphan claim. Set to true after the existing
     * (unscoped) journal entries have been assigned to a logged-in owner exactly once, so the
     * backfill never re-runs and a later different user can't re-claim them.
     */
    suspend fun isJournalOrphansClaimed(): Boolean = dataStore.data.map { it[JOURNAL_ORPHANS_CLAIMED] ?: false }.first()

    suspend fun setJournalOrphansClaimed(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[JOURNAL_ORPHANS_CLAIMED] = value
        }
    }

    /**
     * Device-local map of blocked users (id -> display name) for §6.18 UGC moderation. Blocking is
     * client-side: a blocked member is hidden from group member lists on this device. The name is
     * kept so the "Blocked users" management screen can show who was blocked — blocked members are
     * hidden everywhere else, so their profile is no longer reachable to unblock from. Stored as a
     * JSON string (DataStore has no map key type).
     */
    fun observeBlockedUsers(): Flow<Map<Long, String>> = dataStore.data.map { preferences ->
        decodeBlocked(preferences[BLOCKED_USERS])
    }

    suspend fun blockUser(userId: Long, displayName: String) {
        dataStore.edit { preferences ->
            val current = decodeBlocked(preferences[BLOCKED_USERS])
            preferences[BLOCKED_USERS] = encodeBlocked(current + (userId to displayName))
        }
    }

    suspend fun unblockUser(userId: Long) {
        dataStore.edit { preferences ->
            val current = decodeBlocked(preferences[BLOCKED_USERS])
            preferences[BLOCKED_USERS] = encodeBlocked(current - userId)
        }
    }

    private fun decodeBlocked(raw: String?): Map<Long, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching { json.decodeFromString<Map<String, String>>(raw) }
            .getOrDefault(emptyMap())
            .mapNotNull { (key, value) -> key.toLongOrNull()?.let { it to value } }
            .toMap()
    }

    private fun encodeBlocked(map: Map<Long, String>): String = json.encodeToString(map.mapKeys { it.key.toString() })

    companion object {
        private val json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                explicitNulls = false
            }
        private val USER_KEY = stringPreferencesKey("user")
        private val AVATAR_VERSION = longPreferencesKey("avatar_version")
        private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val FIRST_LOGIN_PERMISSION_PROMPT_PENDING =
            booleanPreferencesKey("first_login_permission_prompt_pending")
        private val LAST_USED_REMINDER_OFFSET = longPreferencesKey("last_used_reminder_offset")
        private val SUGGEST_CARD_DISMISSED_DAY = longPreferencesKey("suggest_card_dismissed_day")
        private val CHAT_DRAFT = stringPreferencesKey("chat_draft")
        private val PENDING_CHAT_PROMPT = stringPreferencesKey("pending_chat_prompt")
        private val REDUCE_MOTION = booleanPreferencesKey("accessibility_reduce_motion")
        private val JOURNAL_BIOMETRIC_PROTECTED = booleanPreferencesKey("journal_biometric_protection")
        private val JOURNAL_ORPHANS_CLAIMED = booleanPreferencesKey("journal_orphans_claimed")
        private val PERF_COLLECTION_ENABLED = booleanPreferencesKey("perf_collection_enabled")
        private val CRASH_ANALYTICS_ENABLED = booleanPreferencesKey("crash_analytics_enabled")
        private val BLOCKED_USERS = stringPreferencesKey("blocked_users")
    }
}
