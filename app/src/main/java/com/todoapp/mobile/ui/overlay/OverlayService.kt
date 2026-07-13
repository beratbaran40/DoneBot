package com.todoapp.mobile.ui.overlay

import android.app.ActivityOptions
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.todoapp.mobile.MainActivity
import com.todoapp.mobile.MainViewModel
import com.todoapp.mobile.R
import com.todoapp.mobile.data.notification.NotificationService
import com.todoapp.mobile.di.IoDispatcher
import com.todoapp.mobile.di.MainDispatcher
import com.todoapp.mobile.domain.alarm.AlarmScheduler
import com.todoapp.mobile.domain.alarm.AlarmType
import com.todoapp.mobile.domain.alarm.buildDailyPlanAlarmItem
import com.todoapp.mobile.domain.constants.DailyPlanDefaults
import com.todoapp.mobile.domain.model.ThemePreference
import com.todoapp.mobile.domain.repository.DailyCardPosition
import com.todoapp.mobile.domain.repository.DailyPlanPreferences
import com.todoapp.mobile.domain.repository.PaletteRepository
import com.todoapp.mobile.domain.repository.ThemeRepository
import com.todoapp.uikit.components.TDOverlayDailyPlanNotificationCard
import com.todoapp.uikit.components.TDOverlayNotificationCard
import com.todoapp.uikit.theme.PaletteKit
import com.todoapp.uikit.theme.TDTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject
import com.example.uikit.R as UikitR

