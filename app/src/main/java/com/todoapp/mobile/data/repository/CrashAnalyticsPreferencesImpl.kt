package com.todoapp.mobile.data.repository

import com.todoapp.mobile.domain.repository.CrashAnalyticsPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrashAnalyticsPreferencesImpl
@Inject
constructor(
    private val dataStoreHelper: DataStoreHelper,
) : CrashAnalyticsPreferences {
    override fun observe(): Flow<Boolean> = dataStoreHelper.observeCrashAnalyticsEnabled()

    override suspend fun get(): Boolean = dataStoreHelper.observeCrashAnalyticsEnabled().first()

    override suspend fun set(enabled: Boolean) {
        dataStoreHelper.setCrashAnalyticsEnabled(enabled)
    }
}
