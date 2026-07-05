package com.todoapp.mobile.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Device-local list of users this account has blocked (§6.18 UGC moderation). Blocking is
 * intentionally client-side: a blocked member's presence (name/avatar rows) is hidden from group
 * surfaces on this device. Reporting content is the server-side, moderatable counterpart
 * ([GroupRepository.reportContent]).
 */
interface BlockedUsersPreferences {
    /** Blocked users on this device as id -> display name. */
    fun observeBlocked(): Flow<Map<Long, String>>

    suspend fun getBlockedIds(): Set<Long>

    suspend fun isBlocked(userId: Long): Boolean

    suspend fun block(userId: Long, displayName: String)

    suspend fun unblock(userId: Long)
}
