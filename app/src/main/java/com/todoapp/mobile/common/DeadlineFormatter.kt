package com.todoapp.mobile.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.todoapp.mobile.R
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

enum class DeadlineStatus { Future, Done, StillPending }

data class DeadlineDisplay(
    val primary: String,
    val secondary: String?,
    val status: DeadlineStatus,
)

private val dateFormatter = DateTimeFormatter.ofPattern("MMM d")

@Composable
fun rememberDeadlineDisplay(
    dueAtEpochMs: Long,
    isCompleted: Boolean,
    nowEpochMs: Long = System.currentTimeMillis(),
    isRecurringInstance: Boolean = false,
): DeadlineDisplay {
    val zone = ZoneId.systemDefault()
    val due = LocalDateTime.ofInstant(Instant.ofEpochMilli(dueAtEpochMs), zone)
    val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowEpochMs), zone)
    val timePattern = rememberDeviceTimePattern()
    val dateTimeFormatter = remember(timePattern) { DateTimeFormatter.ofPattern("MMM d · $timePattern") }
    val timeOnlyFormatter = remember(timePattern) { DateTimeFormatter.ofPattern(timePattern) }
    val absoluteWithTime = due.format(dateTimeFormatter)
    val absoluteDateOnly = due.format(dateFormatter)
    val timeOnly = due.format(timeOnlyFormatter)

    val minutesBetween = ChronoUnit.MINUTES.between(now, due)
    val hoursBetween = ChronoUnit.HOURS.between(now, due)
    val daysBetween = ChronoUnit.DAYS.between(now.toLocalDate(), due.toLocalDate())

    return when {
        minutesBetween < 0L -> pastDisplay(
            isCompleted = isCompleted,
            minutesPast = abs(minutesBetween).toInt(),
            hoursPast = abs(hoursBetween).toInt(),
            daysPast = abs(daysBetween).toInt(),
            absoluteWithTime = absoluteWithTime,
        )
        isCompleted -> DeadlineDisplay(
            primary = stringResource(R.string.deadline_completed_primary),
            secondary = absoluteWithTime,
            status = DeadlineStatus.Done,
        )
        hoursBetween < 24L -> {
            val hours = hoursBetween.toInt()
            val minutesInHour = (minutesBetween - hours * 60L).toInt()
            DeadlineDisplay(
                primary = stringResource(R.string.deadline_in_hours_minutes, hours, minutesInHour),
                secondary = absoluteWithTime,
                status = DeadlineStatus.Future,
            )
        }
        // Recurring instances >24h away show their fixed daily time, not a misleading countdown.
        isRecurringInstance -> DeadlineDisplay(
            primary = timeOnly,
            secondary = absoluteDateOnly,
            status = DeadlineStatus.Future,
        )
        daysBetween <= 7L -> {
            val days = daysBetween.toInt().coerceAtLeast(1)
            DeadlineDisplay(
                primary = pluralStringResource(R.plurals.deadline_days_left, days, days),
                secondary = absoluteWithTime,
                status = DeadlineStatus.Future,
            )
        }
        else -> DeadlineDisplay(
            primary = absoluteDateOnly,
            secondary = null,
            status = DeadlineStatus.Future,
        )
    }
}

@Composable
private fun pastDisplay(
    isCompleted: Boolean,
    minutesPast: Int,
    hoursPast: Int,
    daysPast: Int,
    absoluteWithTime: String,
): DeadlineDisplay {
    val agoText = when {
        daysPast >= 1 -> pluralStringResource(R.plurals.deadline_ago_days, daysPast, daysPast)
        hoursPast >= 1 -> pluralStringResource(R.plurals.deadline_ago_hours, hoursPast, hoursPast)
        else -> pluralStringResource(R.plurals.deadline_ago_minutes, minutesPast, minutesPast)
    }
    return if (isCompleted) {
        DeadlineDisplay(primary = agoText, secondary = absoluteWithTime, status = DeadlineStatus.Done)
    } else {
        val suffix = stringResource(R.string.deadline_still_pending_suffix)
        DeadlineDisplay(
            primary = "$agoText$suffix",
            secondary = absoluteWithTime,
            status = DeadlineStatus.StillPending,
        )
    }
}
