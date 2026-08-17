package com.todoapp.mobile.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.ui.common.taskform.TaskFormDateField
import com.todoapp.mobile.ui.common.taskform.TaskFrequencyChips
import com.todoapp.mobile.ui.common.taskform.TaskIntervalStepper
import com.todoapp.mobile.ui.common.taskform.TaskRepeatUntilField
import com.todoapp.mobile.ui.common.taskform.TaskWeekdayPicker
import com.todoapp.mobile.ui.details.DetailsContract.UiAction
import com.todoapp.mobile.ui.details.DetailsContract.UiState
import com.todoapp.uikit.components.TDRoutineProgress
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.theme.TDTheme

/**
 * When the task happens: its start day, the span it runs for, and — for anything that repeats — the
 * rule that governs both.
 *
 * Split out of DetailsScreen.kt, which was already past the file-size ceiling.
 */

/**
 * Frequency chips plus, for a bounded routine, how far along it is ("Day 12 of 30"). The counts are
 * computed in the ViewModel — the by-weekday case walks real firing days and must not run per frame.
 */
@Composable
internal fun DetailsRecurrenceBlock(
    uiState: UiState.Success,
    onAction: (UiAction) -> Unit,
) {
    TaskFrequencyChips(
        selected = uiState.selectedRecurrence,
        onSelect = { onAction(UiAction.OnRecurrenceChange(it)) },
    )
    TaskIntervalStepper(
        frequency = uiState.selectedRecurrence,
        interval = uiState.recurrenceInterval,
        onChange = { onAction(UiAction.OnIntervalChange(it)) },
    )
    if (uiState.selectedRecurrence == Recurrence.WEEKLY) {
        TaskWeekdayPicker(
            selected = uiState.recurrenceByDay,
            onToggle = { onAction(UiAction.OnWeekdayToggle(it)) },
        )
    }
    val dayIndex = uiState.routineDayIndex
    val dayTotal = uiState.routineDayTotal
    if (dayIndex != null && dayTotal != null) {
        TDRoutineProgress(
            current = dayIndex,
            total = dayTotal,
            label = stringResource(R.string.routine_day_progress, dayIndex, dayTotal),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * The start day and, for a repeating task, where the repeat stops.
 *
 * This used to call [com.todoapp.uikit.components.TDDatePickerDialog] directly with neither
 * `onRangeSelect` nor `rangeEnd`, which the picker reads as "spans are off" and honours by ignoring
 * the end entirely — no band in the grid, no summary sentence, and a collapsed field that named only
 * the start. A task created by holding two days in the Creation Hub therefore came back looking like
 * a one-day task, with its actual end stranded in a chip row further down the screen.
 *
 * Going through the shared [TaskFormDateField] instead makes this identical to the create form:
 * the hold-then-tap gesture, the band, the "runs between … and …" sentence, and — new here — the
 * ability to move an arbitrary end date at all, which the month presets alone could never express.
 *
 * `onDeselect` is deliberately not passed. A task always has a date, and offering to clear it left
 * the field reading "Pick a date" while `taskDate` sat unchanged behind it: Save stayed greyed out
 * and Cancel had nothing to undo.
 */
@Composable
internal fun DetailsDateSection(
    uiState: UiState.Success,
    onAction: (UiAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TDText(
            text = stringResource(R.string.task_date),
            style = TDTheme.typography.heading6,
            color = TDTheme.colors.onSurface,
        )
        TaskFormDateField(
            date = uiState.taskDate,
            onSelect = { onAction(UiAction.OnTaskDateEdit(it)) },
            // An end means nothing on something that doesn't repeat — firesOn never reads it — so the
            // gesture, the hint and the band are all withheld there. Same question the create form asks.
            onRangeSelect = if (uiState.capabilities.recurs) {
                { start, end ->
                    onAction(UiAction.OnTaskDateEdit(start))
                    onAction(UiAction.OnRecurrenceUntilChange(end))
                }
            } else {
                null
            },
            rangeEnd = uiState.recurrenceUntil,
            onRangeClear = { onAction(UiAction.OnRecurrenceUntilChange(null)) },
        )
        if (uiState.capabilities.recurs) {
            TaskRepeatUntilField(
                anchor = uiState.taskDate,
                until = uiState.recurrenceUntil,
                onSelect = { onAction(UiAction.OnRecurrenceUntilChange(it)) },
            )
        }
    }
}
