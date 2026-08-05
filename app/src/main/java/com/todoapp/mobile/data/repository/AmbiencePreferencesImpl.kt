package com.todoapp.mobile.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.todoapp.mobile.domain.ambience.PomodoroAmbience
import com.todoapp.mobile.domain.repository.AmbiencePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns its three DataStore keys rather than adding six more accessors to [DataStoreHelper] — that
 * facade is already at detekt's function ceiling, and these keys are read by nothing else.
 * [AlarmSoundPreferencesImpl] sets the same precedent with its local `alarm_sound_uri`.
 */
@Singleton
class AmbiencePreferencesImpl
@Inject
constructor(
    private val dataStore: DataStore<Preferences>,
) : AmbiencePreferences {
    override fun observeSelection(): Flow<PomodoroAmbience> = dataStore.data.map { PomodoroAmbience.fromId(it[SELECTION]) }

    override suspend fun setSelection(value: PomodoroAmbience) {
        dataStore.edit { it[SELECTION] = value.id }
    }

    override fun observeVolume(): Flow<Float> = dataStore.data.map { (it[VOLUME] ?: AmbiencePreferences.DEFAULT_VOLUME).coerceIn(0f, 1f) }

    override suspend fun setVolume(value: Float) {
        dataStore.edit { it[VOLUME] = value.coerceIn(0f, 1f) }
    }

    override fun observePlayInBackground(): Flow<Boolean> = dataStore.data.map { it[PLAY_IN_BACKGROUND] ?: false }

    override suspend fun setPlayInBackground(value: Boolean) {
        dataStore.edit { it[PLAY_IN_BACKGROUND] = value }
    }

    private companion object {
        val SELECTION = stringPreferencesKey("pomodoro_ambience_id")
        val VOLUME = floatPreferencesKey("pomodoro_ambience_volume")
        val PLAY_IN_BACKGROUND = booleanPreferencesKey("pomodoro_ambience_background")
    }
}
