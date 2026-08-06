package com.todoapp.mobile.data.repository

import com.todoapp.mobile.domain.repository.DailyCardPosition
import com.todoapp.mobile.domain.repository.DailyPlanPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class DailyPlanPreferencesImpl
@Inject
constructor(
    private val dataStoreHelper: DataStoreHelper,
) : DailyPlanPreferences {
    companion object {
        private const val PLAN_TIME_KEY = "daily_plan_time"
        private const val ENABLED_KEY = "daily_plan_enabled"
        private const val CARD_POSITION_X_KEY = "daily_plan_card_position_x"
        private const val CARD_POSITION_Y_KEY = "daily_plan_card_position_y"
        private val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }

    override fun observePlanTime(): Flow<LocalTime?> = dataStoreHelper.observeOptionalString(
        PLAN_TIME_KEY
    ).map { value ->
        if (value.isNullOrBlank()) {
            null
        } else {
            runCatching { LocalTime.parse(value, FORMATTER) }.getOrNull()
        }
    }

    override suspend fun savePlanTime(time: LocalTime) {
        dataStoreHelper.saveString(PLAN_TIME_KEY, time.format(FORMATTER))
    }

    // Stored as a string rather than a boolean so "never set" stays distinguishable from "off", and
    // an absent key reads as enabled — the behaviour every existing install already has.
    override fun observeEnabled(): Flow<Boolean> = dataStoreHelper.observeOptionalString(ENABLED_KEY)
        .map { it?.toBooleanStrictOrNull() ?: true }

    override suspend fun setEnabled(enabled: Boolean) {
        dataStoreHelper.saveString(ENABLED_KEY, enabled.toString())
    }

    override fun observeCardPosition(): Flow<DailyCardPosition> = combine(
        dataStoreHelper.observeOptionalString(CARD_POSITION_X_KEY),
        dataStoreHelper.observeOptionalString(CARD_POSITION_Y_KEY),
    ) { x, y ->
        DailyCardPosition(
            x?.toFloatOrNull() ?: 0f,
            y?.toFloatOrNull() ?: 0f,
        )
    }

    override suspend fun saveCardPosition(position: DailyCardPosition) {
        dataStoreHelper.saveString(CARD_POSITION_X_KEY, position.cardPositionX.toString())
        dataStoreHelper.saveString(CARD_POSITION_Y_KEY, position.cardPositionY.toString())
    }
}
