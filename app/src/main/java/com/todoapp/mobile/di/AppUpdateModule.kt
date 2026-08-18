package com.todoapp.mobile.di

import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The Play in-app update client, behind Hilt so it can be swapped for `FakeAppUpdateManager` in tests
 * — which is the whole reason this module exists. Play answers "no update" for any build it did not
 * install, so a test that cannot substitute the manager cannot test this feature at all.
 *
 * `@Singleton` because the availability check and the flow start are two calls that used to build two
 * managers, i.e. two binder connections to the Play Store, for one conversation.
 *
 * This lives in `src/main` on purpose. A `src/debug` + `src/release` module pair would leave the third
 * build type, `releaseLocal`, with no binding at all — its source set is its own, and `initWith` copies
 * build-type config, not sources (see the three copies of AppCheckInstaller.kt for the same tax).
 */
@Module
@InstallIn(SingletonComponent::class)
object AppUpdateModule {
    @Provides
    @Singleton
    fun provideAppUpdateManager(@ApplicationContext context: Context): AppUpdateManager = AppUpdateManagerFactory.create(context)
}
