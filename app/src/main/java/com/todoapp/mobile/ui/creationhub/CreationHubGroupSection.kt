package com.todoapp.mobile.ui.creationhub

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.common.components.AssigneeUi
import com.todoapp.mobile.ui.common.components.GroupTaskAssigneeSelector
import com.todoapp.mobile.ui.common.components.PrioritySelector
import com.todoapp.mobile.ui.common.taskform.TaskFormSectionLabel
import com.todoapp.mobile.ui.creationhub.CreationHubContract.AssigneeOption
import com.todoapp.mobile.ui.creationhub.CreationHubContract.GroupOption
import com.todoapp.mobile.ui.creationhub.CreationHubContract.Step
import com.todoapp.mobile.ui.creationhub.CreationHubContract.TaskScope
import com.todoapp.mobile.ui.creationhub.CreationHubContract.TaskType
import com.todoapp.mobile.ui.creationhub.CreationHubContract.UiAction
import com.todoapp.mobile.ui.creationhub.CreationHubContract.UiState
import com.todoapp.uikit.components.TDChoiceChip
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

/**
 * The group-only part of the creation form: which group, who it is assigned to, and how urgent.
 *
 * Extracted from the old `CreationHubGroupStep`, which was a whole parallel form. That fork is what
 * kept group tasks flat — it simply had no recurrence, steps or reminder sections. Now it is a
 * section inside the one shared form, so a group task gets every capability for free.
 */
@Composable
internal fun CreationHubGroupSection(
    state: UiState,
    onAction: (UiAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        GroupPicker(state = state, onAction = onAction)

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
private fun CreationHubGroupSectionPreview() {
    TDTheme {
        CreationHubGroupSection(
            state = UiState(
                step = Step.TASK_CORE,
                scope = TaskScope.GROUP,
                taskType = TaskType.CUSTOM,
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

@TDPreview
@Composable
private fun CreationHubGroupSectionSingleGroupPreview() {
    TDTheme {
        CreationHubGroupSection(
            state = UiState(
                step = Step.TASK_CORE,
                scope = TaskScope.GROUP,
                taskType = TaskType.ROUTINE,
                adminGroups = listOf(GroupOption(localId = 1, remoteId = 10, name = "Ev")),
                selectedGroupLocalId = 1,
                selectedGroupRemoteId = 10,
            ),
            onAction = {},
        )
    }
}
