package com.todoapp.mobile.data.repository

import com.todoapp.mobile.domain.repository.BlockedUsersPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockedUsersPreferencesImpl
@Inject
constructor(
    private val dataStoreHelper: DataStoreHelper,
) : BlockedUsersPreferences {
    override fun observeBlocked(): Flow<Map<Long, String>> = dataStoreHelper.observeBlockedUsers()

    override suspend fun getBlockedIds(): Set<Long> = dataStoreHelper.observeBlockedUsers().first().keys

    override suspend fun isBlocked(userId: Long): Boolean = dataStoreHelper.observeBlockedUsers().first().containsKey(userId)

    override suspend fun block(userId: Long, displayName: String) = dataStoreHelper.blockUser(userId, displayName)

    override suspend fun unblock(userId: Long) = dataStoreHelper.unblockUser(userId)
}
