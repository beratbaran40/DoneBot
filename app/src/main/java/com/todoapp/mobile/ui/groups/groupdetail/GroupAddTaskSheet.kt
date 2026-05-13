package com.todoapp.mobile.ui.groups.groupdetail

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.uikit.R
import com.todoapp.mobile.BuildConfig
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
import com.todoapp.uikit.theme.TDTheme
import java.time.format.DateTimeFormatter

@Composable
fun GroupAddTaskSheet(
    groupName: String,
    formState: TaskFormState,
    members: List<GroupDetailContract.GroupMemberUiItem>,
    onAction: (TaskFormUiAction) -> Unit,
    submitLabel: String = stringResource(com.todoapp.mobile.R.string.create_task),
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
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
                    painter = painterResource(com.todoapp.mobile.R.drawable.ic_groups),
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
                    painterResource(R.drawable.ic_close),
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
                ?: stringResource(com.todoapp.mobile.R.string.starts),
            onClick = { showStartTimePicker = true },
            isError = formState.timeErrorRes != null,
            supportingText = formState.timeErrorRes?.let { stringResource(it) },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_clock),
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
                members = members,
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
private fun GroupTaskAssigneeSelector(
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
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(members, key = { it.userId }) { member ->
                val isSelected = member.userId == selectedAssigneeId
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier =
                    Modifier
                        .clickable {
                            onAssigneeSelected(if (isSelected) null else member.userId)
                        }.padding(4.dp),
                ) {
                    val absoluteAvatarUrl =
                        member.avatarUrl?.takeIf { it.isNotBlank() }?.let {
                            val base =
                                BuildConfig.BASE_URL
                                    .trimEnd('/')
                            "$base/${it.trimStart('/')}"
                        }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) {
                                    TDTheme.colors.pendingGray
                                } else {
                                    TDTheme.colors.lightPending
                                },
                            ).then(
                                if (isSelected) {
                                    Modifier.border(2.dp, TDTheme.colors.pendingGray, CircleShape)
                                } else {
                                    Modifier
                                },
                            ),
                    ) {
                        if (absoluteAvatarUrl != null) {
                            AsyncImage(
                                model = absoluteAvatarUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(40.dp).clip(CircleShape),
                            )
                        } else {
                            TDText(
                                text = member.initials,
                                style = TDTheme.typography.subheading2,
                                color = if (isSelected) TDTheme.colors.surface else TDTheme.colors.pendingGray,
                                textAlign = TextAlign.Center,
                            )
                        }
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
                            RoundedCornerShape(12.dp),
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
                                painter = painterResource(R.drawable.ic_delete),
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

@Composable
private fun PrioritySelector(
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    val options: List<Pair<String?, String>> =
        listOf(
            null to stringResource(com.todoapp.mobile.R.string.priority_none),
            "LOW" to "LOW",
            "MEDIUM" to "MED",
            "HIGH" to "HIGH",
        )
    Column(Modifier.fillMaxWidth()) {
        TDText(
            text = stringResource(com.todoapp.mobile.R.string.priority),
            style = TDTheme.typography.subheading2,
            color = TDTheme.colors.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, label) ->
                PriorityChip(
                    value = value,
                    label = label,
                    isSelected = selected == value,
                    onClick = { onSelect(value) },
                )
            }
        }
    }
}

@Composable
private fun PriorityChip(
    value: String?,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val (bg, fg) =
        when (value?.uppercase()) {
            "HIGH" -> TDTheme.colors.lightRed to TDTheme.colors.crossRed
            "MEDIUM" -> TDTheme.colors.lightOrange to TDTheme.colors.orange
            "LOW" -> TDTheme.colors.lightPending to TDTheme.colors.darkPending
            else -> TDTheme.colors.lightPending to TDTheme.colors.pendingGray
        }
    val containerBg = if (isSelected) bg else bg.copy(alpha = 0.35f)
    val contentColor = if (isSelected) fg else fg.copy(alpha = 0.6f)
    Box(
        modifier =
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(containerBg)
            .then(
                if (isSelected) Modifier.border(2.dp, TDTheme.colors.pendingGray, RoundedCornerShape(8.dp))
                else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        TDText(
            text = label,
            style = TDTheme.typography.subheading1,
            color = contentColor,
        )
    }
}
