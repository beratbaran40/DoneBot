package com.todoapp.mobile.common

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

fun Context.needsPostNotificationsPermission(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
    ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.POST_NOTIFICATIONS,
    ) != PackageManager.PERMISSION_GRANTED

fun Context.needsOverlayPermission(): Boolean = !Settings.canDrawOverlays(this)

fun Context.needsCameraPermission(): Boolean = ContextCompat.checkSelfPermission(
    this,
    Manifest.permission.CAMERA,
) != PackageManager.PERMISSION_GRANTED

/**
 * Opens this app's notification settings, where POST_NOTIFICATIONS can be granted after the OS has
 * stopped offering to prompt for it. More specific than [openAppDetailsSettings] — it lands on the
 * notification screen itself rather than the app's permission list.
 */
fun Context.openAppNotificationSettings() {
    runCatching {
        startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

/**
 * Opens this app's details page in system Settings, where the user can grant a
 * permission the OS refuses to prompt for (e.g. permanently-denied CAMERA).
 */
fun Context.openAppDetailsSettings() {
    runCatching {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }
}
