package com.todoapp.mobile.ui.groups.grouptaskdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.mobile.ui.groups.groupdetail.GroupDetailContract
import com.todoapp.mobile.ui.groups.grouptaskdetail.GroupTaskDetailContract.UiAction
import com.todoapp.mobile.ui.groups.grouptaskdetail.GroupTaskDetailContract.UiState
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDButtonSize
import com.todoapp.uikit.components.TDCompactOutlinedTextField
import com.todoapp.uikit.components.TDDatePickerDialog
import com.todoapp.uikit.components.TDPickerField
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.components.TDWheelTimePickerDialog
import com.todoapp.uikit.theme.TDTheme
import java.time.format.DateTimeFormatter

@Composable
fun GroupTaskEditSheet(
    state: UiState.Success,
    onAction: (UiAction) -> Unit,
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        TDText(
            text = stringResource(com.todoapp.mobile.R.string.edit_task),
            style = TDTheme.typography.heading4,
            color = TDTheme.colors.onBackground,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        IconButton(
            onClick = { onAction(UiAction.OnEditDismiss) },
            modifier = Modifier.align(Alignment.End),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(com.todoapp.mobile.R.string.cd_close),
                tint = TDTheme.colors.onBackground,
                modifier = Modifier.size(20.dp),
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = TDTheme.colors.lightGray.copy(alpha = 0.4f),
        )

        TDCompactOutlinedTextField(
            label = stringResource(com.todoapp.mobile.R.string.task_title),
            value = state.editTitle,
            onValueChange = { onAction(UiAction.OnEditTitleChange(it)) },
        )
        Spacer(Modifier.height(12.dp))
        TDDatePickerDialog(
            selectedDate = state.editDate,
            onDateSelect = { onAction(UiAction.OnEditDateSelect(it)) },
            onDateDeselect = { onAction(UiAction.OnEditDateDeselect) },
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_clock),
                contentDescription = null,
                tint = TDTheme.colors.onBackground,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(8.dp))
            TDText(
                text = stringResource(com.todoapp.mobile.R.string.task_all_day_label),
                style = TDTheme.typography.regularTextStyle,
                color = TDTheme.colors.onBackground,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = state.editIsAllDay,
                onCheckedChange = { onAction(UiAction.OnEditAllDayChange(it)) },
                colors =
                SwitchDefaults.colors(
                    checkedThumbColor = TDTheme.colors.purple,
                    checkedTrackColor = TDTheme.colors.lightPurple,
                ),
            )
        }
        if (!state.editIsAllDay) {
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    TDPickerField(
                        title = stringResource(com.todoapp.mobile.R.string.set_time),
                        value = state.editTimeStart?.format(timeFormatter)
                            ?: stringResource(com.todoapp.mobile.R.string.starts),
                        onClick = { showStartPicker = true },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_clock),
                                contentDescription = null,
                                tint = TDTheme.colors.onBackground,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    TDPickerField(
                        title = "",
                        value = state.editTimeEnd?.format(timeFormatter)
                            ?: stringResource(com.todoapp.mobile.R.string.ends),
                        onClick = { showEndPicker = true },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_clock),
                                contentDescription = null,
                                tint = TDTheme.colors.onBackground,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        TDCompactOutlinedTextField(
            label = stringResource(com.todoapp.mobile.R.string.description),
            value = state.editDescription,
            onValueChange = { onAction(UiAction.OnEditDescriptionChange(it)) },
            singleLine = false,
        )
        Spacer(Modifier.height(12.dp))
        val launchLocationPicker =
            com.todoapp.mobile.ui.common.rememberLocationPickerLauncher { name, address, lat, lng ->
                onAction(UiAction.OnEditLocationPicked(name, address, lat, lng))
            }
        com.todoapp.uikit.components.TDLocationPicker(
            name = state.editLocationName,
            address = state.editLocationAddress,
            addLabel = stringResource(com.todoapp.mobile.R.string.location_add_hint),
            clearContentDescription = stringResource(com.todoapp.mobile.R.string.location_clear),
            onClick = launchLocationPicker,
            onClear = { onAction(UiAction.OnEditLocationCleared) },
        )
        if (state.members.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            EditTaskAssigneeSelector(
                members = state.members,
                selectedAssigneeId = state.editAssigneeId,
                onAssigneeSelected = { onAction(UiAction.OnEditAssigneeChange(it)) },
            )
        }
        Spacer(Modifier.height(16.dp))
        TDButton(
            text = stringResource(com.todoapp.mobile.R.string.save_changes),
            onClick = { onAction(UiAction.OnEditSave) },
            size = TDButtonSize.SMALL,
            isEnable = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (showStartPicker) {
        TDWheelTimePickerDialog(
            initialTime = state.editTimeStart,
            onConfirm = {
                onAction(UiAction.OnEditTimeStartChange(it))
                showStartPicker = false
            },
            onDismiss = { showStartPicker = false },
        )
    }
    if (showEndPicker) {
        TDWheelTimePickerDialog(
            initialTime = state.editTimeEnd,
            onConfirm = {
                onAction(UiAction.OnEditTimeEndChange(it))
                showEndPicker = false
            },
            onDismiss = { showEndPicker = false },
        )
    }
}

@Composable
private fun EditTaskAssigneeSelector(
    members: List<GroupDetailContract.GroupMemberUiItem>,
    selectedAssigneeId: Long?,
    onAssigneeSelected: (Long?) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TDText(
            text = stringResource(com.todoapp.mobile.R.string.assign_to),
            style = TDTheme.typography.heading3,
            color = TDTheme.colors.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(vertical = 8.dp),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(members, key = { it.userId }) { member ->
                val isSelected = member.userId == selectedAssigneeId
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier =
                    Modifier
                        .clickable { onAssigneeSelected(if (isSelected) null else member.userId) }
                        .padding(4.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) TDTheme.colors.pendingGray else TDTheme.colors.lightPending)
                            .then(
                                if (isSelected) {
                                    Modifier.border(2.dp, TDTheme.colors.pendingGray, CircleShape)
                                } else {
                                    Modifier
                                },
                            ),
                    ) {
                        TDText(
                            text = member.initials,
                            style = TDTheme.typography.subheading2,
                            color = if (isSelected) TDTheme.colors.surface else TDTheme.colors.pendingGray,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    TDText(
                        text = member.displayName.split(" ").firstOrNull() ?: member.displayName,
                        style = TDTheme.typography.subheading4,
                        color = if (isSelected) TDTheme.colors.pendingGray else TDTheme.colors.gray,
                    )
                }
            }
        }
    }
}
