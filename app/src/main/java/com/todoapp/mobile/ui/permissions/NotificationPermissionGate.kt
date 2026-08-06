package com.todoapp.mobile.ui.permissions

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.todoapp.mobile.common.needsPostNotificationsPermission
import com.todoapp.mobile.common.openAppNotificationSettings

/**
 * Whether POST_NOTIFICATIONS is standing in the way, and the one action that resolves it.
 *
 * [isBlocked] is false on anything below API 33, where the permission does not exist.
 */
@Stable
class NotificationPermissionGate internal constructor(
    val isBlocked: Boolean,
    val fix: () -> Unit,
)

/**
 * For features that quietly do nothing without notification permission — the pomodoro foreground
 * service, and therefore background ambience — so a control can say so instead of lying.
 *
 * [NotificationPermissionPrompt] is the full-card version for a screen that is *about* the
 * permission; this is the same logic with no UI of its own, for a control that merely depends on it.
 */
@Composable
fun rememberNotificationPermissionGate(): NotificationPermissionGate {
    val context = LocalContext.current
    var isBlocked by remember { mutableStateOf(context.needsPostNotificationsPermission()) }
    var routeToSettings by remember { mutableStateOf(false) }

    // Granting in system Settings calls nothing back, so the answer is re-read on every resume
    // rather than only when a launcher returns.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) isBlocked = context.needsPostNotificationsPermission()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        isBlocked = !granted
        if (!granted) {
            // shouldShowRequestPermissionRationale is false BOTH before the first ask and after a
            // permanent denial, so it can only tell those apart once an ask has been made. Hence the
            // order: always ask first, and only route to Settings once the OS has refused to prompt.
            val activity = context as? Activity
            routeToSettings = activity == null ||
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    return remember(isBlocked, routeToSettings, launcher) {
        NotificationPermissionGate(
            isBlocked = isBlocked,
            fix = {
                when {
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> Unit
                    routeToSettings -> context.openAppNotificationSettings()
                    else -> launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
        )
    }
}
