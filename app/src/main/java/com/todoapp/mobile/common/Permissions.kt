package com.todoapp.mobile.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
