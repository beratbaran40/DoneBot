package com.todoapp.mobile.di

import com.todoapp.mobile.data.analytics.FirebaseAnalyticsHelper
import com.todoapp.mobile.domain.analytics.AnalyticsHelper
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Hilt requires an abstract class for a @Module holding @Binds; detekt's UnnecessaryAbstractClass rule
// doesn't know that, hence the narrow suppression (matches LocalStorageModuleForBindings).
@Suppress("UnnecessaryAbstractClass")
@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {
    @Binds
    @Singleton
    abstract fun bindAnalyticsHelper(impl: FirebaseAnalyticsHelper): AnalyticsHelper
}
