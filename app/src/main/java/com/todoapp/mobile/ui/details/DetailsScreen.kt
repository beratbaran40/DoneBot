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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.todoapp.mobile.BuildConfig
import com.todoapp.mobile.R
import com.todoapp.mobile.common.deviceTimeFormatter
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.TaskCategory
import com.todoapp.mobile.domain.model.TaskType
import com.todoapp.mobile.ui.common.categoryOptions
import com.todoapp.mobile.ui.common.components.TaskPhotoBannerEditable
import com.todoapp.mobile.ui.common.components.TaskTypeBadge
import com.todoapp.mobile.ui.common.components.taskTypeAccent
import com.todoapp.mobile.ui.common.taskform.TaskReminderChips
import com.todoapp.mobile.ui.common.taskform.TaskReminderTimesEditor
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
import com.todoapp.uikit.components.TDPickerField
import com.todoapp.uikit.components.TDSwitch
import com.todoapp.uikit.components.TDSwitchTone
import com.todoapp.uikit.components.TDTaskCompletionCard
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.components.TDWheelTimePicker
import com.todoapp.uikit.extensions.ObscuredTouchGuard
import com.todoapp.uikit.extensions.collectWithLifecycle
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.theme.TDTheme
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

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
            .fillMaxSize(),
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
                            ObscuredTouchGuard()
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
            painter = tdPainter(com.example.uikit.R.drawable.ic_error),
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
    val context = LocalContext.current
    val timeFormatter = remember(context) { deviceTimeFormatter(context) }

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

                // Completion toggle only when nothing else owns completion: a recurring task completes
                // per-day from the list surfaces, and a staged one derives it from its steps.
                if (!uiState.capabilities.completionIsPerDay && !uiState.capabilities.completionIsDerivedFromSteps) {
                    TDTaskCompletionCard(
                        isCompleted = uiState.isCompleted,
                        onToggle = { onAction(UiAction.OnToggleComplete) },
                    )
                }

                Column(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(TDTheme.shapes.large)
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

                    // Anything that repeats surfaces frequency right after the title (before the date).
                    if (uiState.capabilities.recurs) {
                        DetailsRecurrenceBlock(uiState = uiState, onAction = onAction)
                    }

                    DetailsDateSection(uiState = uiState, onAction = onAction)

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = tdPainter(com.example.uikit.R.drawable.ic_clock),
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
                        TDSwitch(
                            checked = uiState.isAllDay,
                            onCheckedChange = { onAction(UiAction.OnAllDayChange(it)) },
                            tone = TDSwitchTone.ACCENT,
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
                                            painter = tdPainter(com.example.uikit.R.drawable.ic_clock),
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
                                            painter = tdPainter(com.example.uikit.R.drawable.ic_clock),
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
                    if (!uiState.capabilities.hasSteps) {
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

                    DetailsReminderBlock(uiState = uiState, onAction = onAction)

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

                if (uiState.capabilities.hasSteps) {
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
            shape = TDTheme.shapes.large,
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
private fun DetailsSuccessPreview_Completed() {
    TDTheme {
        DetailsSuccessContent(
            DetailsPreviewData.successState(
                taskTitle = "Call the dentist",
                isCompleted = true,
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

/** Open-ended routine: frequency block, no scheduled end, so no span and no day-N-of-M. */
@com.todoapp.uikit.previews.TDPreview
@Composable
private fun DetailsSuccessPreview_Routine() {
    TDTheme {
        DetailsSuccessContent(
            DetailsPreviewData.successState(
                taskTitle = "Su iç",
                taskType = TaskType.ROUTINE,
                selectedRecurrence = com.todoapp.mobile.domain.model.Recurrence.DAILY,
                capabilities = recurringCapabilities,
            ),
        ) {}
    }
}

/** The span case: a routine with a scheduled end, which is what the date field now has to show. */
@com.todoapp.uikit.previews.TDPreview
@Composable
private fun DetailsSuccessPreview_BoundedRoutine() {
    TDTheme {
        DetailsSuccessContent(
            DetailsPreviewData.successState(
                taskTitle = "Antibiyotik",
                taskType = TaskType.ROUTINE,
                selectedRecurrence = com.todoapp.mobile.domain.model.Recurrence.DAILY,
                capabilities = recurringCapabilities,
                taskDate = java.time.LocalDate.of(2026, 8, 17),
                recurrenceUntil = java.time.LocalDate.of(2026, 8, 30),
                routineDayIndex = 3,
                routineDayTotal = 14,
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
                taskType = TaskType.STAGED,
                capabilities = com.todoapp.mobile.ui.common.taskform.TaskCapabilities(
                    recurs = false,
                    hasSteps = true,
                    hasMultipleReminders = false,
                ),
                subtaskDrafts = listOf(
                    SubtaskDraft(1L, "Giriş", true),
                    SubtaskDraft(2L, "Yöntem", false),
                    SubtaskDraft(null, "", false),
                ),
            ),
        ) {}
    }
}

/**
 * The shape the whole declared-type change exists for: repeats, runs between two dates, has steps and
 * reminds more than once a day. Every gated section on the screen is on at once.
 */
@com.todoapp.uikit.previews.TDPreview
@Composable
private fun DetailsSuccessPreview_Custom() {
    TDTheme {
        DetailsSuccessContent(
            DetailsPreviewData.successState(
                taskTitle = "Sabah rutini",
                taskType = TaskType.CUSTOM,
                selectedRecurrence = com.todoapp.mobile.domain.model.Recurrence.WEEKLY,
                capabilities = com.todoapp.mobile.ui.common.taskform.TaskCapabilities(
                    recurs = true,
                    hasSteps = true,
                    hasMultipleReminders = true,
                ),
                taskDate = java.time.LocalDate.of(2026, 8, 17),
                recurrenceUntil = java.time.LocalDate.of(2026, 11, 16),
                recurrenceInterval = 2,
                recurrenceByDay = setOf(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.WEDNESDAY),
                reminderTimes = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)),
                routineDayIndex = 2,
                routineDayTotal = 26,
                subtaskDrafts = listOf(
                    SubtaskDraft(1L, "Esneme", true),
                    SubtaskDraft(2L, "Kahvaltı", false),
                    SubtaskDraft(null, "", false),
                ),
            ),
        ) {}
    }
}

private val recurringCapabilities = com.todoapp.mobile.ui.common.taskform.TaskCapabilities(
    recurs = true,
    hasSteps = false,
    hasMultipleReminders = false,
)

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

/**
 * A repeating task reminds at absolute times of day; a one-off reminds relative to its start. Staged
 * steps carry no reminder of their own, so they get neither.
 *
 * The scheduled end used to live here too, which put "Ends" below Category and a long way from the
 * date it bounds — the reverse of the order the create form uses. It now sits directly under the
 * date field, in [DetailsDateSection].
 */
@Composable
private fun DetailsReminderBlock(
    uiState: UiState.Success,
    onAction: (UiAction) -> Unit,
) {
    if (uiState.capabilities.recurs) {
        TaskReminderTimesEditor(
            times = uiState.reminderTimes,
            onAdd = { onAction(UiAction.OnReminderTimeAdd(it)) },
            onRemove = { onAction(UiAction.OnReminderTimeRemove(it)) },
        )
        return
    }
    if (uiState.capabilities.hasSteps) return
    TaskReminderChips(
        selected = uiState.reminderOffsetMinutes,
        onSelect = { onAction(UiAction.OnReminderOffsetChange(it)) },
    )
    if (uiState.isReminderInPast) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TDTheme.colors.background, TDTheme.shapes.small)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = tdPainter(com.example.uikit.R.drawable.ic_warning),
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

@Composable
private fun DetailsTypeHeader(type: TaskType) {
    when (type) {
        TaskType.ONE_TIME -> TaskTypeHeader(
            icon = tdPainter(com.example.uikit.R.drawable.ic_edit_task),
            name = stringResource(R.string.type_one_time_title),
            subtitle = stringResource(R.string.type_one_time_subtitle),
            accent = taskTypeAccent(type),
        )

        TaskType.ROUTINE -> TaskTypeHeader(
            icon = tdPainter(R.drawable.ic_calendar),
            name = stringResource(R.string.type_routine_title),
            subtitle = stringResource(R.string.type_routine_subtitle),
            accent = taskTypeAccent(type),
        )

        TaskType.STAGED -> TaskTypeHeader(
            icon = tdPainter(R.drawable.ic_staged),
            name = stringResource(R.string.type_staged_title),
            subtitle = stringResource(R.string.type_staged_subtitle),
            accent = taskTypeAccent(type),
        )

        TaskType.CUSTOM -> TaskTypeHeader(
            icon = tdPainter(R.drawable.ic_custom),
            name = stringResource(R.string.type_custom_title),
            subtitle = stringResource(R.string.type_custom_subtitle),
            accent = taskTypeAccent(type),
        )
    }
}
