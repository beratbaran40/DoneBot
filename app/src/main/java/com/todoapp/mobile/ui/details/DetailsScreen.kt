package com.todoapp.mobile.ui.details

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.todoapp.mobile.BuildConfig
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.TaskCategory
import com.todoapp.mobile.ui.common.categoryOptions
import com.todoapp.mobile.ui.common.components.TaskPhotoBannerEditable
import com.todoapp.mobile.ui.common.components.TaskTypeBadge
import com.todoapp.mobile.ui.common.taskform.TaskFormType
import com.todoapp.mobile.ui.common.taskform.TaskFrequencyChips
import com.todoapp.mobile.ui.common.taskform.TaskReminderChips
import com.todoapp.mobile.ui.common.taskform.TaskTypeHeader
import com.todoapp.mobile.ui.details.DetailsContract.UiAction
import com.todoapp.mobile.ui.details.DetailsContract.UiEffect
import com.todoapp.mobile.ui.details.DetailsContract.UiState
import com.todoapp.mobile.ui.groups.grouptaskdetail.TaskPhotosSection
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDButtonSize
import com.todoapp.uikit.components.TDButtonType
import com.todoapp.uikit.components.TDCategoryPicker
import com.todoapp.uikit.components.TDCompactOutlinedTextField
import com.todoapp.uikit.components.TDDatePickerDialog
import com.todoapp.uikit.components.TDPickerField
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.components.TDWheelTimePicker
import com.todoapp.uikit.extensions.collectWithLifecycle
import com.todoapp.uikit.theme.TDTheme
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun DetailsScreen(
    uiState: UiState,
    uiEffect: Flow<UiEffect>,
    onAction: (UiAction) -> Unit,
) {
    val context = LocalContext.current

    uiEffect.collectWithLifecycle {
        when (it) {
            is UiEffect.ShowToast -> {
                Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    androidx.activity.compose.BackHandler {
        onAction(UiAction.OnBackClick)
    }

    Box(
        modifier =
        Modifier
            .fillMaxSize()
            .background(TDTheme.colors.background),
    ) {
        when (uiState) {
            is UiState.Loading -> DetailsSkeleton()
            is UiState.Error -> DetailsErrorContent(uiState.message, onAction)
            is UiState.Success -> {
                DetailsSuccessContent(uiState, onAction)
                if (uiState.showDiscardDialog) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { onAction(UiAction.OnDismissDiscardDialog) },
                        title = {
                            TDText(
                                text = stringResource(R.string.details_discard_title),
                                style = TDTheme.typography.heading4,
                            )
                        },
                        text = {
                            TDText(
                                text = stringResource(R.string.details_discard_message),
                                style = TDTheme.typography.regularTextStyle,
                            )
                        },
                        confirmButton = {
                            TDButton(
                                text = stringResource(R.string.details_discard_confirm),
                                onClick = { onAction(UiAction.OnConfirmDiscard) },
                                size = TDButtonSize.SMALL,
                                type = TDButtonType.PRIMARY,
                            )
                        },
                        dismissButton = {
                            TDButton(
                                text = stringResource(R.string.cancel),
                                onClick = { onAction(UiAction.OnDismissDiscardDialog) },
                                size = TDButtonSize.SMALL,
                                type = TDButtonType.CANCEL,
                            )
                        },
                        containerColor = TDTheme.colors.surface,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailsErrorContent(
    message: String,
    onAction: (UiAction) -> Unit,
) {
    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(com.example.uikit.R.drawable.ic_error),
            contentDescription = null,
            tint = TDTheme.colors.crossRed,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(16.dp))
        TDText(
            text = message,
            style = TDTheme.typography.heading3,
            color = TDTheme.colors.onSurface,
        )
        Spacer(Modifier.height(24.dp))
        TDButton(
            text = stringResource(R.string.retry),
            onClick = { onAction(UiAction.OnRetry) },
            size = TDButtonSize.SMALL,
        )
    }
}

@Composable
private fun DetailsSuccessContent(
    uiState: UiState.Success,
    onAction: (UiAction) -> Unit,
) {
    val verticalScroll = rememberScrollState()
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        Column(
            modifier =
            Modifier
                .weight(1f)
                .verticalScroll(verticalScroll),
        ) {
            // Hero banner = cover photo (newest pending upload, else first saved photo). Full-bleed.
            val bannerBytes = uiState.pendingPhotoUploads.lastOrNull()?.bytes
            val bannerUrl = uiState.photoUrls.firstOrNull()?.let { detailsAbsoluteUrl(it) }
            val bannerModel: Any? = bannerBytes ?: bannerUrl
            if (bannerModel != null) {
                TaskPhotoBannerEditable(
                    displayModel = bannerModel,
                    onCropped = { bytes -> replaceBannerPhoto(uiState, onAction, bytes) },
                    onRemove = { removeBannerPhoto(uiState, onAction) },
                    badge = { TaskTypeBadge(uiState.taskType) },
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Spacer(Modifier.height(8.dp))

                Column(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Type now shows as the banner's corner badge when a banner is present.
                    if (bannerModel == null) {
                        DetailsTypeHeader(uiState.taskType)
                    }

                    TDCompactOutlinedTextField(
                        label = stringResource(R.string.task_title),
                        value = uiState.taskTitle,
                        onValueChange = { onAction(UiAction.OnTaskTitleEdit(it)) },
                        isError = uiState.titleError != null,
                        supportingText = uiState.titleError?.let { stringResource(it) },
                    )

                    // Routine surfaces frequency right after the title (before the start date).
                    if (uiState.taskType == TaskFormType.ROUTINE) {
                        TaskFrequencyChips(
                            selected = uiState.selectedRecurrence,
                            onSelect = { onAction(UiAction.OnRecurrenceChange(it)) },
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TDText(
                            text = stringResource(R.string.task_date),
                            style = TDTheme.typography.heading6,
                            color = TDTheme.colors.onSurface,
                        )
                        TDDatePickerDialog(
                            selectedDate = uiState.dialogSelectedDate,
                            onDateSelect = { onAction(UiAction.OnDialogDateSelect(it)) },
                            onDateDeselect = { onAction(UiAction.OnDialogDateDeselect) },
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(com.example.uikit.R.drawable.ic_clock),
                            tint = TDTheme.colors.onBackground,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        TDText(
                            text = stringResource(R.string.task_all_day_label),
                            style = TDTheme.typography.regularTextStyle,
                            color = TDTheme.colors.onBackground,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = uiState.isAllDay,
                            onCheckedChange = { onAction(UiAction.OnAllDayChange(it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TDTheme.colors.purple,
                                checkedTrackColor = TDTheme.colors.lightPurple,
                            ),
                        )
                    }

                    if (!uiState.isAllDay) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                TDPickerField(
                                    title = stringResource(R.string.set_time),
                                    value =
                                    uiState.taskTimeStart?.format(timeFormatter)
                                        ?: stringResource(R.string.starts),
                                    onClick = { showStartTimePicker = true },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(com.example.uikit.R.drawable.ic_clock),
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
                                    uiState.taskTimeEnd?.format(timeFormatter)
                                        ?: stringResource(R.string.ends),
                                    onClick = { showEndTimePicker = true },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(com.example.uikit.R.drawable.ic_clock),
                                            tint = TDTheme.colors.onBackground,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    },
                                )
                            }
                        }
                    }

                    // Category is hidden for staged goals (no single category for a multi-step task).
                    if (uiState.taskType != TaskFormType.STAGED) {
                        TDText(
                            text = stringResource(R.string.category_label),
                            style = TDTheme.typography.heading6,
                            color = TDTheme.colors.onBackground,
                        )
                        TDCategoryPicker(
                            selectedKey = uiState.selectedCategory.name,
                            options = categoryOptions(),
                            onSelected = { key -> onAction(UiAction.OnCategoryChange(TaskCategory.valueOf(key))) },
                        )
                        if (uiState.selectedCategory == TaskCategory.OTHER) {
                            TDCompactOutlinedTextField(
                                label = stringResource(R.string.category_other_hint),
                                value = uiState.customCategoryName,
                                onValueChange = { onAction(UiAction.OnCustomCategoryNameChange(it)) },
                            )
                        }
                    }

                    // Reminder applies to one-time & routine; staged steps don't carry their own reminder.
                    if (uiState.taskType != TaskFormType.STAGED) {
                        TaskReminderChips(
                            selected = uiState.reminderOffsetMinutes,
                            onSelect = { onAction(UiAction.OnReminderOffsetChange(it)) },
                        )
                        if (uiState.isReminderInPast) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(TDTheme.colors.background, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    painter = painterResource(com.example.uikit.R.drawable.ic_warning),
                                    contentDescription = null,
                                    tint = TDTheme.colors.orange,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                TDText(
                                    text = stringResource(R.string.reminder_in_past_warning),
                                    style = TDTheme.typography.subheading1,
                                    color = TDTheme.colors.orange,
                                )
                            }
                        }
                    }

                    TDCompactOutlinedTextField(
                        label = stringResource(R.string.description),
                        value = uiState.taskDescription,
                        onValueChange = { onAction(UiAction.OnTaskDescriptionEdit(it)) },
                        singleLine = false,
                    )

                    val launchLocationPicker =
                        com.todoapp.mobile.ui.common.rememberLocationPickerLauncher { name, address, lat, lng ->
                            onAction(UiAction.OnLocationPicked(name, address, lat, lng))
                        }
                    com.todoapp.uikit.components.TDLocationPicker(
                        name = uiState.locationName,
                        address = uiState.locationAddress,
                        addLabel = stringResource(R.string.location_add_hint),
                        clearContentDescription = stringResource(R.string.location_clear),
                        onClick = launchLocationPicker,
                        onClear = { onAction(UiAction.OnLocationCleared) },
                    )
                }

                TaskPhotosSection(
                    photoUrls = uiState.photoUrls,
                    onPick = { bytes, mime -> onAction(UiAction.OnPhotoPicked(bytes, mime)) },
                    onDelete = { photoId -> onAction(UiAction.OnPhotoDelete(photoId)) },
                    pendingUploads = uiState.pendingPhotoUploads,
                    onCancelPending = { index -> onAction(UiAction.OnPendingPhotoCancel(index)) },
                )

                if (uiState.taskType == TaskFormType.STAGED) {
                    DetailsSubtaskEditor(
                        drafts = uiState.subtaskDrafts,
                        onTitleChange = { index, title -> onAction(UiAction.OnSubtaskTitleChange(index, title)) },
                        onToggle = { index -> onAction(UiAction.OnSubtaskToggle(index)) },
                        onRemove = { index -> onAction(UiAction.OnSubtaskRemove(index)) },
                    )
                }
            }
        }

        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .background(TDTheme.colors.background)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TDButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.save_changes),
                onClick = { onAction(UiAction.OnSaveChanges) },
                size = TDButtonSize.MEDIUM,
                isEnable = uiState.isDirty && !uiState.isSaving,
                fullWidth = true,
            )
            TDButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.cancel),
                onClick = { onAction(UiAction.OnCancelClick) },
                size = TDButtonSize.MEDIUM,
                type = TDButtonType.SECONDARY,
                fullWidth = true,
            )
        }
    }

    if (showStartTimePicker) {
        WheelTimePickerDialog(
            initialTime = uiState.taskTimeStart,
            onConfirm = {
                onAction(UiAction.OnTaskTimeStartEdit(it))
                showStartTimePicker = false
            },
            onDismiss = { showStartTimePicker = false },
        )
    }

    if (showEndTimePicker) {
        WheelTimePickerDialog(
            initialTime = uiState.taskTimeEnd,
            onConfirm = {
                onAction(UiAction.OnTaskTimeEndEdit(it))
                showEndTimePicker = false
            },
            onDismiss = { showEndTimePicker = false },
        )
    }
}

