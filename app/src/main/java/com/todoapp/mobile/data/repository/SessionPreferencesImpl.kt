package com.todoapp.mobile.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.todoapp.mobile.data.auth.TokenCipher
import com.todoapp.mobile.domain.repository.SessionPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class SessionPreferencesImpl
@Inject
constructor(
    private val dataStore: DataStore<Preferences>,
    private val legacyPreferences: SharedPreferences,
    private val cipher: TokenCipher,
) : SessionPreferences {
    private val migrationMutex = Mutex()

    @Volatile
    private var migrated = false

    override suspend fun setAccessToken(token: String) {
        ensureMigrated()
        dataStore.edit { it[ACCESS_TOKEN_KEY] = cipher.encrypt(token) }
    }

    override suspend fun getAccessToken(): String? {
        ensureMigrated()
        return dataStore.data.map { it[ACCESS_TOKEN_KEY] }.first()?.let(cipher::decrypt)
    }

    override suspend fun setRefreshToken(token: String) {
        ensureMigrated()
        dataStore.edit { it[REFRESH_TOKEN_KEY] = cipher.encrypt(token) }
    }

    override suspend fun getRefreshToken(): String? {
        ensureMigrated()
        return dataStore.data.map { it[REFRESH_TOKEN_KEY] }.first()?.let(cipher::decrypt)
    }

    override fun observeRefreshToken(): Flow<String?> = flow {
        ensureMigrated()
        emitAll(dataStore.data.map { prefs -> prefs[REFRESH_TOKEN_KEY]?.let(cipher::decrypt) })
    }

    override suspend fun setExpiresAt(expiresIn: Long) {
        ensureMigrated()
        val expiresAtMillis = System.currentTimeMillis() + (expiresIn * MILLIS_IN_SECOND)
        dataStore.edit { it[EXPIRES_AT_KEY] = expiresAtMillis }
    }

    override suspend fun getExpiresAt(): Long {
        ensureMigrated()
        return dataStore.data.map { it[EXPIRES_AT_KEY] ?: 0L }.first()
    }

    override suspend fun clear(): Boolean {
        ensureMigrated()
        dataStore.edit {
            it.remove(ACCESS_TOKEN_KEY)
            it.remove(REFRESH_TOKEN_KEY)
            it.remove(EXPIRES_AT_KEY)
        }
        return true
    }

    private suspend fun ensureMigrated() {
        if (migrated) return
        migrationMutex.withLock {
            if (migrated) return
            migrateLegacyEncryptedPrefs()
            encryptExistingSessionTokens()
            migrated = true
        }
    }

    /** V1: tokens once lived in EncryptedSharedPreferences; pull any leftovers into DataStore (plaintext). */
    private suspend fun migrateLegacyEncryptedPrefs() {
        val alreadyDone = dataStore.data.map { it[MIGRATED_KEY] ?: false }.first()
        if (alreadyDone) return
        val legacyAccess = readLegacy(LEGACY_KEY_ACCESS_TOKEN)
        val legacyRefresh = readLegacy(LEGACY_KEY_REFRESH_TOKEN)
        val legacyExpires = readLegacyLong(LEGACY_KEY_EXPIRES_AT)
        dataStore.edit { prefs ->
            if (!legacyAccess.isNullOrBlank()) prefs[ACCESS_TOKEN_KEY] = legacyAccess
            if (!legacyRefresh.isNullOrBlank()) prefs[REFRESH_TOKEN_KEY] = legacyRefresh
            if (legacyExpires != 0L) prefs[EXPIRES_AT_KEY] = legacyExpires
            prefs[MIGRATED_KEY] = true
        }
        runCatching {
            legacyPreferences.edit {
                remove(LEGACY_KEY_ACCESS_TOKEN)
                remove(LEGACY_KEY_REFRESH_TOKEN)
                remove(LEGACY_KEY_EXPIRES_AT)
            }
        }
    }

    /**
     * V2: encrypt session tokens written as plaintext before value-encryption existed, and drop the
     * orphaned plaintext token copies the (removed) AuthTokenManager left in this same DataStore. Runs
     * once; afterwards [setAccessToken]/[setRefreshToken] keep values encrypted. Existing sessions survive
     * (no forced logout) because [TokenCipher.decrypt] passes plaintext through until this step runs.
     */
    private suspend fun encryptExistingSessionTokens() {
        val alreadyEncrypted = dataStore.data.map { it[VALUES_ENCRYPTED_KEY] ?: false }.first()
        if (alreadyEncrypted) return
        val currentAccess = dataStore.data.map { it[ACCESS_TOKEN_KEY] }.first()
        val currentRefresh = dataStore.data.map { it[REFRESH_TOKEN_KEY] }.first()
        dataStore.edit { prefs ->
            if (!currentAccess.isNullOrBlank()) prefs[ACCESS_TOKEN_KEY] = cipher.encrypt(currentAccess)
            if (!currentRefresh.isNullOrBlank()) prefs[REFRESH_TOKEN_KEY] = cipher.encrypt(currentRefresh)
            ORPHAN_TOKEN_KEYS.forEach { prefs.remove(stringPreferencesKey(it)) }
            prefs[VALUES_ENCRYPTED_KEY] = true
        }
    }

    private fun readLegacy(key: String): String? = runCatching {
        legacyPreferences.getString(key, null)
    }.getOrNull()

    private fun readLegacyLong(key: String): Long = runCatching {
        legacyPreferences.getLong(key, 0L)
    }.getOrDefault(0L)

    companion object {
        const val MILLIS_IN_SECOND = 1000L
        private const val LEGACY_KEY_ACCESS_TOKEN = "access_token"
        private const val LEGACY_KEY_REFRESH_TOKEN = "refresh_token"
        private const val LEGACY_KEY_EXPIRES_AT = "expires_at"
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("session_access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("session_refresh_token")
        private val EXPIRES_AT_KEY = longPreferencesKey("session_expires_at")
        private val MIGRATED_KEY = booleanPreferencesKey("session_migrated_to_datastore_v1")
        private val VALUES_ENCRYPTED_KEY = booleanPreferencesKey("session_values_encrypted_v1")
        private val ORPHAN_TOKEN_KEYS =
            listOf("access_token", "refresh_token", "user_id", "email", "display_name", "avatar_url")
    }
}
