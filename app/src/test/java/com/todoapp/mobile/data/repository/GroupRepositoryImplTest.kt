package com.todoapp.mobile.data.repository

import com.todoapp.mobile.data.model.entity.GroupEntity
import com.todoapp.mobile.data.model.network.data.GroupData
import com.todoapp.mobile.data.model.network.data.GroupSummaryData
import com.todoapp.mobile.data.model.network.data.GroupSummaryDataList
import com.todoapp.mobile.data.model.network.request.CreateGroupRequest
import com.todoapp.mobile.data.source.local.datasource.GroupLocalDataSource
import com.todoapp.mobile.data.source.remote.datasource.GroupRemoteDataSource
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks the race-free group reconcile (2026-07 duplicate-card fix): concurrent getGroups callers
 * (screen resume + pull-to-refresh + FCM force refresh) previously shared a stale snapshot and each
 * inserted a brand-new group (e.g. a just-accepted invite) once — two rows, one remote_id, two
 * cards. The repo mutex serializes check-then-insert; the fake mirrors the unique remote_id index
 * (insertIgnoring) as the DB-level floor and counts conflicts so a mutex regression stays visible.
 */
class GroupRepositoryImplTest {
    private val remoteDataSource = mockk<GroupRemoteDataSource>(relaxed = true)
    private val localDataSource = FakeGroupLocalDataSource()

    private fun repository() = GroupRepositoryImpl(
        groupRemoteDataSource = remoteDataSource,
        groupLocalDataSource = localDataSource,
        groupTaskLocalDataSource = mockk(relaxed = true),
        groupMemberLocalDataSource = mockk(relaxed = true),
        groupActivityLocalDataSource = mockk(relaxed = true),
        taskRemoteDataSource = mockk(relaxed = true),
        taskLocalDataSource = mockk(relaxed = true),
        alarmScheduler = mockk(relaxed = true),
        groupSubtaskDao = mockk(relaxed = true),
        groupTaskDailyCompletionDao = mockk(relaxed = true),
        todoApi = mockk(relaxed = true),
        ioDispatcher = Dispatchers.Unconfined,
    )

    @Test
    fun `concurrent getGroups calls insert a brand-new group exactly once`() = runTest {
        coEvery { remoteDataSource.getGroups() } returns Result.success(summaryList(summary(id = 42L)))
        val repository = repository()

        launch { repository.getGroups(force = true) }
        launch { repository.getGroups(force = true) }
        advanceUntilIdle()

        assertEquals(1, localDataSource.rows.count { it.remoteId == 42L })
        // The mutex must serialize the two syncs so the loser lands in the update branch —
        // the unique-index IGNORE (conflict) is the floor, not the expected path.
        assertEquals(0, localDataSource.conflictInsertCount)
    }

    @Test
    fun `a payload repeating the same group id lands as a single row`() = runTest {
        coEvery { remoteDataSource.getGroups() } returns
            Result.success(summaryList(summary(id = 42L), summary(id = 42L)))

        repository().getGroups(force = true)

        assertEquals(1, localDataSource.rows.count { it.remoteId == 42L })
    }

    @Test
    fun `createGroup after a sync already inserted the same remote id does not duplicate`() = runTest {
        coEvery { remoteDataSource.getGroups() } returns Result.success(summaryList(summary(id = 42L)))
        coEvery { remoteDataSource.createGroup(any()) } returns Result.success(groupData(id = 42L))
        val repository = repository()

        repository.getGroups(force = true)
        repository.createGroup(CreateGroupRequest(name = "Fam", description = ""))

        assertEquals(1, localDataSource.rows.count { it.remoteId == 42L })
        // The sync's summary row (role/counts) must survive; create-if-absent never overwrites it.
        assertEquals("ADMIN", localDataSource.rows.single { it.remoteId == 42L }.role)
    }

    @Test
    fun `a second sync updates the existing row in place`() = runTest {
        coEvery { remoteDataSource.getGroups() } returns Result.success(summaryList(summary(id = 42L)))
        val repository = repository()
        repository.getGroups(force = true)

        coEvery { remoteDataSource.getGroups() } returns
            Result.success(summaryList(summary(id = 42L, name = "Renamed", memberCount = 5)))
        repository.getGroups(force = true)

        val row = localDataSource.rows.single { it.remoteId == 42L }
        assertEquals("Renamed", row.name)
        assertEquals(5, row.memberCount)
        assertEquals(1, localDataSource.rows.size)
    }

    private fun summary(
        id: Long,
        name: String = "Fam",
        memberCount: Int = 2,
    ) = GroupSummaryData(
        id = id,
        name = name,
        description = "",
        role = "ADMIN",
        memberCount = memberCount,
        pendingTaskCount = 0,
        createdAt = 0L,
    )

    private fun summaryList(vararg groups: GroupSummaryData) = GroupSummaryDataList(
        groups = groups.toList(),
        count = groups.size,
    )

    private fun groupData(id: Long) = GroupData(
        id = id,
        name = "Fam",
        description = "",
        createdAt = 0L,
        updatedAt = 0L,
        members = emptyList(),
    )
}

/**
 * In-memory stand-in mirroring the v26 DB contract: insertIgnoring honors the unique remote_id
 * index (returns -1 on conflict), and the read points yield() so runTest interleaves concurrent
 * callers exactly where the real DAO suspends — making the pre-fix race reproducible on the JVM.
 */
private class FakeGroupLocalDataSource : GroupLocalDataSource {
    val rows = mutableListOf<GroupEntity>()
    var conflictInsertCount = 0
        private set
    private val flow = MutableStateFlow<List<GroupEntity>>(emptyList())
    private var nextId = 1L

    override fun observeAll(): Flow<List<GroupEntity>> = flow

    override suspend fun insertIgnoring(group: GroupEntity): Long {
        yield()
        if (group.remoteId != null && rows.any { it.remoteId == group.remoteId }) {
            conflictInsertCount++
            return -1L
        }
        val assigned = group.copy(id = nextId++)
        rows += assigned
        emit()
        return assigned.id
    }

    override suspend fun getByRemoteId(remoteId: Long): GroupEntity? {
        yield()
        return rows.firstOrNull { it.remoteId == remoteId }
    }

    override suspend fun delete(group: GroupEntity) {
        rows.removeAll { it.id == group.id }
        emit()
    }

    override suspend fun deleteAll(group: GroupEntity) {
        rows.clear()
        emit()
    }

    override suspend fun update(group: GroupEntity) {
        yield()
        val index = rows.indexOfFirst { it.id == group.id }
        if (index >= 0) rows[index] = group
        emit()
    }

    override suspend fun getGroupById(id: Long): GroupEntity? = rows.firstOrNull { it.id == id }

    override suspend fun getGroupByName(name: String): GroupEntity = rows.first { it.name == name }

    override suspend fun updateOrderIndex(id: Long, orderIndex: Int) {
        val index = rows.indexOfFirst { it.id == id }
        if (index >= 0) rows[index] = rows[index].copy(orderIndex = orderIndex)
        emit()
    }

    override suspend fun updateOrderIndices(updates: List<Pair<Long, Int>>) {
        updates.forEach { (id, orderIndex) -> updateOrderIndex(id, orderIndex) }
    }

    override fun getAllGroupsOrdered(): Flow<List<GroupEntity>> = flow

    private fun emit() {
        flow.value = rows.sortedBy { it.orderIndex }
    }
}
