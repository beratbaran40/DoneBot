package com.todoapp.mobile.ui.groups.groupdetail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.uikit.R
import com.todoapp.mobile.BuildConfig
import com.todoapp.mobile.common.deviceTimeFormatter
import com.todoapp.mobile.ui.common.components.AssigneeUi
import com.todoapp.mobile.ui.common.components.GroupTaskAssigneeSelector
import com.todoapp.mobile.ui.common.components.PrioritySelector
import com.todoapp.mobile.ui.common.taskform.rememberTimeFieldPlaceholder
import com.todoapp.mobile.ui.home.ExistingPhoto
import com.todoapp.mobile.ui.home.PendingPhotosRow
import com.todoapp.mobile.ui.home.TaskFormState
import com.todoapp.mobile.ui.home.TaskFormUiAction
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDButtonSize
import com.todoapp.uikit.components.TDCompactOutlinedTextField
import com.todoapp.uikit.components.TDDatePickerDialog
import com.todoapp.uikit.components.TDPickerField
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.components.TDWheelTimePickerDialog
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.theme.TDTheme

@Composable
fun GroupAddTaskSheet(
    groupName: String,
    formState: TaskFormState,
    members: List<GroupDetailContract.GroupMemberUiItem>,
    onAction: (TaskFormUiAction) -> Unit,
    submitLabel: String = stringResource(com.todoapp.mobile.R.string.create_task),
) {
    val context = LocalContext.current
    val timeFormatter = remember(context) { deviceTimeFormatter(context) }
    val startTimePlaceholder = rememberTimeFieldPlaceholder(isStart = true)
    var showStartTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = tdPainter(com.todoapp.mobile.R.drawable.ic_groups),
                    contentDescription = null,
                    tint = TDTheme.colors.pendingGray,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                TDText(
                    text = groupName,
                    style = TDTheme.typography.heading5,
                    color = TDTheme.colors.onBackground,
                )
            }
            IconButton(onClick = { onAction(TaskFormUiAction.Dismiss) }) {
                Icon(
                    tdPainter(R.drawable.ic_close),
                    tint = TDTheme.colors.onBackground,
                    contentDescription = stringResource(com.todoapp.mobile.R.string.close_button),
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = TDTheme.colors.lightGray.copy(alpha = 0.4f),
        )

        TDCompactOutlinedTextField(
            label = stringResource(com.todoapp.mobile.R.string.task_title),
            value = formState.taskTitle,
            onValueChange = { onAction(TaskFormUiAction.TitleChange(it)) },
            isError = formState.titleErrorRes != null,
            supportingText = formState.titleErrorRes?.let { stringResource(it) },
        )
        Spacer(Modifier.height(12.dp))
        TDDatePickerDialog(
            selectedDate = formState.dialogSelectedDate,
            onDateSelect = { onAction(TaskFormUiAction.DateSelect(it)) },
            onDateDeselect = { onAction(TaskFormUiAction.DateDeselect) },
            isError = formState.dateErrorRes != null,
            supportingText = formState.dateErrorRes?.let { stringResource(it) },
        )
        Spacer(Modifier.height(12.dp))
        TDPickerField(
            title = stringResource(com.todoapp.mobile.R.string.set_time),
            value =
            formState.taskTimeStart?.format(timeFormatter)
                ?: startTimePlaceholder,
            onClick = { showStartTimePicker = true },
            isError = formState.timeErrorRes != null,
            supportingText = formState.timeErrorRes?.let { stringResource(it) },
            leadingIcon = {
                Icon(
                    painter = tdPainter(R.drawable.ic_clock),
                    tint = TDTheme.colors.onBackground,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            },
        )
        Spacer(Modifier.height(12.dp))
        TDCompactOutlinedTextField(
            label = stringResource(com.todoapp.mobile.R.string.description),
            value = formState.taskDescription,
            onValueChange = { onAction(TaskFormUiAction.DescriptionChange(it)) },
            singleLine = false,
        )
        Spacer(Modifier.height(12.dp))
        val launchLocationPicker =
            com.todoapp.mobile.ui.common.rememberLocationPickerLauncher { name, address, lat, lng ->
                onAction(TaskFormUiAction.LocationPicked(name, address, lat, lng))
            }
        com.todoapp.uikit.components.TDLocationPicker(
            name = formState.locationName,
            address = formState.locationAddress,
            addLabel = stringResource(com.todoapp.mobile.R.string.location_add_hint),
            clearContentDescription = stringResource(com.todoapp.mobile.R.string.location_clear),
            onClick = launchLocationPicker,
            onClear = { onAction(TaskFormUiAction.LocationCleared) },
        )
        Spacer(Modifier.height(12.dp))
        PrioritySelector(
            selected = formState.selectedPriority,
            onSelect = { value -> onAction(TaskFormUiAction.PriorityChange(value)) },
        )
        if (members.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            GroupTaskAssigneeSelector(
                members = members.map {
                    AssigneeUi(
                        userId = it.userId,
                        displayName = it.displayName,
                        avatarUrl = it.avatarUrl,
                        initials = it.initials,
                    )
                },
                selectedAssigneeId = formState.selectedAssigneeId,
                onAssigneeSelected = { userId -> onAction(TaskFormUiAction.AssigneeChange(userId)) },
            )
        }
        Spacer(Modifier.height(12.dp))
        if (formState.existingPhotos.isNotEmpty()) {
            ExistingPhotosRow(
                photos = formState.existingPhotos,
                markedForDelete = formState.photoIdsToDelete,
                onToggle = { id -> onAction(TaskFormUiAction.ExistingPhotoToggleDelete(id)) },
            )
            Spacer(Modifier.height(12.dp))
        }
        PendingPhotosRow(
            pending = formState.pendingPhotos,
            onPick = { bytes, mime -> onAction(TaskFormUiAction.PhotoPicked(bytes, mime)) },
            onRemoveAt = { idx -> onAction(TaskFormUiAction.PhotoRemoveAt(idx)) },
        )
        Spacer(Modifier.height(12.dp))
        TDButton(
            text = submitLabel,
            onClick = { onAction(TaskFormUiAction.Create) },
            size = TDButtonSize.SMALL,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (showStartTimePicker) {
        TDWheelTimePickerDialog(
            initialTime = formState.taskTimeStart,
            onConfirm = {
                onAction(TaskFormUiAction.TimeStartChange(it))
                showStartTimePicker = false
            },
            onDismiss = { showStartTimePicker = false },
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun GroupAddTaskSheetPreview() {
    TDTheme {
        GroupAddTaskSheet(
            groupName = "The Smith Family",
            formState = TaskFormState(taskTitle = "Buy groceries"),
            members = emptyList(),
            onAction = {},
        )
    }
}

@Composable
private fun ExistingPhotosRow(
    photos: List<ExistingPhoto>,
    markedForDelete: Set<Long>,
    onToggle: (Long) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        TDText(
            text = stringResource(com.todoapp.mobile.R.string.photos),
            style = TDTheme.typography.subheading2,
            color = TDTheme.colors.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(photos, key = { it.id }) { photo ->
                val marked = photo.id in markedForDelete
                val absoluteUrl =
                    run {
                        val base =
                            BuildConfig.BASE_URL
                                .trimEnd('/')
                        "$base/${photo.url.trimStart('/')}"
                    }
                Box(
                    modifier =
                    Modifier
                        .size(72.dp)
                        .clip(
                            TDTheme.shapes.medium,
                        ).background(TDTheme.colors.lightPending)
                        .clickable { onToggle(photo.id) },
                ) {
                    AsyncImage(
                        model = absoluteUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(72.dp),
                    )
                    if (marked) {
                        Box(
                            modifier =
                            Modifier
                                .size(72.dp)
                                .background(TDTheme.colors.crossRed.copy(alpha = 0.55f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = tdPainter(R.drawable.ic_delete),
                                contentDescription = null,
                                tint = TDTheme.colors.surface,
                            )
                        }
                    }
                }
            }
        }
    }
}
