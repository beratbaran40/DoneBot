package com.todoapp.mobile.data.repository

import com.todoapp.mobile.common.DomainException
import com.todoapp.mobile.data.model.entity.PomodoroSessionEntity
import com.todoapp.mobile.data.model.entity.SyncStatus
import com.todoapp.mobile.data.model.network.data.PomodoroSessionListData
import com.todoapp.mobile.data.model.network.data.PomodoroUploadData
import com.todoapp.mobile.data.model.network.request.PomodoroSessionDto
import com.todoapp.mobile.data.source.local.PomodoroSessionDao
import com.todoapp.mobile.data.source.remote.datasource.PomodoroRemoteDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * The upload path's failure policy, which is where rows get lost if it is wrong.
 *
 * The case that matters most is `NotFound`. Everywhere else in this codebase a 404 means "this row is
 * permanently gone, stop pushing it" — here it means "this backend does not have the endpoint yet",
 * which is what a staged rollout looks like from a new client. Treating those the same would delete a
 * user's focus history to save one wasted request.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PomodoroSessionSyncTest {

    private val dao = mockk<PomodoroSessionDao>(relaxed = true)
    private val remote = mockk<PomodoroRemoteDataSource>()
    private val dataStoreHelper = mockk<DataStoreHelper>(relaxed = true)
    private val clock = Clock.fixed(Instant.parse("2026-08-18T10:00:00Z"), ZoneOffset.UTC)

    private lateinit var repository: PomodoroSessionRepositoryImpl

    @Before
    fun setUp() {
        every { dataStoreHelper.observeUser() } returns flowOf(null)
        repository = PomodoroSessionRepositoryImpl(dao, remote, dataStoreHelper, clock)
    }

    @Test
    fun `a successful upload marks exactly the rows it sent as synced`() = runTest {
        coEvery { dao.getPending(any()) } returns rows(3)
        coEvery { remote.upload(any()) } returns Result.success(PomodoroUploadData(accepted = 3))

        val result = repository.pushPending()

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { dao.markSynced(listOf("s0", "s1", "s2")) }
    }

    @Test
    fun `more than fifty pending rows go out in chunks of fifty`() = runTest {
        coEvery { dao.getPending(any()) } returns rows(120)
        coEvery { remote.upload(any()) } returns Result.success(PomodoroUploadData(accepted = 50))

        repository.pushPending()

        // 50 + 50 + 20. The server refuses a larger batch, so an unchunked push would 400 the lot.
        coVerify(exactly = 3) { remote.upload(any()) }
        coVerify(exactly = 1) { remote.upload(match { it.size == 20 }) }
    }

    @Test
    fun `a retryable failure leaves the rows pending and reports failure`() = runTest {
        coEvery { dao.getPending(any()) } returns rows(2)
        coEvery { remote.upload(any()) } returns Result.failure(DomainException.NoInternet())

        val result = repository.pushPending()

        assertTrue("the worker must be told to retry", result.isFailure)
        coVerify(exactly = 0) { dao.markSynced(any()) }
    }

    @Test
    fun `a missing endpoint leaves the rows pending but does not fail the sync`() = runTest {
        coEvery { dao.getPending(any()) } returns rows(2)
        coEvery { remote.upload(any()) } returns Result.failure(DomainException.NotFound())

        val result = repository.pushPending()

        // New app, older backend. Marking these synced would discard them permanently; failing the
        // whole sync would drag the task push down with it. Neither, and they go again next time.
        coVerify(exactly = 0) { dao.markSynced(any()) }
        assertTrue("a task sync in the same worker must not be failed by this", result.isSuccess)
    }

    @Test
    fun `a genuinely rejected batch is dropped rather than retried forever`() = runTest {
        coEvery { dao.getPending(any()) } returns rows(2)
        coEvery { remote.upload(any()) } returns Result.failure(DomainException.Unknown(IllegalStateException("400 bad request")))

        val result = repository.pushPending()

        // A batch the server will never accept would otherwise block every later one. Chunking at fifty
        // is what bounds the loss.
        coVerify(exactly = 1) { dao.markSynced(listOf("s0", "s1")) }
        assertTrue(result.isSuccess)
    }

    @Test
    fun `nothing pending means no network call at all`() = runTest {
        coEvery { dao.getPending(any()) } returns emptyList()

        assertTrue(repository.pushPending().isSuccess)

        coVerify(exactly = 0) { remote.upload(any()) }
    }

    // ---------------------------------------------------------------- backfill

    @Test
    fun `backfill inserts the downloaded rows and settles them as synced`() = runTest {
        coEvery { remote.list(any(), any()) } returns Result.success(
            PomodoroSessionListData(items = listOf(dto("a"), dto("b")), count = 2),
        )

        val result = repository.backfill(20_000L, 20_365L)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { dao.insertAll(match { it.size == 2 }) }
        // Marking them synced settles the local copy of any row the device was still holding pending.
        coVerify(exactly = 1) { dao.markSynced(listOf("a", "b")) }
    }

    @Test
    fun `a second backfill inside the cooldown makes no request`() = runTest {
        coEvery { remote.list(any(), any()) } returns Result.success(PomodoroSessionListData())

        repository.backfill(20_000L, 20_365L)
        repository.backfill(20_000L, 20_365L)

        // The sign-in collector can emit more than once per launch; re-downloading a year each time
        // would be rude to a database that scales to zero.
        coVerify(exactly = 1) { remote.list(any(), any()) }
    }

    // ---------------------------------------------------------------- helpers

    private fun rows(count: Int): List<PomodoroSessionEntity> = (0 until count).map { index ->
        PomodoroSessionEntity(
            clientSessionId = "s$index",
            clientRunId = "run",
            sessionIndex = index,
            mode = "FOCUS",
            plannedSeconds = 1500,
            elapsedSeconds = 1500,
            completed = true,
            startedAt = 0L,
            endedAt = 1L,
            localDate = 20_000L,
            tzOffsetMinutes = 0,
            syncStatus = SyncStatus.PENDING_CREATE,
        )
    }

    private fun dto(id: String) = PomodoroSessionDto(
        clientSessionId = id,
        clientRunId = "run",
        sessionIndex = 0,
        mode = "FOCUS",
        plannedSeconds = 1500,
        elapsedSeconds = 900,
        completed = false,
        startedAt = 0L,
        endedAt = 1L,
        localDate = 20_000L,
        tzOffsetMinutes = 180,
    )
}
