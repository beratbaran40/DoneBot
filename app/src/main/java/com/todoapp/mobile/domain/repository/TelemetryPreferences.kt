package com.todoapp.mobile.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * User consent for sharing anonymous performance/diagnostics data (Firebase Performance Monitoring,
 * §3.10). Default false — opt-in. The single source of truth the app pushes to
 * FirebasePerformance's collection flag on every change; also the seed of the broader §7.3
 * telemetry opt-out.
 */
interface TelemetryPreferences {
    fun observe(): Flow<Boolean>

    suspend fun get(): Boolean

    suspend fun set(enabled: Boolean)
}
