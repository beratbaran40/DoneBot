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
                TaskFormDateField(date = state.date, onSelect = { onAction(UiAction.OnDateSelect(it)) })
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
                // Date first: "when is this?" is the question a user can always answer, while
                // "how often?" only makes sense once they have a day in mind.
                TaskFormDateField(
                    date = state.date,
                    onSelect = { onAction(UiAction.OnDateSelect(it)) },
                    // Holding two days sets the routine's end, so the gesture is only offered once
                    // the task actually repeats — an end date on a one-off means nothing.
                    onRangeSelect = if (recurs) {
                        { start, end ->
                            onAction(UiAction.OnDateSelect(start))
                            onAction(UiAction.OnRecurrenceUntilSelect(end))
                        }
                    } else {
                        null
                    },
                    rangeEnd = state.recurrenceUntil,
                )
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
            // GROUP is rendered by CreationHubGroupStep, never reaches here; arm kept for exhaustiveness.
            TaskType.GROUP, null ->
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