@AndroidEntryPoint
class OverlayService :
    Service(),
    LifecycleOwner,
    SavedStateRegistryOwner {
    @Inject
    lateinit var dailyPlanPreferences: DailyPlanPreferences

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    @Inject
    lateinit var themeRepository: ThemeRepository

    @Inject
    lateinit var paletteRepository: PaletteRepository

    @Inject
    lateinit var alarmSoundPreferences: com.todoapp.mobile.domain.repository.AlarmSoundPreferences

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    @MainDispatcher
    lateinit var mainDispatcher: CoroutineDispatcher

    private lateinit var windowManager: WindowManager
    private val ringtone = com.todoapp.mobile.common.RingtoneHolder()

    // Service-scoped coroutine host for all background work (ringtone playback, card-position
    // saves, alarm reschedule). Backed by a SupervisorJob that lives until onDestroy() cancels it,
    // so a failing child does not tear down the others. `by lazy` because ioDispatcher is
    // field-injected in onCreate and is not available at construction time.
    private val serviceScope by lazy {
        CoroutineScope(SupervisorJob() + ioDispatcher)
    }

    @Suppress("ktlint:standard:backing-property-naming")
    private val _lifecycleRegistry = LifecycleRegistry(this)

    @Suppress("ktlint:standard:backing-property-naming")
    private val _savedStateRegistryController: SavedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry = _savedStateRegistryController.savedStateRegistry
    override val lifecycle: Lifecycle = _lifecycleRegistry
    private var taskOverlayView: View? = null
    private var dailyPlanOverlayView: View? = null

    override fun onBind(intent: Intent?): IBinder = throw UnsupportedOperationException(BOUND_MODE_NOT_SUPPORTED)

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        _savedStateRegistryController.performAttach()
        _savedStateRegistryController.performRestore(null)
        _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        OverlayServiceChannel.ensure(this)
    }

    override fun onStartCommand(
        intent: Intent,
        flags: Int,
        startId: Int,
    ): Int {
        Timber.tag(TAG).d("onStartCommand called, extras: %s", intent.extras)
        promoteToForeground()
        if (intent.hasExtra(INTENT_EXTRA_COMMAND_SHOW_OVERLAY)) {
            val message = intent.getStringExtra(INTENT_EXTRA_COMMAND_SHOW_OVERLAY)
            val minutesBefore = intent.getLongExtra(INTENT_EXTRA_LONG, 0)
            val overlayType = intent.getStringExtra(INTENT_EXTRA_OVERLAY_TYPE) ?: OVERLAY_TYPE_TASK
            val taskId = intent.getLongExtra(INTENT_EXTRA_TASK_ID, -1L).takeIf { it > 0 }
            val overlayShown = showOverlay(message.orEmpty(), minutesBefore, overlayType, taskId)
            // Honor user-selected alarm sound preference. Channel sounds are immutable post-creation
            // so we play the ringtone manually here. Skip when we fell back to a notification — the
            // NotificationService plays its own alarm sound, so this would double up.
            if (overlayShown) {
                serviceScope.launch {
                    val uri = runCatching { alarmSoundPreferences.currentAlarmSoundUri() }.getOrNull()
                    ringtone.play(context = this@OverlayService, explicitUri = uri)
                }
            }
        } else if (intent.hasExtra(INTENT_EXTRA_COMMAND_HIDE_OVERLAY)) {
            hideOverlay()
            ringtone.stop()
        }
        return START_NOT_STICKY
    }

    private fun showOverlay(
        message: String,
        minutesBefore: Long,
        overlayType: String,
        taskId: Long? = null,
    ): Boolean {
        val targetViewRef =
            when (overlayType) {
                OVERLAY_TYPE_DAILY_PLAN -> dailyPlanOverlayView
                else -> taskOverlayView
            }
        if (targetViewRef != null) return true

        // The overlay-vs-notification choice is frozen into the PendingIntent at *schedule* time
        // (AlarmSchedulerImpl). If the user granted "draw over other apps", scheduled a reminder,
        // then revoked it before fire time, addView() below would throw BadTokenException and crash
        // this foreground service. Re-check at fire time and degrade to the notification instead.
        if (!Settings.canDrawOverlays(this)) {
            Timber.tag(TAG).w("Overlay permission revoked before fire; falling back to notification")
            fallbackToNotification(message, minutesBefore, taskId)
            stopSelf()
            return false
        }

        _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val layoutParams = getLayoutParams(overlayType)

        if (overlayType == OVERLAY_TYPE_DAILY_PLAN) {
            serviceScope.launch {
                val saved = dailyPlanPreferences.observeCardPosition().first()
                withContext(mainDispatcher) {
                    layoutParams.x = saved.cardPositionX.toInt()
                    layoutParams.y = saved.cardPositionY.toInt()
                    dailyPlanOverlayView?.let {
                        runCatching { windowManager.updateViewLayout(it, layoutParams) }
                            .onFailure { e -> Timber.tag(TAG).w(e, "updateViewLayout (restore) failed") }
                    }
                }
            }
        }

        val newView =
            ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@OverlayService)
                setViewTreeSavedStateRegistryOwner(this@OverlayService)
                setContent {
                    val themePreference by themeRepository.themeFlow
                        .collectAsStateWithLifecycle(initialValue = ThemePreference.SYSTEM_DEFAULT)
                    val palette by paletteRepository.paletteFlow
                        .collectAsStateWithLifecycle(initialValue = PaletteKit.ORIGINAL)
                    val isSystemDark = isSystemInDarkTheme()
                    val darkTheme =
                        when (themePreference) {
                            ThemePreference.DARK_MODE -> true
                            ThemePreference.LIGHT_MODE -> false
                            ThemePreference.SYSTEM_DEFAULT -> isSystemDark
                        }
                    TDTheme(darkTheme = darkTheme, palette = palette) {
                        var show by remember { mutableStateOf(true) }
                        LaunchedEffect(show) {
                            if (!show) {
                                delay(HIDE_OVERLAY_ANIMATION_DELAY)
                                hideOverlay()
                            }
                        }
                        when (overlayType) {
                            OVERLAY_TYPE_DAILY_PLAN -> {
                                TDOverlayDailyPlanNotificationCard(
                                    isVisible = show,
                                    onDismiss = { show = false },
                                    onOpenApp = {
                                        show = false
                                        openApp()
                                    },
                                    onDrag = { dx, dy ->
                                        layoutParams.x += dx.toInt()
                                        layoutParams.y += dy.toInt()
                                        runCatching { windowManager.updateViewLayout(this@apply, layoutParams) }
                                            .onFailure { e -> Timber.tag(TAG).w(e, "updateViewLayout (drag) failed") }
                                    },
                                    onDragEnd = {
                                        serviceScope.launch {
                                            dailyPlanPreferences.saveCardPosition(
                                                DailyCardPosition(
                                                    layoutParams.x.toFloat(),
                                                    layoutParams.y.toFloat(),
                                                ),
                                            )
                                        }
                                    },
                                )
                            }

                            OVERLAY_TYPE_TASK -> {
                                TDOverlayNotificationCard(
                                    message = message,
                                    minutesBefore = minutesBefore,
                                    show = show,
                                    onDismissClick = { show = false },
                                    onOpenClick = {
                                        show = false
                                        openApp(taskId)
                                    },
                                )
                            }
                        }
                    } // TDTheme
                }
            }
        when (overlayType) {
            OVERLAY_TYPE_DAILY_PLAN -> {
                dailyPlanOverlayView = newView
                rescheduleNextDailyPlan()
            }

            OVERLAY_TYPE_TASK -> taskOverlayView = newView
        }
        return runCatching {
            windowManager.addView(newView, layoutParams)
            true
        }.getOrElse { e ->
            // Permission can be pulled between the canDrawOverlays check above and here, or the OEM
            // can deny the window. Clear the ref we just stored (else hideOverlay() would removeView
            // a window that was never added and crash again), then degrade to the notification.
            Timber.tag(TAG).w(e, "addView failed; falling back to notification")
            when (overlayType) {
                OVERLAY_TYPE_DAILY_PLAN -> dailyPlanOverlayView = null
                OVERLAY_TYPE_TASK -> taskOverlayView = null
            }
            fallbackToNotification(message, minutesBefore, taskId)
            stopSelf()
            false
        }
    }

    private fun rescheduleNextDailyPlan() {
        serviceScope.launch {
            val time =
                dailyPlanPreferences.observePlanTime().first()
                    ?: DailyPlanDefaults.DEFAULT_PLAN_TIME

            val now = LocalDateTime.now()
            val item =
                buildDailyPlanAlarmItem(
                    selectedTime = time,
                    now = now,
                    message = "",
                )
            alarmScheduler.schedule(item, AlarmType.DAILY_PLAN)
        }
    }

    private fun hideOverlay() {
        taskOverlayView?.let {
            runCatching { windowManager.removeView(it) }
                .onFailure { e -> Timber.tag(TAG).w(e, "removeView (task) failed") }
            taskOverlayView = null
        }
        dailyPlanOverlayView?.let {
            runCatching { windowManager.removeView(it) }
                .onFailure { e -> Timber.tag(TAG).w(e, "removeView (daily plan) failed") }
            dailyPlanOverlayView = null
        }
        _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        startedAsForeground = false
        stopSelf()
    }

    override fun onDestroy() {
        // Cancel every coroutine started on serviceScope. The SupervisorJob never completes on
        // its own, so without this the scope — and the OverlayService reference its children
        // capture — would survive stopSelf() and leak the Service.
        serviceScope.cancel()
        ringtone.stop()
        super.onDestroy()
    }

    private var startedAsForeground: Boolean = false

    private fun promoteToForeground() {
        if (startedAsForeground) return
        val notification: Notification = NotificationCompat
            .Builder(this, OverlayServiceChannel.CHANNEL_ID)
            .setSmallIcon(UikitR.drawable.ic_sand_clock)
            .setContentTitle(getString(R.string.alarm_overlay_running))
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    OverlayServiceChannel.FOREGROUND_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            } else {
                startForeground(OverlayServiceChannel.FOREGROUND_NOTIFICATION_ID, notification)
            }
            startedAsForeground = true
        }.onFailure {
            // startForegroundService() was already called by AlarmFireReceiver, so if this promotion
            // fails the system kills the process with the un-catchable "did not call startForeground
            // in time" exception. Stop cleanly instead.
            Timber.tag(TAG).w(it, "startForeground failed; stopping service")
            stopSelf()
        }
    }

    private fun getLayoutParams(overlayType: String): WindowManager.LayoutParams = WindowManager
        .LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y =
                if (overlayType == OVERLAY_TYPE_DAILY_PLAN) {
                    val bottomMarginPx = (DAILY_PLAN_BOTTOM_MARGIN_DP * resources.displayMetrics.density).toInt()
                    resources.displayMetrics.heightPixels - bottomMarginPx
                } else {
                    0
                }
        }

    private fun openApp(taskId: Long? = null) {
        val intent =
            Intent(this, MainActivity::class.java).apply {
                // CLEAR_TOP (like NotificationService) so onNewIntent fires on a warm app and the
                // reminder_task_id extra is actually delivered to MainViewModel.onPushIntent.
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                taskId?.let { putExtra(MainViewModel.EXTRA_REMINDER_TASK_ID, it) }
            }
        // A raw startActivity() from this background service is silently dropped by BAL on
        // Android 14+, even while the overlay window is visible — the app never opens. Launch via a
        // PendingIntent and explicitly opt into the background activity start, the mechanism the
        // platform sanctions for exactly this case. Older versions keep the direct start.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent =
                PendingIntent.getActivity(
                    this,
                    taskId?.toInt() ?: 0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )

            // MODE_BACKGROUND_ACTIVITY_START_ALLOWED is deprecated on API 35+ in favor of the
            // finer-grained ALLOW_ALWAYS/ALLOW_IF_VISIBLE, but it is the only mode available on
            // API 34 and still maps to "allow" on 35/36 — keep the single constant for both.
            @Suppress("DEPRECATION")
            val options =
                ActivityOptions
                    .makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED,
                    )
            runCatching { pendingIntent.send(this, 0, null, null, null, null, options.toBundle()) }
                .onFailure { Timber.tag(TAG).w(it, "openApp: PendingIntent.send failed") }
        } else {
            runCatching { startActivity(intent) }
                .onFailure { Timber.tag(TAG).w(it, "openApp: startActivity failed") }
        }
    }

    // Graceful degradation when the overlay window can't be drawn (permission revoked after
    // scheduling, or OEM denial). Mirrors the NotificationService branch that AlarmFireReceiver
    // would have taken had the permission been absent at schedule time — same extra keys.
    private fun fallbackToNotification(
        message: String,
        minutesBefore: Long,
        taskId: Long? = null,
    ) {
        runCatching {
            ContextCompat.startForegroundService(
                this,
                Intent(this, NotificationService::class.java).apply {
                    putExtra(NotificationService.INTENT_EXTRA_MESSAGE, message)
                    putExtra(NotificationService.INTENT_EXTRA_LONG, minutesBefore)
                    taskId?.let { putExtra(NotificationService.INTENT_EXTRA_TASK_ID, it) }
                },
            )
        }.onFailure { Timber.tag(TAG).w(it, "Notification fallback failed") }
    }

    companion object {
        const val INTENT_EXTRA_COMMAND_SHOW_OVERLAY = "INTENT_EXTRA_COMMAND_SHOW_OVERLAY"
        const val INTENT_EXTRA_COMMAND_HIDE_OVERLAY = "INTENT_EXTRA_COMMAND_HIDE_OVERLAY"
        const val INTENT_EXTRA_LONG = "INTENT_EXTRA_LONG"
        const val HIDE_OVERLAY_ANIMATION_DELAY = 300L
        const val BOUND_MODE_NOT_SUPPORTED = "Bound mode not supported"
        const val INTENT_EXTRA_OVERLAY_TYPE = "INTENT_EXTRA_OVERLAY_TYPE"
        const val INTENT_EXTRA_TASK_ID = "INTENT_EXTRA_TASK_ID"
        const val OVERLAY_TYPE_TASK = "OVERLAY_TYPE_TASK"
        const val OVERLAY_TYPE_DAILY_PLAN = "OVERLAY_TYPE_DAILY_PLAN"
        const val DAILY_PLAN_BOTTOM_MARGIN_DP = 80
        private const val TAG = "OverlayService"
    }
}
