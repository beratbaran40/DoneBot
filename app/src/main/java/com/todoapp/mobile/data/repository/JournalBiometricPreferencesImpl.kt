package com.todoapp.mobile.data.repository

import com.todoapp.mobile.domain.repository.JournalBiometricPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JournalBiometricPreferencesImpl
@Inject
constructor(
    private val dataStoreHelper: DataStoreHelper,
) : JournalBiometricPreferences {
    override fun observe(): Flow<Boolean> = dataStoreHelper.observeJournalBiometricProtected()

    override suspend fun get(): Boolean = dataStoreHelper.observeJournalBiometricProtected().first()

    override suspend fun set(enabled: Boolean) {
        dataStoreHelper.setJournalBiometricProtected(enabled)
    }
}
