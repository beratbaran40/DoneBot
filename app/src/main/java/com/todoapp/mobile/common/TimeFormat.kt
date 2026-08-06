package com.todoapp.mobile.common

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.todoapp.mobile.R
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

/**
 * How long is left until the task, for a reminder firing [minutesBefore] minutes early: "15 minutes",
 * "1 hour", "1 day".
 *
 * The reminder surfaces used to interpolate the raw minute count, so picking "1 day before" produced
 * "in 1440 minutes". Both the tray notification and the overlay card format through here, so the two
 * can no longer word the same moment differently.
 */
fun reminderLeadDuration(context: Context, minutesBefore: Long): String {
    val minutes = minutesBefore.coerceAtLeast(0L)
    return when {
        minutes % MINUTES_PER_DAY == 0L && minutes >= MINUTES_PER_DAY ->
            context.resources.getQuantityString(
                R.plurals.reminder_lead_days,
                (minutes / MINUTES_PER_DAY).toInt(),
                (minutes / MINUTES_PER_DAY).toInt(),
            )

        minutes % MINUTES_PER_HOUR == 0L && minutes >= MINUTES_PER_HOUR ->
            context.resources.getQuantityString(
                R.plurals.reminder_lead_hours,
                (minutes / MINUTES_PER_HOUR).toInt(),
                (minutes / MINUTES_PER_HOUR).toInt(),
            )

        else ->
            context.resources.getQuantityString(
                R.plurals.reminder_lead_minutes,
                minutes.toInt(),
                minutes.toInt(),
            )
    }
}

/**
 * The short lead-in shown on the overlay card: "Now" when the task starts now, "in 15 minutes"
 * otherwise. The card used to render this as "15m ago" — the right number pointing the wrong way in
 * time, since the reminder fires *before* the task.
 */
fun reminderLeadLabel(context: Context, minutesBefore: Long): String = if (minutesBefore <= 0L) {
    context.getString(R.string.reminder_lead_now)
} else {
    context.getString(R.string.reminder_lead_in_format, reminderLeadDuration(context, minutesBefore))
}

private const val MINUTES_PER_HOUR = 60L
private const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR
