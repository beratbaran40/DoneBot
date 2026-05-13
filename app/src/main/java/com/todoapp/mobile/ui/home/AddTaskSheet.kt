package com.todoapp.mobile.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.mobile.domain.constants.DailyPlanDefaults
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.TaskCategory
import com.todoapp.mobile.ui.common.categoryOptions
import com.todoapp.mobile.ui.common.recurrenceExplainer
import com.todoapp.mobile.ui.common.recurrenceOptions
import com.todoapp.mobile.ui.common.reminderOffsetOptions
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDButtonSize
import com.todoapp.uikit.components.TDCategoryPicker
import com.todoapp.uikit.components.TDCompactOutlinedTextField
import com.todoapp.uikit.components.TDDatePickerDialog
import com.todoapp.uikit.components.TDLocationPicker
import com.todoapp.uikit.components.TDPickerField
import com.todoapp.uikit.components.TDRecurrencePicker
import com.todoapp.uikit.components.TDReminderOffsetPicker
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.components.TDWheelTimePickerDialog
import com.todoapp.uikit.theme.TDTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
internal fun AddTaskSheet(
    formState: TaskFormState,
    onAction: (TaskFormUiAction) -> Unit,
    availableGroups: List<HomeContract.GroupSelectionItem> = emptyList(),
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

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
            TDText(
                text = stringResource(com.todoapp.mobile.R.string.add_new_task),
                color = TDTheme.colors.onBackground,
            )
            IconButton(
                onClick = { onAction(TaskFormUiAction.Dismiss) },
            ) {
                Icon(
                    painterResource(R.drawable.ic_close),
                    tint = TDTheme.colors.onBackground,
                    contentDescription = stringResource(com.todoapp.mobile.R.string.close_button),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_clock),
                tint = TDTheme.colors.onBackground,
                contentDescription = null,
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
                checked = formState.isAllDay,
                onCheckedChange = { onAction(TaskFormUiAction.AllDayChange(it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TDTheme.colors.purple,
                    checkedTrackColor = TDTheme.colors.lightPurple,
                ),
            )
        }
        if (!formState.isAllDay) {
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
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
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    TDPickerField(
                        title = "",
                        value =
                        formState.taskTimeEnd?.format(timeFormatter)
                            ?: stringResource(com.todoapp.mobile.R.string.ends),
                        onClick = { showEndTimePicker = true },
                        isError = formState.timeErrorRes != null,
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_clock),
                                tint = TDTheme.colors.onBackground,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        TDText(
            text = stringResource(com.todoapp.mobile.R.string.reminder_label),
            style = TDTheme.typography.heading6,
            color = TDTheme.colors.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        TDReminderOffsetPicker(
            selectedMinutes = formState.reminderOffsetMinutes,
            options = reminderOffsetOptions(),
            onSelected = { onAction(TaskFormUiAction.ReminderOffsetChange(it)) },
        )
        if (isReminderInPast(formState)) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TDTheme.colors.background, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_warning),
                    contentDescription = null,
                    tint = TDTheme.colors.orange,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                TDText(
                    text = stringResource(com.todoapp.mobile.R.string.reminder_in_past_warning),
                    style = TDTheme.typography.subheading1,
                    color = TDTheme.colors.orange,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        TDCompactOutlinedTextField(
            label = stringResource(com.todoapp.mobile.R.string.description),
            value = formState.taskDescription,
            onValueChange = { onAction(TaskFormUiAction.DescriptionChange(it)) },
            singleLine = false,
        )
        Spacer(Modifier.height(12.dp))
        val launchLocationPicker = com.todoapp.mobile.ui.common.rememberLocationPickerLauncher { name, address, lat, lng ->
            onAction(TaskFormUiAction.LocationPicked(name, address, lat, lng))
        }
        TDLocationPicker(
            name = formState.locationName,
            address = formState.locationAddress,
            addLabel = stringResource(com.todoapp.mobile.R.string.location_add_hint),
            clearContentDescription = stringResource(com.todoapp.mobile.R.string.location_clear),
            onClick = launchLocationPicker,
            onClear = { onAction(TaskFormUiAction.LocationCleared) },
        )
        Spacer(Modifier.height(12.dp))
        DetailsSection(
            isExpanded = formState.isAdvancedSettingsExpanded,
            selectedCategory = formState.selectedCategory,
            customCategoryName = formState.customCategoryName,
            selectedRecurrence = formState.selectedRecurrence,
            pendingPhotos = formState.pendingPhotos,
            isSecret = formState.isTaskSecret,
            availableGroups = availableGroups,
            selectedGroupId = formState.selectedGroupId,
            onToggleExpanded = { onAction(TaskFormUiAction.ToggleAdvancedSettings) },
            onCategoryChange = { onAction(TaskFormUiAction.CategoryChange(it)) },
            onCustomCategoryNameChange = { onAction(TaskFormUiAction.CustomCategoryNameChange(it)) },
            onRecurrenceChange = { onAction(TaskFormUiAction.RecurrenceChange(it)) },
            onPhotoPicked = { bytes, mime -> onAction(TaskFormUiAction.PhotoPicked(bytes, mime)) },
            onPhotoRemoveAt = { onAction(TaskFormUiAction.PhotoRemoveAt(it)) },
            onSecretChange = { onAction(TaskFormUiAction.SecretChange(it)) },
            onGroupSelected = { groupId ->
                onAction(
                    TaskFormUiAction.GroupSelectionChanged(
                        if (formState.selectedGroupId == groupId) null else groupId,
                    ),
                )
            },
        )
        Spacer(Modifier.height(12.dp))
        TDButton(
            text = stringResource(com.todoapp.mobile.R.string.create_task),
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

    if (showEndTimePicker) {
        TDWheelTimePickerDialog(
            initialTime = formState.taskTimeEnd,
            onConfirm = {
                onAction(TaskFormUiAction.TimeEndChange(it))
                showEndTimePicker = false
            },
            onDismiss = { showEndTimePicker = false },
        )
    }
}

@Composable
private fun GroupAssignmentSection(
    availableGroups: List<HomeContract.GroupSelectionItem>,
    selectedGroupId: Long?,
    onGroupSelected: (Long) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TDText(
            text = stringResource(com.todoapp.mobile.R.string.assign_to_a_group),
            style = TDTheme.typography.subheading2,
            color = TDTheme.colors.gray,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        availableGroups.forEach { group ->
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { onGroupSelected(group.groupId) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(com.todoapp.mobile.R.drawable.ic_groups),
                    contentDescription = null,
                    tint = TDTheme.colors.pendingGray,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(12.dp))
                TDText(
                    text = group.name,
                    style = TDTheme.typography.subheading3,
                    color = TDTheme.colors.onBackground,
                    modifier = Modifier.weight(1f),
                )
                Checkbox(
                    checked = selectedGroupId == group.groupId,
                    onCheckedChange = { onGroupSelected(group.groupId) },
                    colors =
                    CheckboxDefaults.colors(
                        checkedColor = TDTheme.colors.pendingGray,
                        uncheckedColor = TDTheme.colors.onBackground.copy(alpha = 0.5f),
                    ),
                )
            }
        }
    }
}

@Composable
private fun DetailsSection(
    isExpanded: Boolean,
    selectedCategory: TaskCategory,
    customCategoryName: String,
    selectedRecurrence: Recurrence,
    pendingPhotos: List<PendingPhoto>,
    isSecret: Boolean,
    availableGroups: List<HomeContract.GroupSelectionItem>,
    selectedGroupId: Long?,
    onToggleExpanded: () -> Unit,
    onCategoryChange: (TaskCategory) -> Unit,
    onCustomCategoryNameChange: (String) -> Unit,
    onRecurrenceChange: (Recurrence) -> Unit,
    onPhotoPicked: (ByteArray, String) -> Unit,
    onPhotoRemoveAt: (Int) -> Unit,
    onSecretChange: (Boolean) -> Unit,
    onGroupSelected: (Long) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onToggleExpanded() }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TDText(
                text = stringResource(com.todoapp.mobile.R.string.details_label),
                style = TDTheme.typography.heading3,
                color = TDTheme.colors.onBackground.copy(alpha = 0.7f),
            )
            Icon(
                painter =
                painterResource(
                    if (isExpanded) {
                        R.drawable.ic_outline_expand_circle_down_24
                    } else {
                        R.drawable.ic_outline_expand_circle_right_24
                    },
                ),
                contentDescription = null,
                tint = TDTheme.colors.onBackground.copy(alpha = 0.7f),
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.fillMaxWidth()) {
                TDText(
                    text = stringResource(com.todoapp.mobile.R.string.category_label),
                    style = TDTheme.typography.heading6,
                    color = TDTheme.colors.onBackground,
                )
                Spacer(Modifier.height(8.dp))
                TDCategoryPicker(
                    selectedKey = selectedCategory.name,
                    options = categoryOptions(),
                    onSelected = { key -> onCategoryChange(TaskCategory.valueOf(key)) },
                )
                if (selectedCategory == TaskCategory.OTHER) {
                    Spacer(Modifier.height(8.dp))
                    TDCompactOutlinedTextField(
                        label = stringResource(com.todoapp.mobile.R.string.category_other_hint),
                        value = customCategoryName,
                        onValueChange = onCustomCategoryNameChange,
                    )
                }
                Spacer(Modifier.height(12.dp))
                TDText(
                    text = stringResource(com.todoapp.mobile.R.string.recurrence_label),
                    style = TDTheme.typography.heading6,
                    color = TDTheme.colors.onBackground,
                )
                Spacer(Modifier.height(8.dp))
                TDRecurrencePicker(
                    selectedKey = selectedRecurrence.name,
                    options = recurrenceOptions(),
                    onSelected = { key -> onRecurrenceChange(Recurrence.valueOf(key)) },
                )
                if (selectedRecurrence != Recurrence.NONE) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TDTheme.colors.background, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_warning),
                            contentDescription = null,
                            tint = TDTheme.colors.orange,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        TDText(
                            text = recurrenceExplainer(selectedRecurrence),
                            style = TDTheme.typography.subheading1,
                            color = TDTheme.colors.orange,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                PendingPhotosRow(
                    pending = pendingPhotos,
                    onPick = onPhotoPicked,
                    onRemoveAt = onPhotoRemoveAt,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SecretCheckbox(
                        checked = isSecret,
                        onCheckedChange = onSecretChange,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    TDText(
                        text = stringResource(com.todoapp.mobile.R.string.secret_task),
                        style = TDTheme.typography.heading6,
                        color = TDTheme.colors.onBackground,
                    )
                }
                if (availableGroups.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    GroupAssignmentSection(
                        availableGroups = availableGroups,
                        selectedGroupId = selectedGroupId,
                        onGroupSelected = onGroupSelected,
                    )
                }
            }
        }
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun AddTaskSheetPreview() {
    TDTheme {
        AddTaskSheet(
            formState = TaskFormState(),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun AddTaskSheetPreview_Dark() {
    TDTheme(darkTheme = true) {
        AddTaskSheet(
            formState = TaskFormState(),
            onAction = {},
        )
    }
}

@Composable
private fun SecretCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(4.dp)
    val bgColor by animateColorAsState(
        targetValue = if (checked) TDTheme.colors.pendingGray else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "secretCheckboxBg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) {
            TDTheme.colors.pendingGray
        } else {
            TDTheme.colors.onBackground.copy(alpha = 0.6f)
        },
        animationSpec = tween(durationMillis = 200),
        label = "secretCheckboxBorder",
    )

    val iconScale = remember { Animatable(if (checked) 1f else 0f) }
    val iconAlpha = remember { Animatable(if (checked) 1f else 0f) }
    var prevChecked by remember { mutableStateOf(checked) }

    LaunchedEffect(checked) {
        if (checked && !prevChecked) {
            iconAlpha.snapTo(0f)
            iconScale.snapTo(0.4f)
            iconAlpha.animateTo(1f, animationSpec = tween(durationMillis = 150))
            iconScale.animateTo(
                targetValue = 1.4f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
            iconScale.animateTo(1f, animationSpec = tween(durationMillis = 150))
        } else if (!checked && prevChecked) {
            iconAlpha.animateTo(0f, animationSpec = tween(durationMillis = 120))
            iconScale.snapTo(0f)
        }
        prevChecked = checked
    }

    Box(
        modifier = modifier
            .size(24.dp)
            .clip(shape)
            .background(bgColor, shape)
            .border(width = 2.dp, color = borderColor, shape = shape)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(com.todoapp.mobile.R.drawable.ic_secret_mode),
            contentDescription = null,
            colorFilter = ColorFilter.tint(TDTheme.colors.white),
            modifier = Modifier
                .size(16.dp)
                .scale(iconScale.value)
                .alpha(iconAlpha.value),
        )
    }
}

/**
 * Mirrors DetailsViewModel.computeIsReminderInPast — UI-level guard so the user sees a warning
 * the moment they pick a past time, without needing a round-trip through TaskFormState.
 */
private fun isReminderInPast(formState: TaskFormState): Boolean {
    if (formState.selectedRecurrence != Recurrence.NONE) return false
    val offset = formState.reminderOffsetMinutes ?: return false
    val date = formState.dialogSelectedDate ?: return false
    val effectiveTime = when {
        formState.isAllDay -> DailyPlanDefaults.DEFAULT_PLAN_TIME
        else -> formState.taskTimeStart ?: return false
    }
    val reminderTime = LocalDateTime.of(date, effectiveTime.minusMinutes(offset))
    return reminderTime.isBefore(LocalDateTime.now())
}
