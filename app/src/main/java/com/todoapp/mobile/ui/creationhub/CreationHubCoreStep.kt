package com.todoapp.mobile.ui.creationhub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.ui.common.taskform.TaskFormDateField
import com.todoapp.mobile.ui.common.taskform.TaskFrequencyChips
import com.todoapp.mobile.ui.common.taskform.TaskIntervalStepper
import com.todoapp.mobile.ui.common.taskform.TaskReminderChips
import com.todoapp.mobile.ui.common.taskform.TaskReminderTimesEditor
import com.todoapp.mobile.ui.common.taskform.TaskRepeatUntilField
import com.todoapp.mobile.ui.common.taskform.TaskSubtaskEditor
import com.todoapp.mobile.ui.common.taskform.TaskWeekdayPicker
import com.todoapp.mobile.ui.creationhub.CreationHubContract.TaskScope
import com.todoapp.mobile.ui.creationhub.CreationHubContract.TaskType
import com.todoapp.mobile.ui.creationhub.CreationHubContract.UiAction
import com.todoapp.mobile.ui.creationhub.CreationHubContract.UiState
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDCompactOutlinedTextField

@Composable
internal fun CreationHubCoreStep(
    state: UiState,
    onAction: (UiAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TDCompactOutlinedTextField(
            value = state.title,
            label = stringResource(R.string.task_title),
            placeholder = stringResource(CreationHubPlaceholders.titleRes(state.placeholderIndex)),
            onValueChange = { onAction(UiAction.OnTitleChange(it)) },
            isError = state.titleError,
            supportingText = if (state.titleError) stringResource(R.string.error_task_title_required) else null,
        )

        // Who it belongs to comes before what shape it takes: a group task's audience changes how the
        // rest of the form reads. Only rendered for the group scope — a personal task has no group,
        // no assignee and no priority.
        if (state.scope == TaskScope.GROUP) {
            CreationHubGroupSection(state = state, onAction = onAction)
        }

        // Field order is type-specific: routine surfaces frequency right after the title (before the
        // start date); one-time and staged keep the date first.
        when (state.taskType) {
            TaskType.ONE_TIME -> {
                TaskFormDateField(date = state.date, onSelect = { onAction(UiAction.OnDateSelect(it)) })
                TaskReminderChips(
                    selected = state.reminderOffsetMinutes,
                    onSelect = { onAction(UiAction.OnReminderSelect(it)) },
                )
            }
            TaskType.ROUTINE -> {
                TaskFrequencyChips(
                    selected = state.recurrence,
                    onSelect = { onAction(UiAction.OnFrequencySelect(it)) },
                )
                // A routine repeats by definition — its chips carry no "doesn't repeat" option — so
                // the span gesture and the end field are always live, with no capability gate.
                CreationDateField(state = state, onAction = onAction, rangeEnabled = true)
                TaskRepeatUntilField(
                    anchor = state.date,
                    until = state.recurrenceUntil,
                    onSelect = { onAction(UiAction.OnRecurrenceUntilSelect(it)) },
                )
            }
            TaskType.STAGED -> {
                TaskFormDateField(date = state.date, onSelect = { onAction(UiAction.OnDateSelect(it)) })
                TaskSubtaskEditor(
                    drafts = state.subtaskDrafts,
                    onChange = { index, text -> onAction(UiAction.OnSubtaskChange(index, text)) },
                    onRemove = { onAction(UiAction.OnSubtaskRemove(it)) },
                    stepPlaceholder = { stringResource(CreationHubPlaceholders.stepRes(it)) },
                )
            }
            // Every section at once, in the same order the classic types use: what repeats, when it
            // starts, how it ends, when it reminds, what its steps are. Sections the user leaves
            // alone simply don't apply — nothing here has to be unlocked first.
            TaskType.CUSTOM -> {
                val recurs = state.recurrence != Recurrence.NONE
                // The repeat rule comes first and stays in one block — frequency, then how often,
                // then which weekdays. Asking the date first split the rule around it, and it also
                // meant the calendar's hold-two-days gesture was dead on the first open (the gesture
                // only exists for something that repeats). This way it is live straight away.
                TaskFrequencyChips(
                    selected = state.recurrence,
                    onSelect = { onAction(UiAction.OnFrequencySelect(it)) },
                    allowNone = true,
                )
                if (recurs) {
                    TaskIntervalStepper(
                        frequency = state.recurrence,
                        interval = state.recurrenceInterval,
                        onChange = { onAction(UiAction.OnIntervalChange(it)) },
                    )
                    if (state.recurrence == Recurrence.WEEKLY) {
                        TaskWeekdayPicker(
                            selected = state.recurrenceByDay,
                            onToggle = { onAction(UiAction.OnWeekdayToggle(it)) },
                        )
                    }
                }
                CreationDateField(state = state, onAction = onAction, rangeEnabled = recurs)
                // An end date and absolute reminder times only make sense once it repeats; before
                // that a one-off reminds relative to its own start, like every other single task.
                if (recurs) {
                    TaskRepeatUntilField(
                        anchor = state.date,
                        until = state.recurrenceUntil,
                        onSelect = { onAction(UiAction.OnRecurrenceUntilSelect(it)) },
                    )
                    TaskReminderTimesEditor(
                        times = state.reminderTimes,
                        onAdd = { onAction(UiAction.OnReminderTimeAdd(it)) },
                        onRemove = { onAction(UiAction.OnReminderTimeRemove(it)) },
                    )
                } else {
                    TaskReminderChips(
                        selected = state.reminderOffsetMinutes,
                        onSelect = { onAction(UiAction.OnReminderSelect(it)) },
                    )
                }
                TaskSubtaskEditor(
                    drafts = state.subtaskDrafts,
                    onChange = { index, text -> onAction(UiAction.OnSubtaskChange(index, text)) },
                    onRemove = { onAction(UiAction.OnSubtaskRemove(it)) },
                    stepPlaceholder = { stringResource(CreationHubPlaceholders.stepRes(it)) },
                )
            }
            null ->
                TaskFormDateField(date = state.date, onSelect = { onAction(UiAction.OnDateSelect(it)) })
        }

        CreationHubDetailsSection(state = state, onAction = onAction)

        TDButton(
            text = stringResource(R.string.create),
            fullWidth = true,
            isEnable = !state.isSaving,
            onClick = { onAction(UiAction.OnCreate) },
        )
    }
}

/**
 * The start-date field, with the calendar's hold-two-days gesture wired to where the repeat stops.
 *
 * [rangeEnabled] is the one question worth asking: an end date on something that doesn't repeat means
 * nothing — `firesOn` never reads it — so the gesture, the hint and the band are all withheld there.
 * Routine answers yes unconditionally; custom answers it per what the user has switched on.
 */
@Composable
private fun CreationDateField(
    state: UiState,
    onAction: (UiAction) -> Unit,
    rangeEnabled: Boolean,
) {
    TaskFormDateField(
        date = state.date,
        onSelect = { onAction(UiAction.OnDateSelect(it)) },
        onRangeSelect = if (rangeEnabled) {
            { start, end ->
                onAction(UiAction.OnDateSelect(start))
                onAction(UiAction.OnRecurrenceUntilSelect(end))
            }
        } else {
            null
        },
        rangeEnd = state.recurrenceUntil,
        // Confirming a single day drops the span. Without this the end date lived on in state, so the
        // band and the "between … and …" sentence never went away.
        onRangeClear = { onAction(UiAction.OnRecurrenceUntilSelect(null)) },
    )
}
