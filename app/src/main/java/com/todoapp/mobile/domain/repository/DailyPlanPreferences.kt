package com.todoapp.mobile.domain.repository

import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

data class DailyCardPosition(
    val cardPositionX: Float = 0f,
    val cardPositionY: Float = 0f,
)

interface DailyPlanPreferences {
    fun observePlanTime(): Flow<LocalTime?>

    suspend fun savePlanTime(time: LocalTime)

    /**
     * Whether the daily "plan your day" reminder fires at all. Defaults to **true** so current users
     * keep the behaviour they have. Until this existed there was no off switch anywhere: the startup
     * and boot sweeps armed the reminder at the 09:00 default even for someone who had never opened
     * the Plan Your Day screen, and nothing in the app could stop it.
     */
    fun observeEnabled(): Flow<Boolean>

    suspend fun setEnabled(enabled: Boolean)

    fun observeCardPosition(): Flow<DailyCardPosition>

    suspend fun saveCardPosition(position: DailyCardPosition)
}
