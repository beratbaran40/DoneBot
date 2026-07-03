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
