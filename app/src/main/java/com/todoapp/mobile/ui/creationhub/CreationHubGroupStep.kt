package com.todoapp.mobile.ui.creationhub

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.todoapp.mobile.ui.common.components.AssigneeUi
import com.todoapp.mobile.ui.common.components.GroupTaskAssigneeSelector
import com.todoapp.mobile.ui.common.components.PrioritySelector
import com.todoapp.mobile.ui.common.taskform.TaskFormDateField
import com.todoapp.mobile.ui.common.taskform.TaskFormSectionLabel
import com.todoapp.mobile.ui.creationhub.CreationHubContract.AssigneeOption
import com.todoapp.mobile.ui.creationhub.CreationHubContract.GroupOption
import com.todoapp.mobile.ui.creationhub.CreationHubContract.Step
import com.todoapp.mobile.ui.creationhub.CreationHubContract.TaskType
import com.todoapp.mobile.ui.creationhub.CreationHubContract.UiAction
import com.todoapp.mobile.ui.creationhub.CreationHubContract.UiState
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDChoiceChip
import com.todoapp.uikit.components.TDCompactOutlinedTextField
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

/**
 * Group-task creation form: pick an admin group, optionally assign a member (default unassigned),
 * set a priority, plus the shared Details panel. No recurrence / staged / category — group tasks
 * are a flat, assignable task in the existing group system.
 */
@Composable
internal fun CreationHubGroupStep(
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

        GroupPicker(state = state, onAction = onAction)

        TaskFormDateField(date = state.date, onSelect = { onAction(UiAction.OnDateSelect(it)) })

        if (state.groupMembers.isNotEmpty()) {
            GroupTaskAssigneeSelector(
                members = state.groupMembers.map {
                    AssigneeUi(
                        userId = it.userId,
                        displayName = it.displayName,
                        avatarUrl = it.avatarUrl,
                        initials = it.initials,
                    )
                },
                selectedAssigneeId = state.selectedAssigneeId,
                onAssigneeSelected = { onAction(UiAction.OnAssigneeSelect(it)) },
            )
        }

        PrioritySelector(
            selected = state.priority,
            onSelect = { onAction(UiAction.OnPrioritySelect(it)) },
        )

        CreationHubDetailsSection(state = state, onAction = onAction)

        TDButton(
            text = stringResource(R.string.create),
            fullWidth = true,
            isEnable = !state.isSaving,
            onClick = { onAction(UiAction.OnCreate) },
        )
    }
}

@Composable
private fun GroupPicker(
    state: UiState,
    onAction: (UiAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TaskFormSectionLabel(stringResource(R.string.creation_group_label))
        if (state.adminGroups.size > 1) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.adminGroups.forEach { group ->
                    TDChoiceChip(
                        label = group.name,
                        selected = state.selectedGroupRemoteId == group.remoteId,
                        onClick = { onAction(UiAction.OnGroupSelect(group.localId, group.remoteId)) },
                    )
                }
            }
        } else {
            // Exactly one admin group — auto-selected in the VM; show it for context.
            val name = state.adminGroups.firstOrNull()?.name.orEmpty()
            TDText(
                text = name,
                style = TDTheme.typography.heading6,
                color = TDTheme.colors.onBackground,
            )
        }
    }
}

@TDPreview
@Composable
private fun CreationHubGroupStepPreview() {
    TDTheme {
        CreationHubGroupStep(
            state = UiState(
                step = Step.TASK_CORE,
                taskType = TaskType.GROUP,
                title = "Mutfağı topla",
                adminGroups = listOf(
                    GroupOption(localId = 1, remoteId = 10, name = "Ev"),
                    GroupOption(localId = 2, remoteId = 20, name = "Ofis"),
                ),
                selectedGroupLocalId = 1,
                selectedGroupRemoteId = 10,
                groupMembers = listOf(
                    AssigneeOption(userId = 1, displayName = "Berat Baran", avatarUrl = null, initials = "BB"),
                    AssigneeOption(userId = 2, displayName = "Ayşe Yılmaz", avatarUrl = null, initials = "AY"),
                ),
                selectedAssigneeId = 2,
                priority = "HIGH",
            ),
            onAction = {},
        )
    }
}
