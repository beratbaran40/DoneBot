package com.todoapp.mobile.ui.creationhub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.common.components.taskTypeAccent
import com.todoapp.mobile.ui.common.taskform.TaskFormType
import com.todoapp.mobile.ui.creationhub.CreationHubContract.TaskType
import com.todoapp.mobile.ui.creationhub.CreationHubContract.UiAction
import com.todoapp.uikit.components.TDOptionCard
import com.todoapp.uikit.theme.TDTheme
import com.example.uikit.R as UiKitR

@Composable
internal fun CreationHubTypeStep(
    showGroupCard: Boolean,
    onAction: (UiAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TDOptionCard(
            title = stringResource(R.string.type_one_time_title),
            subtitle = stringResource(R.string.type_one_time_subtitle),
            icon = painterResource(UiKitR.drawable.ic_edit_task),
            accentColor = taskTypeAccent(TaskFormType.ONE_TIME),
            onClick = { onAction(UiAction.OnTypeSelect(TaskType.ONE_TIME)) },
        )
        TDOptionCard(
            title = stringResource(R.string.type_routine_title),
            subtitle = stringResource(R.string.type_routine_subtitle),
            icon = painterResource(R.drawable.ic_calendar),
            accentColor = taskTypeAccent(TaskFormType.ROUTINE),
            onClick = { onAction(UiAction.OnTypeSelect(TaskType.ROUTINE)) },
        )
        TDOptionCard(
            title = stringResource(R.string.type_staged_title),
            subtitle = stringResource(R.string.type_staged_subtitle),
            icon = painterResource(R.drawable.ic_staged),
            accentColor = taskTypeAccent(TaskFormType.STAGED),
            onClick = { onAction(UiAction.OnTypeSelect(TaskType.STAGED)) },
        )
        TDOptionCard(
            title = stringResource(R.string.type_custom_title),
            subtitle = stringResource(R.string.type_custom_subtitle),
            icon = painterResource(R.drawable.ic_custom),
            accentColor = taskTypeAccent(TaskFormType.CUSTOM),
            onClick = { onAction(UiAction.OnTypeSelect(TaskType.CUSTOM)) },
        )
        // Only shown when the user administers at least one group.
        if (showGroupCard) {
            TDOptionCard(
                title = stringResource(R.string.type_group_title),
                subtitle = stringResource(R.string.type_group_subtitle),
                icon = painterResource(R.drawable.ic_groups),
                accentColor = TDTheme.colors.darkPurple,
                onClick = { onAction(UiAction.OnTypeSelect(TaskType.GROUP)) },
            )
        }
    }
}
