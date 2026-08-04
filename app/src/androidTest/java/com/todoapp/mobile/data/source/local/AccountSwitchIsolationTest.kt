package com.todoapp.mobile.data.source.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.todoapp.mobile.data.model.entity.JournalEntryEntity
import com.todoapp.mobile.data.model.entity.SubtaskDailyCompletionEntity
import com.todoapp.mobile.data.model.entity.SubtaskEntity
import com.todoapp.mobile.data.model.entity.TaskDailyCompletionEntity
import com.todoapp.mobile.data.model.entity.TaskEntity
import com.todoapp.mobile.data.model.entity.TaskReminderEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * §7.12 account-switch / multi-account local-data isolation. On one device, logging out of user A and into
 * user B must leave NONE of A's local data reachable by B. `MainViewModel.clearLocalSession()` (proven to
 * fire every clear by MainViewModelLogoutTest) relies on two silent invariants this test pins against real
 * SQLite — a pure MockK test cannot prove either:
 *
 *  1. tasks -> subtasks / task_daily_completions CASCADE. Neither child has a caller-driven wipe; both are
 *     emptied ONLY by ON DELETE CASCADE when `DELETE FROM tasks` runs. If FK enforcement or the cascade ever
 *     regressed, user A's staged steps + per-day completion checkmarks would survive into user B.
 *  2. journal_entries is deliberately NOT wiped on logout (local-only diary, no backend copy). Its entire
 *     cross-account isolation rests on owner_user_id scoping — user B's queries must never return A's rows.
 *
 * Plus a forcing function: every table must stay categorized (wiped vs intentionally-retained), so a newly
 * added synced table cannot slip in without an explicit account-switch isolation decision.
 */
@RunWith(AndroidJUnit4::class)
class AccountSwitchIsolationTest {
    private lateinit var db: AppDatabase
    private lateinit var taskDao: TaskDao
    private lateinit var subtaskDao: SubtaskDao
    private lateinit var dailyCompletionDao: TaskDailyCompletionDao
    private lateinit var taskReminderDao: TaskReminderDao
    private lateinit var subtaskDailyCompletionDao: SubtaskDailyCompletionDao
    private lateinit var journalDao: JournalEntryDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Real SQLite, in-memory. Room enables `PRAGMA foreign_keys = ON` by default, so cascades fire.
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        taskDao = db.taskDao()
        subtaskDao = db.subtaskDao()
        dailyCompletionDao = db.taskDailyCompletionDao()
        taskReminderDao = db.taskReminderDao()
        subtaskDailyCompletionDao = db.subtaskDailyCompletionDao()
        journalDao = db.journalEntryDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun deletingAllTasks_cascadesToSubtasksAndDailyCompletions() = runBlocking {
        val taskId = taskDao.insert(sampleTask(id = 1L))
        val subtaskId = subtaskDao.insert(SubtaskEntity(title = "step", parentTaskId = taskId))
        dailyCompletionDao.upsert(
            TaskDailyCompletionEntity(taskId = taskId, date = TODAY_EPOCH_DAY, completedAt = 0L),
        )
        taskReminderDao.insertAll(
            listOf(TaskReminderEntity(taskId = taskId, minuteOfDay = 480, slot = 0)),
        )
        subtaskDailyCompletionDao.upsert(
            SubtaskDailyCompletionEntity(
                subtaskId = subtaskId,
                taskId = taskId,
                date = TODAY_EPOCH_DAY,
                completedAt = 0L,
            ),
        )
        // Sanity: the children exist before the wipe.
        assertEquals(1, rowCount("subtasks"))
        assertEquals(1, rowCount("task_daily_completions"))
        assertEquals(1, rowCount("task_reminders"))
        assertEquals(1, rowCount("subtask_daily_completions"))

        // The exact call clearLocalSession() drives on logout.
        taskDao.deleteAllTasks()

        assertEquals("subtasks must cascade-delete with their parent task", 0, rowCount("subtasks"))
        assertEquals(
            "task_daily_completions must cascade-delete with their task (no explicit wipe exists)",
            0,
            rowCount("task_daily_completions"),
        )
        assertEquals(
            "task_reminders must cascade-delete with their task, or a user's reminder times survive a logout",
            0,
            rowCount("task_reminders"),
        )
        // Two hops: subtask_daily_completions -> subtasks -> tasks. Worth asserting explicitly,
        // since a transitive cascade is exactly the kind of thing that looks fine and isn't.
        assertEquals(
            "subtask_daily_completions must cascade through subtasks to their task",
            0,
            rowCount("subtask_daily_completions"),
        )
    }

