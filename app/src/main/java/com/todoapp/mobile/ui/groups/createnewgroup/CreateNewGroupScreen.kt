package com.todoapp.mobile.ui.groups.createnewgroup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.R.string.bring_your_group_together_in_one_place
import com.todoapp.mobile.R.string.e_g_the_johnsons_summer_vacation
import com.todoapp.mobile.R.string.group_description
import com.todoapp.mobile.R.string.group_name
import com.todoapp.mobile.R.string.let_s_get_started
import com.todoapp.mobile.R.string.new_group
import com.todoapp.mobile.R.string.what_is_this_group_for_collaborating_on_chores_planning_trips_or_daily_tasks
import com.todoapp.mobile.ui.groups.createnewgroup.CreateNewGroupContract.UiAction
import com.todoapp.mobile.ui.groups.createnewgroup.CreateNewGroupContract.UiState
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDInfoCard
import com.todoapp.uikit.components.TDLabeledTextField
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.theme.TDTheme

@Composable
fun CreateNewGroupScreen(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
) {
    CreateNewGroupContent(uiState, onAction)
}

@Composable
private fun CreateNewGroupContent(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
) {
    val verticalScroll = rememberScrollState()
    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .background(color = TDTheme.colors.background)
            .verticalScroll(verticalScroll)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painterResource(com.example.uikit.R.drawable.ic_avatar_new_group),
            contentDescription = stringResource(new_group),
            modifier = Modifier.size(140.dp),
            tint = TDTheme.colors.pendingGray.copy(0.81f),
        )
        TDText(text = stringResource(let_s_get_started), style = TDTheme.typography.heading2)
        TDText(text = stringResource(bring_your_group_together_in_one_place), color = TDTheme.colors.lightGray)
        Spacer(Modifier.height(40.dp))
        TDLabeledTextField(
            title = stringResource(group_name),
            isError = uiState.error != null,
            placeholder = stringResource(e_g_the_johnsons_summer_vacation),
            value = uiState.groupName,
            onValueChange = { onAction(UiAction.OnGroupNameChange(it)) },
        )
        uiState.error?.let {
            Spacer(Modifier.height(4.dp))
            TDText(text = it, color = TDTheme.colors.red, modifier = Modifier.align(Alignment.Start))
        }
        Spacer(Modifier.height(24.dp))
        TDLabeledTextField(
            title = stringResource(group_description),
            placeholder = stringResource(what_is_this_group_for_collaborating_on_chores_planning_trips_or_daily_tasks),
            value = uiState.groupDescription.orEmpty(),
            onValueChange = { onAction(UiAction.OnGroupDescriptionChange(it)) },
            minLines = 5,
        )
        Spacer(Modifier.height(24.dp))
        TDButton(
            text = stringResource(R.string.create),
            fullWidth = true,
            onClick = { onAction(UiAction.OnCreateTap) },
        )
        Spacer(Modifier.height(24.dp))
        TDInfoCard(
            text =
            stringResource(
                R.string.text_field_description,
            ),
            modifier = Modifier.imePadding(),
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun CreateNewGroupScreenPreview() {
    TDTheme {
        CreateNewGroupScreen(
            uiState =
            UiState(
                groupName = "YTU Family",
                groupDescription = "Weekend chores and shared tasks",
                error = null,
                isUserAuthenticated = true,
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun CreateNewGroupScreenPreview2() {
    TDTheme {
        CreateNewGroupScreen(
            uiState =
            UiState(
                groupName = "",
                groupDescription = "",
                error = "Group name is required.",
                isUserAuthenticated = true,
            ),
            onAction = {},
        )
    }
}
