package com.todoapp.mobile.ui.common.taskform

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.uikit.components.TDChoiceChip
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import com.example.uikit.R as UiKitR

/** Sections for the extended recurrence rule: "every other day", "Mon / Wed / Fri". */

const val MAX_RECURRENCE_INTERVAL = 30

/**
 * "Repeat every N <periods>". A stepper rather than free text because the value is bounded and one
 * tap is the common case ("every other day" = 2).
 */
@Composable
fun TaskIntervalStepper(
    frequency: Recurrence,
    interval: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TaskFormSectionLabel(stringResource(R.string.creation_interval_label))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onChange((interval - 1).coerceAtLeast(1)) },
                enabled = interval > 1,
            ) {
                Icon(
                    painter = tdPainter(UiKitR.drawable.ic_minus),
                    contentDescription = stringResource(R.string.creation_interval_decrease_cd),
                    tint = TDTheme.colors.onBackground,
                    modifier = Modifier.size(16.dp),
                )
            }
            TDText(
                text = recurrenceIntervalLabel(frequency, interval),
                style = TDTheme.typography.subheading2,
                color = TDTheme.colors.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 120.dp),
            )
            IconButton(
                onClick = { onChange((interval + 1).coerceAtMost(MAX_RECURRENCE_INTERVAL)) },
                enabled = interval < MAX_RECURRENCE_INTERVAL,
            ) {
                Icon(
                    painter = tdPainter(UiKitR.drawable.ic_plus),
                    contentDescription = stringResource(R.string.creation_interval_increase_cd),
                    tint = TDTheme.colors.onBackground,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/**
 * The weekdays a WEEKLY rule fires on. An empty selection is valid and means "the start date's own
 * weekday" — the legacy behaviour — so the user is never forced to pick before the form is usable.
 */
@Composable
fun TaskWeekdayPicker(
    selected: Set<DayOfWeek>,
    onToggle: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = Locale.getDefault()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TaskFormSectionLabel(stringResource(R.string.creation_weekdays_label))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            DayOfWeek.entries.forEach { day ->
                TDChoiceChip(
                    // Locale-aware short name — no strings.xml entry can beat the platform here.
                    label = day.getDisplayName(TextStyle.SHORT, locale),
                    selected = day in selected,
                    onClick = { onToggle(day) },
                    selectedContainerColor = TDTheme.colors.pendingGray,
                    selectedContentColor = TDTheme.colors.white,
                )
            }
        }
    }
}

/** "Every 2 days" / "2 günde bir". Interval 1 falls back to the plain frequency label. */
@Composable
fun recurrenceIntervalLabel(frequency: Recurrence, interval: Int): String {
    if (interval <= 1) return stringResource(plainFrequencyLabel(frequency))
    val res = when (frequency) {
        Recurrence.DAILY -> R.string.recurrence_every_n_days
        Recurrence.WEEKLY -> R.string.recurrence_every_n_weeks
        Recurrence.MONTHLY -> R.string.recurrence_every_n_months
        Recurrence.YEARLY -> R.string.recurrence_every_n_years
        Recurrence.NONE -> return stringResource(R.string.recurrence_none)
    }
    return stringResource(res, interval)
}

private fun plainFrequencyLabel(frequency: Recurrence): Int = when (frequency) {
    Recurrence.NONE -> R.string.recurrence_none
    Recurrence.DAILY -> R.string.recurrence_daily
    Recurrence.WEEKLY -> R.string.recurrence_weekly
    Recurrence.MONTHLY -> R.string.recurrence_monthly
    Recurrence.YEARLY -> R.string.recurrence_yearly
}

@TDPreview
@Composable
private fun TaskIntervalStepperPreview() {
    TDTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TaskIntervalStepper(frequency = Recurrence.DAILY, interval = 1, onChange = {})
            TaskIntervalStepper(frequency = Recurrence.DAILY, interval = 2, onChange = {})
            TaskIntervalStepper(frequency = Recurrence.WEEKLY, interval = 3, onChange = {})
        }
    }
}

@TDPreview
@Composable
private fun TaskWeekdayPickerPreview() {
    TDTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TaskWeekdayPicker(selected = emptySet(), onToggle = {})
            TaskWeekdayPicker(
                selected = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                onToggle = {},
            )
        }
    }
}
