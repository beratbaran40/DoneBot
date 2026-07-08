package com.todoapp.mobile.data.source.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Room migration regression tests.
 *
 * The database is built WITHOUT `fallbackToDestructiveMigration()`, so a broken migration path is a
 * launch-time crash-loop on the FIRST update (versionCode 2) — invisible on a clean install where no
 * migration runs. These lock the recently-added hops and the one data-carrying manual migration.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun `migrate 22 to 25 applies auto-migrations and validates schema`() {
        helper.createDatabase(TEST_DB, 22).apply {
            execSQL(
                "INSERT INTO tasks (id, title, date, time_start, time_end, is_completed, is_secret) " +
                    "VALUES (1, 'Ship it', 0, 0, 0, 0, 0)",
            )
            close()
        }
        // Auto-migrations 22→23→24→25 are discovered from @Database; validate against 25.json.
        helper.runMigrationsAndValidate(TEST_DB, 25, true)
    }

    @Test
    fun `migrate 25 to 26 dedups duplicate remote groups and keeps the oldest row`() {
        helper.createDatabase(TEST_DB, 25).apply {
            // Two rows for the same backend group (the pre-v26 sync race) + one unsynced row.
            execSQL(
                "INSERT INTO `groups` (id, name, description, remote_id, created_at, order_index) " +
                    "VALUES (1, 'Fam', '', 42, 0, 0)",
            )
            execSQL(
                "INSERT INTO `groups` (id, name, description, remote_id, created_at, order_index) " +
                    "VALUES (2, 'Fam', '', 42, 0, 1)",
            )
            execSQL(
                "INSERT INTO `groups` (id, name, description, remote_id, created_at, order_index) " +
                    "VALUES (3, 'Offline', '', NULL, 0, 2)",
            )
            // One member on the doomed duplicate (id=2), one on the kept row (id=1).
            execSQL(
                "INSERT INTO group_members (id, user_id, local_group_id, display_name, email, role, joined_at) " +
                    "VALUES (1, 9, 2, 'Ayşe', 'a@x.com', 'MEMBER', 0)",
            )
            execSQL(
                "INSERT INTO group_members (id, user_id, local_group_id, display_name, email, role, joined_at) " +
                    "VALUES (2, 9, 1, 'Ayşe', 'a@x.com', 'MEMBER', 0)",
            )
            close()
        }
        // Validates the new unique index against 26.json too.
        val db = helper.runMigrationsAndValidate(TEST_DB, 26, true, MIGRATION_25_26)
        db.query("SELECT id FROM `groups` WHERE remote_id = 42").use { cursor ->
            assertEquals(1, cursor.count)
            assertTrue(cursor.moveToFirst())
            assertEquals(1L, cursor.getLong(0))
        }
        db.query("SELECT COUNT(*) FROM `groups`").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(2, cursor.getInt(0)) // kept remote row + untouched NULL-remote_id row
        }
        db.query("SELECT id FROM group_members").use { cursor ->
            assertEquals(1, cursor.count) // the doomed row's member is purged (FKs are off in onUpgrade)
            assertTrue(cursor.moveToFirst())
            assertEquals(2L, cursor.getLong(0))
        }
    }

    @Test
    fun `migrate 25 to 27 chains the manual dedup with the additive activity column`() {
        helper.createDatabase(TEST_DB, 25).apply {
            execSQL(
                "INSERT INTO `groups` (id, name, description, remote_id, created_at, order_index) " +
                    "VALUES (1, 'Fam', '', 42, 0, 0)",
            )
            execSQL(
                "INSERT INTO group_activities (id, remote_id, local_group_id, type, actor_name, description, timestamp) " +
                    "VALUES (1, 7, 1, 'MEMBER_ADDED', 'Ayşe', 'Ayşe joined the group', 0)",
            )
            close()
        }
        // 25→26 is manual (dedup + unique index); 26→27 (target_name) is discovered from @Database.
        val db = helper.runMigrationsAndValidate(TEST_DB, 27, true, MIGRATION_25_26)
        db.query("SELECT target_name FROM group_activities WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue(cursor.isNull(0)) // legacy rows carry no structured target → English fallback
        }
    }

    @Test
    fun `migrate 12 to 13 backfills recurrence from the daily category`() {
        helper.createDatabase(TEST_DB, 12).apply {
            execSQL(
                "INSERT INTO tasks (id, title, date, time_start, time_end, is_completed, is_secret, category) " +
                    "VALUES (1, 'Daily standup', 0, 0, 0, 0, 0, 'DAILY')",
            )
            close()
        }
        // 12→13 is double-declared (auto + manual). The manually-added migration takes precedence and
        // is the only one that backfills recurrence + rewrites the legacy DAILY category.
        val db = helper.runMigrationsAndValidate(TEST_DB, 13, true, MIGRATION_12_13)
        db.query("SELECT recurrence, category FROM tasks WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("DAILY", cursor.getString(0))
            assertEquals("PERSONAL", cursor.getString(1))
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
