package com.todoapp.mobile.data.source.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import com.todoapp.mobile.data.model.entity.ChatMessageEntity
import com.todoapp.mobile.data.model.entity.GroupActivityEntity
import com.todoapp.mobile.data.model.entity.GroupEntity
import com.todoapp.mobile.data.model.entity.GroupMemberEntity
import com.todoapp.mobile.data.model.entity.GroupTaskEntity
import com.todoapp.mobile.data.model.entity.JournalEntryEntity
import com.todoapp.mobile.data.model.entity.PendingPhotoEntity
import com.todoapp.mobile.data.model.entity.PomodoroEntity
import com.todoapp.mobile.data.model.entity.SubtaskEntity
import com.todoapp.mobile.data.model.entity.SyncStatus
import com.todoapp.mobile.data.model.entity.TaskDailyCompletionEntity
import com.todoapp.mobile.data.model.entity.TaskEntity
import com.todoapp.mobile.data.source.local.converter.StringListConverter
import com.todoapp.mobile.data.source.local.datasource.GroupDao

@Database(
    version = 23,
    entities = [
        TaskEntity::class,
        SubtaskEntity::class,
        PomodoroEntity::class,
        GroupEntity::class,
        GroupTaskEntity::class,
        GroupMemberEntity::class,
        GroupActivityEntity::class,
        PendingPhotoEntity::class,
        TaskDailyCompletionEntity::class,
        ChatMessageEntity::class,
        JournalEntryEntity::class,
    ],
    autoMigrations = [
        AutoMigration(from = 1, to = 2, spec = AppDatabase.Migration1To2Spec::class),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4, spec = AppDatabase.Migration3To4Spec::class),
        AutoMigration(from = 4, to = 5, spec = AppDatabase.Migration4To5Spec::class),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 10, to = 11),
        AutoMigration(from = 11, to = 12),
        AutoMigration(from = 12, to = 13),
        AutoMigration(from = 13, to = 14),
        AutoMigration(from = 14, to = 15),
        AutoMigration(from = 15, to = 16),
        AutoMigration(from = 16, to = 17),
        AutoMigration(from = 17, to = 18),
        AutoMigration(from = 18, to = 19),
        AutoMigration(from = 19, to = 20),
        AutoMigration(from = 20, to = 21),
        AutoMigration(from = 21, to = 22),
        AutoMigration(from = 22, to = 23),
    ],
)
@TypeConverters(AppDatabase.SyncStatusConverter::class, StringListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    class Migration1To2Spec : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE tasks ADD COLUMN is_secret INTEGER NOT NULL DEFAULT 0")
        }
    }

    class Migration3To4Spec : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            super.onPostMigrate(db)
            // db.execSQL("ALTER TABLE tasks ADD COLUMN order_info INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE tasks SET order_index = id ")
        }
    }

    @DeleteColumn(tableName = "groups", columnName = "image_url")
    @DeleteColumn(tableName = "groups", columnName = "color")
    class Migration4To5Spec : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            super.onPostMigrate(db)
            db.execSQL("UPDATE groups SET order_index = id")
        }
    }

    abstract fun taskDao(): TaskDao

    abstract fun subtaskDao(): SubtaskDao

    abstract fun groupDao(): GroupDao

    abstract fun groupTaskDao(): GroupTaskDao

    abstract fun groupMemberDao(): GroupMemberDao

    abstract fun groupActivityDao(): GroupActivityDao

    abstract fun pomodoroDao(): PomodoroDao

    abstract fun pendingPhotoDao(): PendingPhotoDao

    abstract fun taskDailyCompletionDao(): TaskDailyCompletionDao

    abstract fun chatMessageDao(): ChatMessageDao

    abstract fun journalEntryDao(): JournalEntryDao

    class SyncStatusConverter {
        @TypeConverter
        fun fromSyncStatus(status: SyncStatus?): String? = status?.name

        @TypeConverter
        fun toSyncStatus(value: String?): SyncStatus? = value?.let { SyncStatus.valueOf(it) }
    }
}
