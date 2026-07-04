package com.todoapp.mobile.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * User consent for anonymous crash reporting (Crashlytics) + product analytics (§7.3). Opt-OUT: default
 * true, so telemetry stays on for the majority who never open Settings and only stops when they flip it
 * off. Distinct from the opt-IN perf toggle ([TelemetryPreferences]) because crash/usage data and
 * performance traces carry different privacy weight and different sensible defaults.
 */
interface CrashAnalyticsPreferences {
    fun observe(): Flow<Boolean>

    suspend fun get(): Boolean

    suspend fun set(enabled: Boolean)
}