    @Test
    fun journalEntries_areScopedByOwnerSoAccountBNeverSeesAccountA() = runBlocking {
        journalDao.upsert(sampleJournal(title = "A-secret", ownerUserId = USER_A))
        journalDao.upsert(sampleJournal(title = "B-note", ownerUserId = USER_B))

        val visibleToB = journalDao.observeAllForOwner(USER_B).first()

        assertEquals("user B must see exactly their own entries", 1, visibleToB.size)
        assertEquals("B-note", visibleToB.single().title)
        assertTrue(
            "user A's journal must be invisible to user B",
            visibleToB.none { it.ownerUserId == USER_A },
        )
    }

    @Test
    fun everyRoomTable_isCategorizedAsWipedOrIntentionallyRetained() {
        val appTables = tableNames() - SQLITE_INTERNAL_TABLES

        assertEquals(
            "A Room table is neither wiped on logout nor intentionally retained. Add it to WIPED_ON_LOGOUT " +
                "(and clear it in MainViewModel.clearLocalSession) or to INTENTIONALLY_RETAINED (device-scoped " +
                "config, or user-scoped like journal) — otherwise it leaks across an account switch. (§7.12)",
            WIPED_ON_LOGOUT + INTENTIONALLY_RETAINED,
            appTables,
        )
    }

    private fun rowCount(table: String): Int {
        db.query(SimpleSQLiteQuery("SELECT COUNT(*) FROM `$table`")).use { cursor ->
            cursor.moveToFirst()
            return cursor.getInt(0)
        }
    }

    private fun tableNames(): Set<String> {
        db.query(SimpleSQLiteQuery("SELECT name FROM sqlite_master WHERE type='table'")).use { cursor ->
            return buildSet {
                while (cursor.moveToNext()) add(cursor.getString(0))
            }
        }
    }

    private fun sampleTask(id: Long) = TaskEntity(
        title = "task",
        description = null,
        date = TODAY_EPOCH_DAY,
        timeStart = 0L,
        timeEnd = 0L,
        isCompleted = false,
        id = id,
    )

    private fun sampleJournal(
        title: String,
        ownerUserId: Long,
    ) = JournalEntryEntity(
        title = title,
        content = "content",
        photoPaths = "",
        createdAt = 0L,
        updatedAt = 0L,
        ownerUserId = ownerUserId,
    )

    private companion object {
        const val USER_A = 100L
        const val USER_B = 200L
        const val TODAY_EPOCH_DAY = 20_000L

        // Wiped by clearLocalSession() on logout — directly or via FK cascade.
        val WIPED_ON_LOGOUT = setOf(
            "tasks", "subtasks", "task_daily_completions",
            // Cascade-deleted with their parent task, proven by the cascade test above.
            "task_reminders", "subtask_daily_completions",
            "groups", "group_tasks", "group_members", "group_activities",
            "pending_photos", "chat_messages",
        )

        // Intentionally survive logout: `pomodoro` is device-scoped config; `journal_entries` is the
        // local-only diary isolated by owner_user_id (proven above), purged only on account deletion.
        val INTENTIONALLY_RETAINED = setOf("pomodoro", "journal_entries")

        // SQLite/Room bookkeeping tables, not app data.
        val SQLITE_INTERNAL_TABLES = setOf("android_metadata", "room_master_table", "sqlite_sequence")
    }
}
