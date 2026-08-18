package com.todoapp.mobile.ui.common.taskform

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.common.rememberDeviceTimePattern
import com.todoapp.mobile.domain.alarm.MAX_REMINDER_SLOTS
import com.todoapp.uikit.components.TDChoiceChip
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.components.TDWheelTimePickerDialog
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.uikit.R as UiKitR

/**
 * Form sections that only a custom task shows. Kept out of TaskFormSections.kt, which is already at
 * the ~300-line ceiling.
 */

/** Preset spans, in months from the task's start date. */
private val END_PRESET_MONTHS = listOf(1L, 3L, 6L, 9L, 12L)

/**
 * Scheduled end of a routine ("for one month"), as preset spans plus an explicit "no end".
 *
 * [anchor] is the task's start date — the offsets are measured from it, not from today, so changing
 * the start date after picking "1 month" keeps the span a month rather than silently shortening it.
 *
 * The same value can also be set by holding two days in the date picker, which lands on dates no
 * preset matches. That is why the exact end is spelled out underneath: without it a user who picked
 * a span in the calendar would see every chip unselected and no clue what the end actually is.
 */
@Composable
fun TaskRepeatUntilField(
    anchor: LocalDate,
    until: LocalDate?,
    onSelect: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Whether "No end" is offered. False for a group task: its update endpoint reads a null field as
     * "leave it alone", so removing an end is not something the client can express there — and a chip
     * that silently does nothing is worse than one that isn't shown. Flip it back on once the backend
     * grows a `clearRecurrenceUntil` flag.
     */
    allowNoEnd: Boolean = true,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TaskFormSectionLabel(stringResource(R.string.creation_end_label))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (allowNoEnd) {
                TDChoiceChip(
                    label = stringResource(R.string.creation_end_never),
                    selected = until == null,
                    onClick = { onSelect(null) },
                    selectedContainerColor = TDTheme.colors.pendingGray,
                    selectedContentColor = TDTheme.colors.white,
                )
            }
            END_PRESET_MONTHS.forEach { months ->
                val end = anchor.plusMonths(months).minusDays(1)
                TDChoiceChip(
                    label = stringResource(endPresetLabel(months)),
                    selected = until == end,
                    onClick = { onSelect(end) },
                    selectedContainerColor = TDTheme.colors.pendingGray,
                    selectedContentColor = TDTheme.colors.white,
                )
            }
        }
        until?.let { end ->
            TDText(
                text = stringResource(R.string.creation_end_on_date, end.format(END_DATE_FORMAT)),
                style = TDTheme.typography.subheading1,
                color = TDTheme.colors.pendingGray,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

/**
 * The times a task reminds at each occurrence day ("08:00 / 14:00 / 20:00").
 *
 * Capped at [MAX_REMINDER_SLOTS]: each one is a separate armed alarm, and without the exact-alarm
 * permission Android throttles these to roughly one per nine minutes, so a longer list would
 * silently not all fire. Times are kept sorted and de-duplicated by the caller.
 */
@Composable
fun TaskReminderTimesEditor(
    times: List<LocalTime>,
    onAdd: (LocalTime) -> Unit,
    onRemove: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by remember { mutableStateOf(false) }
    // Honours the device's 12/24-hour setting, like every other time display in the app.
    val timePattern = rememberDeviceTimePattern()
    val formatter = remember(timePattern) { DateTimeFormatter.ofPattern(timePattern, Locale.getDefault()) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TaskFormSectionLabel(stringResource(R.string.creation_reminder_times_label))
        times.forEach { time ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TDText(
                    text = time.format(formatter),
                    style = TDTheme.typography.subheading2,
                    color = TDTheme.colors.onBackground,
                )
                IconButton(onClick = { onRemove(time) }) {
                    Icon(
                        painter = tdPainter(UiKitR.drawable.ic_close),
                        contentDescription = stringResource(R.string.creation_remove_reminder_cd),
                        tint = TDTheme.colors.pendingGray,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        if (times.size < MAX_REMINDER_SLOTS) {
            TDChoiceChip(
                label = stringResource(R.string.creation_reminder_add),
                selected = false,
                onClick = { picking = true },
                leadingIcon = tdPainter(UiKitR.drawable.ic_plus),
            )
        } else {
            TDText(
                text = stringResource(R.string.creation_reminder_max, MAX_REMINDER_SLOTS),
                style = TDTheme.typography.subheading1,
                color = TDTheme.colors.pendingGray,
            )
        }
    }
    if (picking) {
        TDWheelTimePickerDialog(
            initialTime = null,
            onConfirm = {
                onAdd(it)
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
}

private val END_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

@StringRes
private fun endPresetLabel(months: Long): Int = when (months) {
    1L -> R.string.creation_end_1_month
    3L -> R.string.creation_end_3_months
    6L -> R.string.creation_end_6_months
    9L -> R.string.creation_end_9_months
    else -> R.string.creation_end_1_year
}

@TDPreview
@Composable
private fun TaskRepeatUntilFieldPreview() {
    TDTheme {
        val anchor = LocalDate.of(2026, 8, 3)
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TaskRepeatUntilField(anchor = anchor, until = null, onSelect = {})
            TaskRepeatUntilField(anchor = anchor, until = anchor.plusMonths(1).minusDays(1), onSelect = {})
        }
    }
}

@TDPreview
@Composable
private fun TaskReminderTimesEditorPreview() {
    TDTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            TaskReminderTimesEditor(times = emptyList(), onAdd = {}, onRemove = {})
            TaskReminderTimesEditor(
                times = listOf(LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(20, 0)),
                onAdd = {},
                onRemove = {},
            )
        }
    }
}
