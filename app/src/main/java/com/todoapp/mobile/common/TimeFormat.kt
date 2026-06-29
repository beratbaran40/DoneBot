package com.todoapp.mobile.common

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Time-of-day pattern that follows the device's 12h/24h system setting
 * (Settings → System → Date & time → "Use 24-hour format"): "HH:mm" or "h:mm a".
 */
fun deviceTimePattern(context: Context): String = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"

/** [DateTimeFormatter] for a bare time-of-day that follows the device 12h/24h setting. */
fun deviceTimeFormatter(context: Context, locale: Locale = Locale.getDefault()): DateTimeFormatter = DateTimeFormatter.ofPattern(deviceTimePattern(context), locale)

/** Composable accessor for [deviceTimePattern], re-read when the context changes. */
@Composable
fun rememberDeviceTimePattern(): String {
    val context = LocalContext.current
    return remember(context) { deviceTimePattern(context) }
}
