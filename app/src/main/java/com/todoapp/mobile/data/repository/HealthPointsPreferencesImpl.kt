package com.todoapp.mobile.data.repository

import com.todoapp.mobile.domain.repository.HealthCheckpoint
import com.todoapp.mobile.domain.repository.HealthPointsPreferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthPointsPreferencesImpl
@Inject
constructor(
    private val dataStoreHelper: DataStoreHelper,
) : HealthPointsPreferences {
    override suspend fun getCheckpoint(): HealthCheckpoint = dataStoreHelper.getHealthCheckpoint()

    override suspend fun setCheckpoint(
        settledHalfHearts: Int,
        lastSettledEpochDay: Long,
        dialogShown: Boolean,
    ) {
        dataStoreHelper.setHealthCheckpoint(settledHalfHearts, lastSettledEpochDay, dialogShown)
    }

    override fun observeDialogShown(): Flow<Boolean> = dataStoreHelper.observeHealthDialogShown()

    override suspend fun setDialogShown(shown: Boolean) {
        dataStoreHelper.setHealthDialogShown(shown)
    }

    override suspend fun clear() {
        dataStoreHelper.clearHealthPoints()
    }
}
