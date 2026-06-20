package com.todoapp.mobile.ui.creationhub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.creationhub.CreationHubContract.TaskType
import com.todoapp.mobile.ui.creationhub.CreationHubContract.UiAction
import com.todoapp.uikit.components.TDOptionCard
import com.todoapp.uikit.theme.TDTheme
import com.example.uikit.R as UiKitR

@Composable
internal fun CreationHubTypeStep(onAction: (UiAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TDOptionCard(
            title = stringResource(R.string.type_one_time_title),
            subtitle = stringResource(R.string.type_one_time_subtitle),
            icon = painterResource(UiKitR.drawable.ic_edit_task),
            accentColor = TDTheme.colors.darkPending,
            onClick = { onAction(UiAction.OnTypeSelect(TaskType.ONE_TIME)) },
        )
        TDOptionCard(
            title = stringResource(R.string.type_routine_title),
            subtitle = stringResource(R.string.type_routine_subtitle),
            icon = painterResource(R.drawable.ic_calendar),
            accentColor = TDTheme.colors.purple,
            onClick = { onAction(UiAction.OnTypeSelect(TaskType.ROUTINE)) },
        )
        TDOptionCard(
            title = stringResource(R.string.type_staged_title),
            subtitle = stringResource(R.string.type_staged_subtitle),
            icon = painterResource(R.drawable.ic_staged),
            accentColor = TDTheme.colors.mediumGreen,
            onClick = { onAction(UiAction.OnTypeSelect(TaskType.STAGED)) },
        )
    }
}
