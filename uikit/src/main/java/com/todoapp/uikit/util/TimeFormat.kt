package com.todoapp.uikit.util

import android.content.Context
import android.text.format.DateFormat

/**
 * Whether the device is configured to use the 24-hour clock
 * (Settings → System → Date & time → "Use 24-hour format").
 *
 * Lives in :uikit and uses the framework [DateFormat] directly so components here
 * never import from :app. Mirrors `com.todoapp.mobile.common` in the app module.
 */
fun deviceUses24HourClock(context: Context): Boolean = DateFormat.is24HourFormat(context)

/**
 * Time-of-day pattern that follows the device's 12h/24h system setting:
 * "HH:mm" (24-hour) or "h:mm a" (12-hour).
 */
fun deviceTimePattern(context: Context): String = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
