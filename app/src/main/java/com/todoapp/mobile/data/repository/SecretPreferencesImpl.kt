package com.todoapp.mobile.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import com.todoapp.mobile.data.security.SecretModeEndCondition
import com.todoapp.mobile.di.IoDispatcher
import com.todoapp.mobile.domain.repository.SecretPreferences
import com.todoapp.mobile.domain.security.SecretModeEndEvent
import com.todoapp.mobile.domain.security.SecretModeReopenOptionId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SecretPreferencesImpl
@Inject
constructor(
    private val sharedPreferences: SharedPreferences,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SecretPreferences {
    override suspend fun saveCondition(condition: SecretModeEndCondition) {
        withContext(ioDispatcher) {
            sharedPreferences.edit {
                when (condition) {
                    is SecretModeEndCondition.UntilTime -> {
                        putString(KEY_CONDITION_TYPE, ConditionType.UNTIL_TIME.name)
                        putLong(KEY_UNTIL_EPOCH_MILLIS, condition.epochMillis)
                        remove(KEY_UNTIL_EVENT)
                    }
                    is SecretModeEndCondition.UntilEvent -> {
                        putString(KEY_CONDITION_TYPE, ConditionType.UNTIL_EVENT.name)
                        putString(KEY_UNTIL_EVENT, condition.event.name)
                        remove(KEY_UNTIL_EPOCH_MILLIS)
                    }
                    is SecretModeEndCondition.Disabled -> {
                        putString(KEY_CONDITION_TYPE, ConditionType.DISABLED.name)
                        remove(KEY_UNTIL_EPOCH_MILLIS)
                        remove(KEY_UNTIL_EVENT)
                    }
                }
            }
        }
    }

    override suspend fun getCondition(): SecretModeEndCondition = withContext(ioDispatcher) {
        val conditionType =
            sharedPreferences
                .getString(KEY_CONDITION_TYPE, ConditionType.DISABLED.name)
                ?.let { ConditionType.valueOf(it) } ?: ConditionType.DISABLED

        when (conditionType) {
            ConditionType.DISABLED -> SecretModeEndCondition.Disabled
            ConditionType.UNTIL_TIME -> {
                val until = sharedPreferences.getLong(KEY_UNTIL_EPOCH_MILLIS, 0L)
                if (until > 0L) {
                    SecretModeEndCondition.UntilTime(until)
                } else {
                    SecretModeEndCondition.Disabled
                }
            }
            ConditionType.UNTIL_EVENT -> {
                val raw = sharedPreferences.getString(KEY_UNTIL_EVENT, null)
                val event = raw?.let { SecretModeEndEvent.valueOf(it) }
                if (event != null) {
                    SecretModeEndCondition.UntilEvent(event)
                } else {
                    SecretModeEndCondition.Disabled
                }
            }
        }
    }

    override fun observeCondition(): Flow<SecretModeEndCondition> = callbackFlow {
        launch {
            val current =
                runCatching { getCondition() }
                    .getOrElse { SecretModeEndCondition.Disabled }
            trySend(current)
        }

        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (
                    key == KEY_CONDITION_TYPE ||
                    key == KEY_UNTIL_EPOCH_MILLIS ||
                    key == KEY_UNTIL_EVENT
                ) {
                    launch {
                        val current =
                            runCatching { getCondition() }
                                .getOrElse { SecretModeEndCondition.Disabled }
                        trySend(current)
                    }
                }
            }

        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)

        awaitClose {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    override suspend fun setLastSelectedOptionId(id: SecretModeReopenOptionId) {
        withContext(ioDispatcher) {
            sharedPreferences.edit { putString(KEY_LAST_SELECTED_OPTION_ID, id.name) }
        }
    }

    override suspend fun getLastSelectedOptionId(): SecretModeReopenOptionId = withContext(ioDispatcher) {
        val raw = sharedPreferences.getString(KEY_LAST_SELECTED_OPTION_ID, null)
        raw
            ?.let { SecretModeReopenOptionId.valueOf(it) }
            ?: SecretModeReopenOptionId.IMMEDIATE
    }

    private companion object {
        const val KEY_CONDITION_TYPE = "secret_mode_condition_type"
        const val KEY_UNTIL_EPOCH_MILLIS = "secret_mode_until_epoch_millis"
        const val KEY_UNTIL_EVENT = "secret_mode_until_event"

        const val KEY_LAST_SELECTED_OPTION_ID = "secret_mode_selected_option"

        enum class ConditionType { DISABLED, UNTIL_TIME, UNTIL_EVENT }
    }
}