@Composable
private fun WheelTimePickerDialog(
    initialTime: LocalTime?,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val now = LocalTime.now()
    var hour by remember { mutableIntStateOf(initialTime?.hour ?: now.hour) }
    var minute by remember { mutableIntStateOf(initialTime?.minute ?: now.minute) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            contentColor = TDTheme.colors.onBackground,
            color = TDTheme.colors.background,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TDWheelTimePicker(
                    hour = hour,
                    minute = minute,
                    onHourChange = { hour = it },
                    onMinuteChange = { minute = it },
                )
                TDButton(
                    text = stringResource(com.example.uikit.R.string.ok),
                    onClick = { onConfirm(LocalTime.of(hour, minute)) },
                    size = TDButtonSize.SMALL,
                )
            }
        }
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun DetailsErrorPreview() {
    TDTheme { DetailsErrorContent("Task not found") {} }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun DetailsSuccessPreview_Dark() {
    TDTheme { DetailsSuccessContent(DetailsPreviewData.successState()) {} }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun DetailsSuccessPreview_Light() {
    TDTheme { DetailsSuccessContent(DetailsPreviewData.successState()) {} }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun DetailsSuccessPreview_Rich() {
    TDTheme {
        DetailsSuccessContent(
            DetailsPreviewData.successState(
                taskTitle = "Mom's Birthday",
                taskDescription = "Don't forget the cake",
                selectedCategory = com.todoapp.mobile.domain.model.TaskCategory.BIRTHDAY,
                selectedRecurrence = com.todoapp.mobile.domain.model.Recurrence.YEARLY,
                reminderOffsetMinutes = 1440L,
                isAllDay = true,
            ),
        ) {}
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun DetailsSuccessPreview_OtherCategory() {
    TDTheme {
        DetailsSuccessContent(
            DetailsPreviewData.successState(
                selectedCategory = com.todoapp.mobile.domain.model.TaskCategory.OTHER,
                customCategoryName = "Vet visit",
                selectedRecurrence = com.todoapp.mobile.domain.model.Recurrence.WEEKLY,
                reminderOffsetMinutes = 60L,
            ),
        ) {}
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun DetailsSuccessPreview_Routine() {
    TDTheme {
        DetailsSuccessContent(
            DetailsPreviewData.successState(
                taskTitle = "Su iç",
                taskType = TaskFormType.ROUTINE,
                selectedRecurrence = com.todoapp.mobile.domain.model.Recurrence.DAILY,
                reminderOffsetMinutes = 60L,
            ),
        ) {}
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun DetailsSuccessPreview_Staged() {
    TDTheme {
        DetailsSuccessContent(
            DetailsPreviewData.successState(
                taskTitle = "Tez bölümünü bitir",
                taskType = TaskFormType.STAGED,
                subtaskDrafts = listOf(
                    SubtaskDraft(1L, "Giriş", true),
                    SubtaskDraft(2L, "Yöntem", false),
                    SubtaskDraft(null, "", false),
                ),
            ),
        ) {}
    }
}

private fun detailsAbsoluteUrl(relative: String): String = BuildConfig.BASE_URL.trimEnd('/') + "/" + relative.trimStart('/')

private fun detailsPhotoIdFromUrl(url: String): Long? = url.trimEnd('/').substringAfterLast('/').toLongOrNull()

/** Removes the current banner cover: cancels the newest staged upload, else stages the saved cover for delete. */
private fun removeBannerPhoto(
    state: UiState.Success,
    onAction: (UiAction) -> Unit,
) {
    if (state.pendingPhotoUploads.isNotEmpty()) {
        onAction(UiAction.OnPendingPhotoCancel(state.pendingPhotoUploads.lastIndex))
    } else {
        val coverUrl = state.photoUrls.firstOrNull() ?: return
        detailsPhotoIdFromUrl(coverUrl)?.let { onAction(UiAction.OnPhotoDelete(it)) }
    }
}

/** Replaces the cover: removes the current one, then stages the freshly cropped image. */
private fun replaceBannerPhoto(
    state: UiState.Success,
    onAction: (UiAction) -> Unit,
    bytes: ByteArray,
) {
    removeBannerPhoto(state, onAction)
    onAction(UiAction.OnPhotoPicked(bytes, "image/jpeg"))
}

@Composable
private fun DetailsTypeHeader(type: TaskFormType) {
    when (type) {
        TaskFormType.ONE_TIME -> TaskTypeHeader(
            icon = painterResource(com.example.uikit.R.drawable.ic_edit_task),
            name = stringResource(R.string.type_one_time_title),
            subtitle = stringResource(R.string.type_one_time_subtitle),
            accent = TDTheme.colors.darkPending,
        )

        TaskFormType.ROUTINE -> TaskTypeHeader(
            icon = painterResource(R.drawable.ic_calendar),
            name = stringResource(R.string.type_routine_title),
            subtitle = stringResource(R.string.type_routine_subtitle),
            accent = TDTheme.colors.purple,
        )

        TaskFormType.STAGED -> TaskTypeHeader(
            icon = painterResource(R.drawable.ic_staged),
            name = stringResource(R.string.type_staged_title),
            subtitle = stringResource(R.string.type_staged_subtitle),
            accent = TDTheme.colors.mediumGreen,
        )
    }
}
