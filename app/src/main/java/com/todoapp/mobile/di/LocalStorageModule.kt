package com.todoapp.mobile.di

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.todoapp.mobile.data.auth.GoogleSignInManager
import com.todoapp.mobile.data.repository.DailyPlanPreferencesImpl
import com.todoapp.mobile.data.repository.FCMTokenPreferencesImpl
import com.todoapp.mobile.data.repository.JournalBiometricPreferencesImpl
import com.todoapp.mobile.data.repository.SecretPreferencesImpl
import com.todoapp.mobile.data.source.local.AppDatabase
import com.todoapp.mobile.data.source.local.GroupActivityDao
import com.todoapp.mobile.data.source.local.GroupMemberDao
import com.todoapp.mobile.data.source.local.GroupTaskDao
import com.todoapp.mobile.data.source.local.JournalEntryDao
import com.todoapp.mobile.data.source.local.PomodoroDao
import com.todoapp.mobile.data.source.local.SubtaskDao
import com.todoapp.mobile.data.source.local.TaskDao
import com.todoapp.mobile.data.source.local.datasource.GroupDao
import com.todoapp.mobile.domain.repository.DailyPlanPreferences
import com.todoapp.mobile.domain.repository.FCMTokenPreferences
import com.todoapp.mobile.domain.repository.JournalBiometricPreferences
import com.todoapp.mobile.domain.repository.SecretPreferences
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import java.io.File
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocalStorageModule {
    private const val DB_NAME = "todo_db"
    private val Context.dataStore by preferencesDataStore(name = "user_prefs")
    private const val PREFS_NAME = "todo_prefs"

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase = Room
        .databaseBuilder(
            context,
            AppDatabase::class.java,
            DB_NAME,
        )
        .addMigrations(MIGRATION_12_13)
        .build()

    /**
     * Splits the implicit category=DAILY recurrence (V1) into a separate `recurrence` column.
     * Done as a manual migration (not auto) because the schema add comes with a data UPDATE.
     */
    private val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE tasks ADD COLUMN recurrence TEXT NOT NULL DEFAULT 'NONE'")
            db.execSQL("UPDATE tasks SET recurrence = 'DAILY', category = 'PERSONAL' WHERE category = 'DAILY'")
        }
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(
        @ApplicationContext context: Context,
    ): SharedPreferences = runCatching { createEncryptedSharedPreferences(context, PREFS_NAME, buildMasterKey(context)) }
        .getOrElse { first ->
            if (first is Error) throw first
            // Keystore master key fell out of sync with the Tink keyset (reinstall, backup/restore,
            // device transfer, keystore rotation, biometric re-enroll). Auth tokens live in DataStore,
            // not here, so wiping this only resets secret-mode timing — it does NOT log the user out.
            Timber.tag("PrefsWipe").w(first, "Encrypted prefs unreadable; wiping keyset+masterkey, rebuilding")
            wipeEncryptedPrefs(context)
            runCatching { createEncryptedSharedPreferences(context, PREFS_NAME, buildMasterKey(context)) }
                .getOrElse { second ->
                    if (second is Error) throw second
                    // Catastrophic edge (broken keystore): never crash startup. Plain prefs let the app
                    // boot; only low-sensitivity secret-mode timing lives here, never auth tokens.
                    Timber.tag("PrefsWipe").e(second, "Rebuild failed; falling back to plain SharedPreferences")
                    context.getSharedPreferences("${PREFS_NAME}_fallback", Context.MODE_PRIVATE)
                }
        }

    // Fully removes the encrypted prefs so the rebuild starts clean. deleteSharedPreferences ALSO evicts
    // the in-memory ContextImpl cache (a raw file delete does not) — without that, the retry re-reads the
    // stale cached keyset and fails again with AEADBadTagException, which is what used to crash startup.
    private fun wipeEncryptedPrefs(context: Context) {
        runCatching { context.deleteSharedPreferences(PREFS_NAME) }
        deleteSharedPreferencesFile(context, PREFS_NAME)
        deleteMasterKeyEntry()
    }

    private fun buildMasterKey(context: Context): MasterKey = MasterKey
        .Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private fun deleteMasterKeyEntry() {
        runCatching {
            val ks = java.security.KeyStore.getInstance("AndroidKeyStore")
            ks.load(null)
            if (ks.containsAlias(MasterKey.DEFAULT_MASTER_KEY_ALIAS)) {
                ks.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            }
        }
    }

    private fun createEncryptedSharedPreferences(
        context: Context,
        fileName: String,
        masterKey: MasterKey,
    ): SharedPreferences = EncryptedSharedPreferences.create(
        context,
        fileName,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private fun deleteSharedPreferencesFile(
        context: Context,
        fileName: String,
    ) {
        try {
            val sharedPrefsFile = File(context.applicationInfo.dataDir, "shared_prefs/$fileName.xml")
            if (sharedPrefsFile.exists()) {
                sharedPrefsFile.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context,
    ): DataStore<androidx.datastore.preferences.core.Preferences> = context.dataStore

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    @Singleton
    fun provideTaskDao(database: AppDatabase): TaskDao = database.taskDao()

    @Provides
    @Singleton
    fun provideSubtaskDao(database: AppDatabase): SubtaskDao = database.subtaskDao()

    @Provides
    @Singleton
    fun provideGroupDao(database: AppDatabase): GroupDao = database.groupDao()

    @Provides
    @Singleton
    fun provideGroupTaskDao(database: AppDatabase): GroupTaskDao = database.groupTaskDao()

    @Provides
    @Singleton
    fun provideGroupMemberDao(database: AppDatabase): GroupMemberDao = database.groupMemberDao()

    @Provides
    @Singleton
    fun provideGroupActivityDao(database: AppDatabase): GroupActivityDao = database.groupActivityDao()

    @Provides
    @Singleton
    fun providePomodoro(database: AppDatabase): PomodoroDao = database.pomodoroDao()

    @Provides
    @Singleton
    fun providePendingPhotoDao(database: AppDatabase): com.todoapp.mobile.data.source.local.PendingPhotoDao = database.pendingPhotoDao()

    @Provides
    @Singleton
    fun provideTaskDailyCompletionDao(
        database: AppDatabase,
    ): com.todoapp.mobile.data.source.local.TaskDailyCompletionDao = database.taskDailyCompletionDao()

    @Provides
    @Singleton
    fun provideChatMessageDao(
        database: AppDatabase,
    ): com.todoapp.mobile.data.source.local.ChatMessageDao = database.chatMessageDao()

    @Provides
    @Singleton
    fun provideJournalEntryDao(database: AppDatabase): JournalEntryDao = database.journalEntryDao()

    @Provides
    @Singleton
    fun provideGoogleSignInManager(): GoogleSignInManager = GoogleSignInManager
}

// Hilt requires `abstract class` for `@Module` types containing `@Binds` methods —
// converting to an interface is a compile error. Detekt's UnnecessaryAbstractClass rule
// doesn't know about that constraint, hence the narrow suppression.
@Suppress("UnnecessaryAbstractClass")
@Module
@InstallIn(SingletonComponent::class)
abstract class LocalStorageModuleForBindings {
    @Binds
    @Singleton
    abstract fun bindSecretModePreferences(secretPreferencesImpl: SecretPreferencesImpl): SecretPreferences

    @Binds
    @Singleton
    abstract fun bindDailyPlanPreferences(dailyPlanPreferencesImpl: DailyPlanPreferencesImpl): DailyPlanPreferences

    @Binds
    @Singleton
    abstract fun bindFcmTokenPreferences(impl: FCMTokenPreferencesImpl): FCMTokenPreferences

    @Binds
    @Singleton
    abstract fun bindJournalBiometricPreferences(
        impl: JournalBiometricPreferencesImpl,
    ): JournalBiometricPreferences
}
