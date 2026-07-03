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
