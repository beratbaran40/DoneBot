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
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import com.todoapp.mobile.data.ambience.AmbienceCoordinator
import com.todoapp.mobile.data.log.CrashlyticsTree
import com.todoapp.mobile.data.network.BackendWarmUp
import com.todoapp.mobile.data.network.NetworkMonitor
import com.todoapp.mobile.data.notification.NotificationService
import com.todoapp.mobile.data.notification.PomodoroNotificationChannels
import com.todoapp.mobile.data.perf.StartupColdStartTrace
import com.todoapp.mobile.di.IoDispatcher
import com.todoapp.mobile.domain.alarm.RescheduleAllAlarmsUseCase
import com.todoapp.mobile.domain.ambience.AmbiencePlayer
import com.todoapp.mobile.domain.engine.PomodoroEngine
import com.todoapp.mobile.domain.repository.CrashAnalyticsPreferences
import com.todoapp.mobile.domain.repository.SecretPreferences
import com.todoapp.mobile.domain.repository.TelemetryPreferences
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
    lateinit var backendWarmUp: BackendWarmUp

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    lateinit var telemetryPreferences: TelemetryPreferences

    @Inject
    lateinit var crashAnalyticsPreferences: CrashAnalyticsPreferences

    @Inject
    lateinit var ambienceCoordinator: AmbienceCoordinator

    @Inject
    lateinit var ambiencePlayer: AmbiencePlayer

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .build()

    override fun onCreate() {
        super<Application>.onCreate()
        if (BuildConfig.DEBUG) configureStrictMode()
        initCrashReporting()
        runCatching { initPerformanceMonitoring() }
            .onFailure { Timber.tag("AppInit").w(it, "initPerformanceMonitoring failed") }
        runCatching { initCrashAnalyticsGating() }
            .onFailure { Timber.tag("AppInit").w(it, "initCrashAnalyticsGating failed") }
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
        runCatching { ambienceCoordinator.start() }
            .onFailure { Timber.tag("AppInit").w(it, "ambienceCoordinator init failed") }
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
            // Debug force-off (test crashes must not pollute prod). In release we do NOT set collection
            // here — the natively-persisted flag stands and initCrashAnalyticsGating() reconciles it with
            // the user's §7.3 opt-out off the startup hot path, so a prior opt-out isn't clobbered each launch.
            if (BuildConfig.DEBUG) setCrashlyticsCollectionEnabled(false)
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
     * Firebase Performance (§3.10). Consent-gated: the manifest default is OFF and collection follows
     * the persisted opt-in ([TelemetryPreferences], default false) — never on in debug. Collecting the
     * preference keeps the SDK's collection flag in sync with the Settings toggle at runtime (the initial
     * emission applies the stored choice at startup). The cold-start trace starts here and is stopped
     * from Home; it records nothing while collection is off or if Home is never reached.
     */
    private fun initPerformanceMonitoring() {
        StartupColdStartTrace.start()
        ProcessLifecycleOwner.get().lifecycleScope.launch(ioDispatcher) {
            telemetryPreferences.observe().collect { optedIn ->
                runCatching {
                    FirebasePerformance.getInstance().isPerformanceCollectionEnabled = !BuildConfig.DEBUG && optedIn
                }.onFailure { Timber.tag("AppInit").w(it, "perf collection toggle failed") }
            }
        }
    }

    /**
     * §7.3 telemetry opt-out. Crashlytics + Analytics collection follow the persisted opt-out
     * ([CrashAnalyticsPreferences], default true) AND-ed with !debug. Both Firebase collection flags
     * persist natively across launches, so this reactive collector reconciles them off the startup hot
     * path (no blocking DataStore read in onCreate) and re-applies the user's choice whenever it changes.
     */
    private fun initCrashAnalyticsGating() {
        ProcessLifecycleOwner.get().lifecycleScope.launch(ioDispatcher) {
            crashAnalyticsPreferences.observe().collect { allowed ->
                val effective = allowed && !BuildConfig.DEBUG
                runCatching {
                    FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(effective)
                    FirebaseAnalytics.getInstance(this@Application).setAnalyticsCollectionEnabled(effective)
                }.onFailure { Timber.tag("AppInit").w(it, "crash/analytics collection toggle failed") }
            }
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
        .memoryCache {
            coil.memory.MemoryCache.Builder(this)
                .maxSizePercent(0.20)
                .build()
        }
        .diskCache {
            coil.disk.DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache"))
                .maxSizeBytes(50L * 1024 * 1024)
                .build()
        }
        .build()

    // Every cold launch AND background→foreground return warms the backend before the user reaches
    // an online surface (chat, sync) — exactly the moments a spun-down/deploying instance would
    // otherwise greet the first real request with a timeout. Self-throttled inside BackendWarmUp.
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        owner.lifecycleScope.launch { backendWarmUp.pingIfStale() }
        // Half of the ambience "keep playing in the background" rule — the coordinator ANDs this
        // with the user's toggle, so with the toggle off the bed follows the app on screen.
        ambienceCoordinator.onAppForegrounded()
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        owner.lifecycleScope.launch {
            OnSecretModeEventUseCase(secretPreferences).invoke(SecretModeEndEvent.APP_CLOSED)
        }
        ambienceCoordinator.onAppBackgrounded()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        pomodoroEngine.shutdown()
        ambienceCoordinator.shutdown()
        ambiencePlayer.shutdown()
        networkMonitor.shutdown()
    }

    // §3.7 Memory-pressure breadcrumb: in release CrashlyticsTree forwards WARN+ to Crashlytics, so
    // an eventual OOM / low-memory kill has a visible trim trail. Coil's ImageLoader already self-trims
    // its caches on these callbacks; on the most aggressive levels we also drop its memory cache.
    // Trim-level constants are API-34-deprecated but the callback still fires with them on-device.
    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            Timber.tag("Memory").w("onTrimMemory level=%d", level)
        }
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            coil.Coil.imageLoader(this).memoryCache?.clear()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            val tasks =
                NotificationChannel(
                    NotificationService.CHANNEL_ID,
                    getString(R.string.notification_channel_tasks_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = getString(R.string.notification_channel_tasks_description)
                    enableVibration(true)
                    enableLights(true)
                    setShowBadge(true)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }

            // Non-urgent FYI channel (§7.8): task completed by a member, invitation accepted/declined,
            // ownership transfer, generic notices. Silent so it never interrupts; still badges + shows.
            val info =
                NotificationChannel(
                    NotificationService.CHANNEL_ID_INFO,
                    getString(R.string.notification_channel_info_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = getString(R.string.notification_channel_info_description)
                    setSound(null, null)
                    enableVibration(false)
                    enableLights(false)
                    setShowBadge(true)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }

            notificationManager.createNotificationChannel(tasks)
            notificationManager.createNotificationChannel(info)
        }
    }
}
