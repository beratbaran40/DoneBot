package com.todoapp.mobile.domain.repository

import kotlinx.coroutines.flow.Flow

interface JournalBiometricPreferences {
    fun observe(): Flow<Boolean>

    suspend fun get(): Boolean

    suspend fun set(enabled: Boolean)
}
