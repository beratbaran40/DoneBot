package com.todoapp.mobile.data.source.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Splits the implicit category=DAILY recurrence (V1) into a separate `recurrence` column.
 *
 * Kept as a manual migration (not an auto-migration) because the schema add is paired with a data
 * UPDATE that rewrites the old category=DAILY rows. Extracted to a top-level `internal val` — instead
 * of living privately inside LocalStorageModule — so the androidTest migration test can validate the
 * exact shipped object (including the backfill), not a copy of it.
 */
internal val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN recurrence TEXT NOT NULL DEFAULT 'NONE'")
        db.execSQL("UPDATE tasks SET recurrence = 'DAILY', category = 'PERSONAL' WHERE category = 'DAILY'")
    }
}

/**
 * One-time repair + guard for duplicate group rows. The pre-v26 sync raced concurrent getGroups
 * callers into inserting the same backend group twice (same remote_id, different local ids), and
 * once present the duplicates were sticky — later syncs only ever updated the first match. Manual
 * (not auto) because the dedup DELETEs must run BEFORE the unique index is created.
 *
 * Keeps MIN(id) per remote_id (the oldest row, i.e. the user's order_index). FK enforcement is off
 * during onUpgrade, so CASCADE won't fire — children of the doomed rows are deleted explicitly and
 * re-sync on the next detail/tasks fetch.
 */
internal val MIGRATION_25_26 = object : Migration(25, 26) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val doomed = "SELECT id FROM `groups` WHERE remote_id IS NOT NULL AND id NOT IN " +
            "(SELECT MIN(id) FROM `groups` WHERE remote_id IS NOT NULL GROUP BY remote_id)"
        db.execSQL("DELETE FROM group_members WHERE local_group_id IN ($doomed)")
        db.execSQL("DELETE FROM group_tasks WHERE local_group_id IN ($doomed)")
        db.execSQL("DELETE FROM group_activities WHERE local_group_id IN ($doomed)")
        db.execSQL("DELETE FROM `groups` WHERE id IN ($doomed)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_groups_remote_id` ON `groups` (`remote_id`)")
    }
}
