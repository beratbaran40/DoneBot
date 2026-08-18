package com.todoapp.mobile.ui.groups.grouptaskdetail

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.todoapp.mobile.BuildConfig
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.common.components.SubtaskChecklist
import com.todoapp.mobile.ui.common.components.TaskPhotoBannerEditable
import com.todoapp.mobile.ui.common.components.TaskTypeBadge
import com.todoapp.mobile.ui.common.components.recurrenceDisplayText
import com.todoapp.mobile.ui.groups.groupdetail.AssigneeAvatar
import com.todoapp.mobile.ui.groups.grouptaskdetail.GroupTaskDetailContract.TaskUiModel
import com.todoapp.mobile.ui.groups.grouptaskdetail.GroupTaskDetailContract.UiAction
import com.todoapp.mobile.ui.groups.grouptaskdetail.GroupTaskDetailContract.UiState
import com.todoapp.uikit.components.TDPriorityBadge
import com.todoapp.uikit.components.TDRoutineProgress
import com.todoapp.uikit.components.TDScreenWithSheet
import com.todoapp.uikit.components.TDTaskCompletionCard
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.extensions.collectWithLifecycle
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.theme.TDTheme
import com.example.uikit.R as UiKitR

@Composable
fun GroupTaskDetailScreen(viewModel: GroupTaskDetailViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    viewModel.uiEffect.collectWithLifecycle { effect ->
        when (effect) {
            is GroupTaskDetailContract.UiEffect.ShowToast ->
                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
        }
    }

    val successState = uiState as? UiState.Success
    TDScreenWithSheet(
        isSheetOpen = successState?.isEditSheetOpen ?: false,
        sheetContent = {
            if (successState != null) {
                GroupTaskEditSheet(state = successState, onAction = viewModel::onAction)
            }
        },
        onDismissSheet = { viewModel.onAction(UiAction.OnEditDismiss) },
    ) {
        GroupTaskDetailContent(
            uiState = uiState,
            onAction = viewModel::onAction,
        )
    }
}

@Composable
private fun GroupTaskDetailContent(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
) {
    when (uiState) {
        is UiState.Loading ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = TDTheme.colors.pendingGray)
            }
        is UiState.Error ->
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                TDText(
                    text = uiState.message,
                    style = TDTheme.typography.subheading3,
                    color = TDTheme.colors.gray,
                )
            }
        is UiState.Success -> TaskDetailBody(task = uiState.task, onAction = onAction)
    }
}

