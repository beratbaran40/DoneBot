package com.todoapp.mobile.ui.creationhub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.model.TaskType
import com.todoapp.mobile.ui.common.components.taskTypeAccent
import com.todoapp.mobile.ui.creationhub.CreationHubContract.UiAction
import com.todoapp.uikit.components.TDOptionCard
import com.todoapp.uikit.image.tdPainter
import com.example.uikit.R as UiKitR

/**
 * The task's *shape*. Scope ("mine or the group's?") is a separate question asked one step earlier —
 * "Group" used to sit here as a fifth card, which is what made a group task un-repeatable: a task
 * could only be one of these, so choosing Group meant giving up Routine, Staged and Custom.
 */
@Composable
internal fun CreationHubTypeStep(onAction: (UiAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TDOptionCard(
            title = stringResource(R.string.type_one_time_title),
            subtitle = stringResource(R.string.type_one_time_subtitle),
            icon = tdPainter(UiKitR.drawable.ic_edit_task),
            accentColor = taskTypeAccent(TaskType.ONE_TIME),
            onClick = { onAction(UiAction.OnTypeSelect(TaskType.ONE_TIME)) },
        )
        TDOptionCard(
            title = stringResource(R.string.type_routine_title),
            subtitle = stringResource(R.string.type_routine_subtitle),
            icon = tdPainter(R.drawable.ic_calendar),
            accentColor = taskTypeAccent(TaskType.ROUTINE),
            onClick = { onAction(UiAction.OnTypeSelect(TaskType.ROUTINE)) },
        )
        TDOptionCard(
            title = stringResource(R.string.type_staged_title),
            subtitle = stringResource(R.string.type_staged_subtitle),
            icon = tdPainter(R.drawable.ic_staged),
            accentColor = taskTypeAccent(TaskType.STAGED),
            onClick = { onAction(UiAction.OnTypeSelect(TaskType.STAGED)) },
        )
        TDOptionCard(
            title = stringResource(R.string.type_custom_title),
            subtitle = stringResource(R.string.type_custom_subtitle),
            icon = tdPainter(R.drawable.ic_custom),
            accentColor = taskTypeAccent(TaskType.CUSTOM),
            onClick = { onAction(UiAction.OnTypeSelect(TaskType.CUSTOM)) },
        )
    }
}
