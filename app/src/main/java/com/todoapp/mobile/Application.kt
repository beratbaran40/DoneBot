package com.todoapp.mobile

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.work.Configuration
import com.google.android.libraries.places.api.Places
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.todoapp.mobile.data.log.CrashlyticsTree
import com.todoapp.mobile.data.network.NetworkMonitor
import com.todoapp.mobile.data.notification.NotificationService
import com.todoapp.mobile.data.notification.PomodoroNotificationChannels
import com.todoapp.mobile.di.IoDispatcher
import com.todoapp.mobile.domain.alarm.RescheduleAllAlarmsUseCase
import com.todoapp.mobile.domain.engine.PomodoroEngine
import com.todoapp.mobile.domain.repository.SecretPreferences
import com.todoapp.mobile.domain.security.SecretModeEndEvent
import com.todoapp.mobile.domain.usecase.security.OnSecretModeEventUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class Application :
    Application(),
    DefaultLifecycleObserver,
    Configuration.Provider,
    coil.ImageLoaderFactory {
    @Inject
    lateinit var secretPreferences: SecretPreferences

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var okHttpClient: okhttp3.OkHttpClient

    @Inject
    lateinit var pomodoroEngine: PomodoroEngine

    @Inject
    lateinit var rescheduleAllAlarmsUseCase: RescheduleAllAlarmsUseCase

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .build()

    override fun onCreate() {
        super<Application>.onCreate()
        initCrashReporting()
        // Cold-start hot path: onCreate runs before any UI. An unhandled throw from a single init
        // (Firebase App Check on odd Play-Services states, notification-channel creation) would crash
        // the app on launch with no screen shown. Guard each so one failure can't block startup.
        runCatching { installAppCheck() }
            .onFailure { Timber.tag("AppInit").w(it, "installAppCheck failed") }
        initializePlacesSdk()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        runCatching {
            createNotificationChannel()
            PomodoroNotificationChannels.ensurePomodoroChannel(this)
        }.onFailure { Timber.tag("AppInit").w(it, "notification channel init failed") }
        ProcessLifecycleOwner.get().lifecycleScope.launch(ioDispatcher) {
            runCatching { rescheduleAllAlarmsUseCase() }
                .onFailure { Timber.tag("RescheduleAllAlarms").w(it, "reschedule on app start failed") }
        }
    }

    /**
     * Crashlytics + Timber wiring. Collection is OFF in debug (manual test crashes would pollute the
     * prod dashboard); release plants [CrashlyticsTree] so Timber WARN/ERROR survive as breadcrumbs /
     * non-fatals — without it, release Timber logs go nowhere. Custom keys ride along on every report.
     */
    private fun initCrashReporting() {
        FirebaseCrashlytics.getInstance().apply {
            setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
            setCustomKey("build_type", if (BuildConfig.DEBUG) "debug" else "release")
            setCustomKey("version_name", BuildConfig.VERSION_NAME)
            setCustomKey("version_code", BuildConfig.VERSION_CODE)
        }
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashlyticsTree())
        }
    }

    /**
     * Places SDK requires a one-time initialization with the Maps API key. We skip if the
     * key is missing (CI/local-no-key dev) — autocomplete will simply fail at runtime, but
     * the rest of the app keeps working. Skip on already-initialized to avoid the SDK's
     * "Initialize" warning during Application restarts in instrumented tests.
     */
    private fun initializePlacesSdk() {
        if (BuildConfig.MAPS_API_KEY.isBlank()) {
            Timber.tag("PlacesSdk").w("MAPS_API_KEY missing — location autocomplete disabled.")
            return
        }
        if (!Places.isInitialized()) {
            runCatching { Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY) }
                .onFailure { Timber.tag("PlacesSdk").w(it, "Places.initialize failed") }
        }
    }

    // Coil picks this up automatically as the app-wide ImageLoader; wires the shared OkHttpClient
    // (with the AuthInterceptor) so authenticated endpoints like /users/{id}/avatar and
    // /tasks/{id}/photos/{photoId} send the Bearer token.
    override fun newImageLoader(): coil.ImageLoader = coil.ImageLoader
        .Builder(this)
        .okHttpClient { okHttpClient }
        .crossfade(true)
        .build()

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        owner.lifecycleScope.launch {
            OnSecretModeEventUseCase(secretPreferences).invoke(SecretModeEndEvent.APP_CLOSED)
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        pomodoroEngine.shutdown()
        networkMonitor.shutdown()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    NotificationService.CHANNEL_ID,
                    getString(R.string.notification_channel_tasks_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    enableVibration(true)
                    enableLights(true)
                    setShowBadge(true)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
            channel.description = getString(R.string.notification_channel_tasks_description)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