@Composable
private fun TaskDetailBody(
    task: TaskUiModel,
    onAction: (UiAction) -> Unit,
) {
    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // Hero banner = first photo (full-bleed). Group tasks show the priority badge in the corner.
        if (task.photoUrls.isNotEmpty()) {
            val cover = groupAbsoluteUrl(task.photoUrls.first())
            TaskPhotoBannerEditable(
                displayModel = cover,
                onCropped = { bytes -> replaceGroupBannerPhoto(task, onAction, bytes) },
                onRemove = { removeGroupBannerPhoto(task, onAction) },
                badge = { task.priority?.let { TDPriorityBadge(priority = it) } },
            )
        }
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TDText(
                    text = task.title,
                    style = TDTheme.typography.heading3,
                    color = TDTheme.colors.onBackground,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onAction(UiAction.OnEditTap) }) {
                    Icon(
                        painter = tdPainter(UiKitR.drawable.ic_edit_task),
                        contentDescription = stringResource(R.string.edit_task),
                        tint = TDTheme.colors.pendingGray,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            // The shape of the task, which this screen never showed at all: a group routine, staged
            // goal and custom task were three identical-looking rows. Derived from the rule the task
            // already carries — group tasks have no stored declaration (see GroupTask.capabilities).
            Spacer(modifier = Modifier.height(8.dp))
            TaskTypeBadge(type = task.taskType)

            if (!task.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                TDText(
                    text = task.description,
                    style = TDTheme.typography.subheading3,
                    color = TDTheme.colors.gray,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            TDTaskCompletionCard(
                isCompleted = task.isCompleted,
                enabled = task.canComplete,
                disabledHint =
                if (!task.canComplete) {
                    stringResource(R.string.only_assignee_can_complete)
                } else {
                    null
                },
                onToggle = { onAction(UiAction.OnToggleComplete) },
            )

            // "Every 2 days", "Mon · Wed · Fri" — the same sentence a personal routine gets.
            val ruleText = recurrenceDisplayText(
                recurrence = task.recurrence,
                recurrenceInterval = task.recurrenceInterval,
                recurrenceByDay = task.recurrenceByDay,
            )
            // The rule used to be nested inside the assignee/due-date condition, so an unassigned
            // group routine with no due time showed no repeat information at all — the one shape
            // where the card is the ONLY place it could have appeared.
            if (task.assigneeName != null || task.dueTime != null || ruleText != null) {
                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(TDTheme.shapes.medium)
                        .background(TDTheme.colors.lightPending)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    task.assigneeName?.let { name ->
                        MetadataRow(label = stringResource(R.string.assignee)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                AssigneeAvatar(
                                    avatarUrl = task.assigneeAvatarUrl,
                                    initials = task.assigneeInitials ?: name.take(2).uppercase(),
                                )
                                TDText(
                                    text = name,
                                    style = TDTheme.typography.subheading2,
                                    color = TDTheme.colors.onBackground,
                                )
                            }
                        }
                    }

                    task.dueTime?.let { time ->
                        MetadataRow(label = stringResource(R.string.due_prefix)) {
                            TDText(
                                text = time,
                                style = TDTheme.typography.subheading2,
                                color = TDTheme.colors.onBackground,
                            )
                        }
                    }

                    ruleText?.let { rule ->
                        MetadataRow(label = stringResource(R.string.creation_frequency_label)) {
                            TDText(
                                text = rule,
                                style = TDTheme.typography.subheading2,
                                color = TDTheme.colors.onBackground,
                            )
                        }
                    }

                    // The scheduled end, which the group screens carried in storage and in the
                    // alarms but never once showed as a date — only indirectly, as the denominator
                    // of the progress bar below.
                    task.recurrenceUntil?.let { end ->
                        MetadataRow(label = stringResource(R.string.creation_end_label)) {
                            TDText(
                                text = end.format(GROUP_END_DATE_FORMAT),
                                style = TDTheme.typography.subheading2,
                                color = TDTheme.colors.onBackground,
                            )
                        }
                    }
                }
            }

            val dayIndex = task.routineDayIndex
            val dayTotal = task.routineDayTotal
            if (dayIndex != null && dayTotal != null) {
                Spacer(modifier = Modifier.height(16.dp))
                TDRoutineProgress(
                    current = dayIndex,
                    total = dayTotal,
                    label = stringResource(R.string.routine_day_progress, dayIndex, dayTotal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (task.subtasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                SubtaskChecklist(
                    subtasks = task.subtasks,
                    onToggle = { subtaskId, checked -> onAction(UiAction.OnSubtaskToggle(subtaskId, checked)) },
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            TaskPhotosSection(
                photoUrls = task.photoUrls,
                onPick = { bytes, mime -> onAction(UiAction.OnPhotoPicked(bytes, mime)) },
                onDelete = { photoId -> onAction(UiAction.OnPhotoDelete(photoId)) },
                onReport = { relativeUrl -> onAction(UiAction.OnPhotoReport(relativeUrl)) },
            )
        }
    }
}

private val GROUP_END_DATE_FORMAT: java.time.format.DateTimeFormatter =
    java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy")

private fun groupAbsoluteUrl(relative: String): String = BuildConfig.BASE_URL.trimEnd('/') + "/" + relative.trimStart('/')

private fun groupPhotoIdFromUrl(url: String): Long? = url.trimEnd('/').substringAfterLast('/').toLongOrNull()

/** Group task photos upload/delete immediately (no staging); the VM reloads after each. */
private fun removeGroupBannerPhoto(
    task: TaskUiModel,
    onAction: (UiAction) -> Unit,
) {
    val cover = task.photoUrls.firstOrNull() ?: return
    groupPhotoIdFromUrl(cover)?.let { onAction(UiAction.OnPhotoDelete(it)) }
}

private fun replaceGroupBannerPhoto(
    task: TaskUiModel,
    onAction: (UiAction) -> Unit,
    bytes: ByteArray,
) {
    removeGroupBannerPhoto(task, onAction)
    onAction(UiAction.OnPhotoPicked(bytes, "image/jpeg"))
}

@Composable
private fun MetadataRow(
    label: String,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TDText(
            text = label,
            style = TDTheme.typography.subheading1,
            color = TDTheme.colors.gray,
        )
        content()
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun GroupTaskDetailContentPreview(
    @PreviewParameter(GroupTaskDetailPreviewProvider::class) uiState: UiState,
) {
    TDTheme {
        GroupTaskDetailContent(
            uiState = uiState,
            onAction = {},
        )
    }
}
