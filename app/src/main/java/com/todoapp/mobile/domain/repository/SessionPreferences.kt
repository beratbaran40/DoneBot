package com.todoapp.mobile.domain.repository

import kotlinx.coroutines.flow.Flow

interface SessionPreferences {
    suspend fun setAccessToken(token: String)

    suspend fun getAccessToken(): String?

    suspend fun setRefreshToken(token: String)

    suspend fun getRefreshToken(): String?

    fun observeRefreshToken(): Flow<String?>

    /**
     * True if a refresh-token value is physically present in storage, WITHOUT attempting to decrypt it.
     * Used only for logout-attribution telemetry: distinguishes "token gone (real logout)" from "token
     * on disk but decrypt transiently failed" (the spurious-logout fingerprint).
     */
    suspend fun hasStoredRefreshTokenBlob(): Boolean

    suspend fun setExpiresAt(expiresIn: Long)

    suspend fun getExpiresAt(): Long?

    suspend fun clear(): Boolean
}
