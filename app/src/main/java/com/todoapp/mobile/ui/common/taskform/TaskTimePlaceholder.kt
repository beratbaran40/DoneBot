package com.todoapp.mobile.ui.common.taskform

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.todoapp.mobile.R

/**
 * Empty-state placeholder for a task time field — e.g. "Starts · HH:MM" (24-hour) or
 * "Starts · H:MM AM" (12-hour). The trailing format mask follows the device's 12/24-hour
 * system setting so the hint matches how a picked time will render in the same field.
 *
 * @param isStart true for the start-time field ("Starts"), false for the end-time field ("Ends").
 */
@Composable
fun rememberTimeFieldPlaceholder(isStart: Boolean): String {
    val context = LocalContext.current
    val is24Hour = remember(context) { DateFormat.is24HourFormat(context) }
    val label = stringResource(if (isStart) R.string.starts else R.string.ends)
    val mask = stringResource(if (is24Hour) R.string.time_mask_24h else R.string.time_mask_12h)
    return stringResource(R.string.time_placeholder_with_mask, label, mask)
}
