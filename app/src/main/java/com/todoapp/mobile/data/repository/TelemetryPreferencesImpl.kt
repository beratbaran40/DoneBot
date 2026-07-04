package com.todoapp.mobile.data.repository

import com.todoapp.mobile.domain.repository.TelemetryPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelemetryPreferencesImpl
@Inject
constructor(
    private val dataStoreHelper: DataStoreHelper,
) : TelemetryPreferences {
    override fun observe(): Flow<Boolean> = dataStoreHelper.observePerfCollectionEnabled()

    override suspend fun get(): Boolean = dataStoreHelper.observePerfCollectionEnabled().first()

    override suspend fun set(enabled: Boolean) {
        dataStoreHelper.setPerfCollectionEnabled(enabled)
    }
}
