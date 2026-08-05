package com.todoapp.mobile.di

import com.todoapp.mobile.data.engine.PomodoroEngineImpl
import com.todoapp.mobile.data.location.PlaceSearchRepositoryImpl
import com.todoapp.mobile.data.repository.AuthRepositoryImpl
import com.todoapp.mobile.data.repository.ChatRepositoryImpl
import com.todoapp.mobile.data.repository.GroupRepositoryImpl
import com.todoapp.mobile.data.repository.InvitationRepositoryImpl
import com.todoapp.mobile.data.repository.JournalRepositoryImpl
import com.todoapp.mobile.data.repository.LanguageRepositoryImpl
import com.todoapp.mobile.data.repository.NotificationRepositoryImpl
import com.todoapp.mobile.data.repository.PaletteRepositoryImpl
import com.todoapp.mobile.data.repository.PomodoroRepositoryImpl
import com.todoapp.mobile.data.repository.SessionPreferencesImpl
import com.todoapp.mobile.data.repository.TaskRepositoryImpl
import com.todoapp.mobile.data.repository.TaskSyncRepositoryImpl
import com.todoapp.mobile.data.repository.ThemeRepositoryImpl
import com.todoapp.mobile.data.repository.UserRepositoryImpl
import com.todoapp.mobile.data.source.local.datasource.GroupActivityLocalDataSource
import com.todoapp.mobile.data.source.local.datasource.GroupActivityLocalDataSourceImpl
import com.todoapp.mobile.data.source.local.datasource.GroupLocalDataSource
import com.todoapp.mobile.data.source.local.datasource.GroupLocalDataSourceImpl
import com.todoapp.mobile.data.source.local.datasource.GroupMemberLocalDataSource
import com.todoapp.mobile.data.source.local.datasource.GroupMemberLocalDataSourceImpl
import com.todoapp.mobile.data.source.local.datasource.GroupTaskLocalDataSource
import com.todoapp.mobile.data.source.local.datasource.GroupTaskLocalDataSourceImpl
import com.todoapp.mobile.data.source.local.datasource.TaskLocalDataSource
import com.todoapp.mobile.data.source.local.datasource.TaskLocalDataSourceImpl
import com.todoapp.mobile.data.source.remote.datasource.GroupRemoteDataSource
import com.todoapp.mobile.data.source.remote.datasource.GroupRemoteDataSourceImpl
import com.todoapp.mobile.data.source.remote.datasource.InvitationRemoteDataSource
import com.todoapp.mobile.data.source.remote.datasource.InvitationRemoteDataSourceImpl
import com.todoapp.mobile.data.source.remote.datasource.NotificationRemoteDataSource
import com.todoapp.mobile.data.source.remote.datasource.NotificationRemoteDataSourceImpl
import com.todoapp.mobile.data.source.remote.datasource.TaskRemoteDataSource
import com.todoapp.mobile.data.source.remote.datasource.TaskRemoteDataSourceImpl
import com.todoapp.mobile.domain.engine.PomodoroEngine
import com.todoapp.mobile.domain.location.PlaceSearchRepository
import com.todoapp.mobile.domain.repository.AuthRepository
import com.todoapp.mobile.domain.repository.ChatRepository
import com.todoapp.mobile.domain.repository.GroupRepository
import com.todoapp.mobile.domain.repository.InvitationRepository
import com.todoapp.mobile.domain.repository.JournalRepository
import com.todoapp.mobile.domain.repository.LanguageRepository
import com.todoapp.mobile.domain.repository.NotificationRepository
import com.todoapp.mobile.domain.repository.PaletteRepository
import com.todoapp.mobile.domain.repository.PomodoroRepository
import com.todoapp.mobile.domain.repository.SessionPreferences
import com.todoapp.mobile.domain.repository.TaskRepository
import com.todoapp.mobile.domain.repository.TaskSyncRepository
import com.todoapp.mobile.domain.repository.ThemeRepository
import com.todoapp.mobile.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Hilt requires `abstract class` for `@Module` types containing `@Binds` methods —
// converting to an interface is a compile error. Detekt's UnnecessaryAbstractClass rule
// doesn't know about that constraint, hence the narrow suppression.
@Suppress("UnnecessaryAbstractClass")
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindTaskRemoteDataSource(impl: TaskRemoteDataSourceImpl): TaskRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindTaskLocalDataSource(impl: TaskLocalDataSourceImpl): TaskLocalDataSource

    @Binds
    @Singleton
    abstract fun bindGroupLocalDataSource(impl: GroupLocalDataSourceImpl): GroupLocalDataSource

    @Binds
    @Singleton
    abstract fun bindGroupTaskLocalDataSource(impl: GroupTaskLocalDataSourceImpl): GroupTaskLocalDataSource

    @Binds
    @Singleton
    abstract fun bindGroupMemberLocalDataSource(impl: GroupMemberLocalDataSourceImpl): GroupMemberLocalDataSource

    @Binds
    @Singleton
    abstract fun bindGroupActivityLocalDataSource(impl: GroupActivityLocalDataSourceImpl): GroupActivityLocalDataSource

    @Binds
    @Singleton
    abstract fun bindSessionPreferences(impl: SessionPreferencesImpl): SessionPreferences

    @Binds
    @Singleton
    abstract fun bindThemeRepository(impl: ThemeRepositoryImpl): ThemeRepository

    @Binds
    @Singleton
    abstract fun bindPaletteRepository(impl: PaletteRepositoryImpl): PaletteRepository

    @Binds
    @Singleton
    abstract fun bindPlaceSearchRepository(impl: PlaceSearchRepositoryImpl): PlaceSearchRepository

    @Binds
    @Singleton
    abstract fun bindLanguageRepository(impl: LanguageRepositoryImpl): LanguageRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindPomodoroEngine(impl: PomodoroEngineImpl): PomodoroEngine

    @Binds
    @Singleton
    abstract fun bindTaskSyncRepository(impl: TaskSyncRepositoryImpl): TaskSyncRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindTaskRepository(taskRepositoryImpl: TaskRepositoryImpl): TaskRepository

    @Binds
    @Singleton
    abstract fun bindGroupDataSource(groupRemoteDataSourceImpl: GroupRemoteDataSourceImpl): GroupRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindGroupRepository(groupRepositoryImpl: GroupRepositoryImpl): GroupRepository

    @Binds
    @Singleton
    abstract fun bindPomodoroRepository(pomodoroRepositoryImpl: PomodoroRepositoryImpl): PomodoroRepository

    @Binds
    @Singleton
    abstract fun bindPendingPhotoRepository(
        impl: com.todoapp.mobile.data.repository.PendingPhotoRepositoryImpl,
    ): com.todoapp.mobile.domain.repository.PendingPhotoRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRemoteDataSource(
        impl: NotificationRemoteDataSourceImpl,
    ): NotificationRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindInvitationRemoteDataSource(
        impl: InvitationRemoteDataSourceImpl,
    ): InvitationRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindInvitationRepository(impl: InvitationRepositoryImpl): InvitationRepository

    @Binds
    @Singleton
    abstract fun bindAlarmSoundPreferences(
        impl: com.todoapp.mobile.data.repository.AlarmSoundPreferencesImpl,
    ): com.todoapp.mobile.domain.repository.AlarmSoundPreferences

    @Binds
    @Singleton
    abstract fun bindActivityPreferences(
        impl: com.todoapp.mobile.data.repository.ActivityPreferencesImpl,
    ): com.todoapp.mobile.domain.repository.ActivityPreferences

    @Binds
    @Singleton
    abstract fun bindHealthPointsPreferences(
        impl: com.todoapp.mobile.data.repository.HealthPointsPreferencesImpl,
    ): com.todoapp.mobile.domain.repository.HealthPointsPreferences

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindJournalRepository(impl: JournalRepositoryImpl): JournalRepository

    @Binds
    @Singleton
    abstract fun bindTelemetryPreferences(
        impl: com.todoapp.mobile.data.repository.TelemetryPreferencesImpl,
    ): com.todoapp.mobile.domain.repository.TelemetryPreferences

    @Binds
    @Singleton
    abstract fun bindCrashAnalyticsPreferences(
        impl: com.todoapp.mobile.data.repository.CrashAnalyticsPreferencesImpl,
    ): com.todoapp.mobile.domain.repository.CrashAnalyticsPreferences

    @Binds
    @Singleton
    abstract fun bindBlockedUsersPreferences(
        impl: com.todoapp.mobile.data.repository.BlockedUsersPreferencesImpl,
    ): com.todoapp.mobile.domain.repository.BlockedUsersPreferences

    @Binds
    @Singleton
    abstract fun bindAmbiencePreferences(
        impl: com.todoapp.mobile.data.repository.AmbiencePreferencesImpl,
    ): com.todoapp.mobile.domain.repository.AmbiencePreferences

    @Binds
    @Singleton
    abstract fun bindAmbiencePlayer(
        impl: com.todoapp.mobile.data.ambience.AmbiencePlayerImpl,
    ): com.todoapp.mobile.domain.ambience.AmbiencePlayer
}
