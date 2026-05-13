package com.todoapp.mobile.ui.overlay

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.todoapp.mobile.MainActivity
import com.todoapp.mobile.R
import com.todoapp.mobile.di.IoDispatcher
import com.todoapp.mobile.di.MainDispatcher
import com.todoapp.mobile.domain.alarm.AlarmScheduler
import com.todoapp.mobile.domain.alarm.AlarmType
import com.todoapp.mobile.domain.alarm.buildDailyPlanAlarmItem
import com.todoapp.mobile.domain.constants.DailyPlanDefaults
import com.todoapp.mobile.domain.model.ThemePreference
import com.todoapp.mobile.domain.repository.DailyCardPosition
import com.todoapp.mobile.domain.repository.DailyPlanPreferences
import com.todoapp.mobile.domain.repository.ThemeRepository
import com.todoapp.uikit.components.TDOverlayDailyPlanNotificationCard
import com.todoapp.uikit.components.TDOverlayNotificationCard
import com.todoapp.uikit.theme.TDTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    lateinit var alarmSoundPreferences: com.todoapp.mobile.domain.repository.AlarmSoundPreferences

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    @MainDispatcher
    lateinit var mainDispatcher: CoroutineDispatcher

    private lateinit var windowManager: WindowManager
    private val ringtone = com.todoapp.mobile.common.RingtoneHolder()
    private val ringtoneScope by lazy {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + ioDispatcher)
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
        android.util.Log.d("OverlayService", "onStartCommand called, extras: ${intent.extras}")
        promoteToForeground()
        if (intent.hasExtra(INTENT_EXTRA_COMMAND_SHOW_OVERLAY)) {
            val message = intent.getStringExtra(INTENT_EXTRA_COMMAND_SHOW_OVERLAY)
            val minutesBefore = intent.getLongExtra(INTENT_EXTRA_LONG, 0)
            val overlayType = intent.getStringExtra(INTENT_EXTRA_OVERLAY_TYPE) ?: OVERLAY_TYPE_TASK
            showOverlay(message.orEmpty(), minutesBefore, overlayType)
            // Honor user-selected alarm sound preference. Channel sounds are immutable post-creation
            // so we play the ringtone manually here.
            ringtoneScope.launch {
                val uri = runCatching { alarmSoundPreferences.currentAlarmSoundUri() }.getOrNull()
                ringtone.play(context = this@OverlayService, explicitUri = uri)
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
    ) {
        val targetViewRef =
            when (overlayType) {
                OVERLAY_TYPE_DAILY_PLAN -> dailyPlanOverlayView
                else -> taskOverlayView
            }
        if (targetViewRef != null) return

        _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val layoutParams = getLayoutParams(overlayType)

        if (overlayType == OVERLAY_TYPE_DAILY_PLAN) {
            CoroutineScope(ioDispatcher).launch {
                val saved = dailyPlanPreferences.observeCardPosition().first()
                withContext(mainDispatcher) {
                    layoutParams.x = saved.cardPositionX.toInt()
                    layoutParams.y = saved.cardPositionY.toInt()
                    dailyPlanOverlayView?.let {
                        windowManager.updateViewLayout(it, layoutParams)
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
                        .collectAsState(initial = ThemePreference.SYSTEM_DEFAULT)
                    val isSystemDark = isSystemInDarkTheme()
                    val darkTheme =
                        when (themePreference) {
                            ThemePreference.DARK_MODE -> true
                            ThemePreference.LIGHT_MODE -> false
                            ThemePreference.SYSTEM_DEFAULT -> isSystemDark
                        }
                    TDTheme(darkTheme = darkTheme) {
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
                                        windowManager.updateViewLayout(this@apply, layoutParams)
                                    },
                                    onDragEnd = {
                                        CoroutineScope(ioDispatcher).launch {
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
                                        openApp()
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
        windowManager.addView(newView, layoutParams)
    }

    private fun rescheduleNextDailyPlan() {
        CoroutineScope(ioDispatcher).launch {
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
            windowManager.removeView(it)
            taskOverlayView = null
        }
        dailyPlanOverlayView?.let {
            windowManager.removeView(it)
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

    private fun openApp() {
        val intent =
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        startActivity(intent)
    }

    companion object {
        const val INTENT_EXTRA_COMMAND_SHOW_OVERLAY = "INTENT_EXTRA_COMMAND_SHOW_OVERLAY"
        const val INTENT_EXTRA_COMMAND_HIDE_OVERLAY = "INTENT_EXTRA_COMMAND_HIDE_OVERLAY"
        const val INTENT_EXTRA_LONG = "INTENT_EXTRA_LONG"
        const val HIDE_OVERLAY_ANIMATION_DELAY = 300L
        const val BOUND_MODE_NOT_SUPPORTED = "Bound mode not supported"
        const val INTENT_EXTRA_OVERLAY_TYPE = "INTENT_EXTRA_OVERLAY_TYPE"
        const val OVERLAY_TYPE_TASK = "OVERLAY_TYPE_TASK"
        const val OVERLAY_TYPE_DAILY_PLAN = "OVERLAY_TYPE_DAILY_PLAN"
        const val DAILY_PLAN_BOTTOM_MARGIN_DP = 80
    }
}
