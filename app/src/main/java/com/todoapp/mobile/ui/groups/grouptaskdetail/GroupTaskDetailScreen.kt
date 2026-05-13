package com.todoapp.mobile.ui.groups.grouptaskdetail

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.groups.groupdetail.AssigneeAvatar
import com.todoapp.mobile.ui.groups.grouptaskdetail.GroupTaskDetailContract.TaskUiModel
import com.todoapp.mobile.ui.groups.grouptaskdetail.GroupTaskDetailContract.UiAction
import com.todoapp.mobile.ui.groups.grouptaskdetail.GroupTaskDetailContract.UiState
import com.todoapp.uikit.components.TDScreenWithSheet
import com.todoapp.uikit.components.TDTaskStatusLabel
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.extensions.collectWithLifecycle
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
            .background(TDTheme.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onAction(UiAction.OnToggleComplete) }
                    .padding(12.dp),
            ) {
                TDTaskStatusLabel(isCompleted = task.isCompleted)
                Spacer(modifier = Modifier.width(12.dp))
                TDText(
                    text = task.title,
                    style = TDTheme.typography.heading3,
                    color = TDTheme.colors.onBackground,
                )
            }
            IconButton(onClick = { onAction(UiAction.OnEditTap) }) {
                Icon(
                    painter = painterResource(UiKitR.drawable.ic_edit_task),
                    contentDescription = stringResource(R.string.edit_task),
                    tint = TDTheme.colors.pendingGray,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

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

        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
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

            MetadataRow(label = stringResource(R.string.status)) {
                TDText(
                    text =
                    if (task.isCompleted) {
                        stringResource(
                            UiKitR.string.status_completed,
                        )
                    } else {
                        stringResource(UiKitR.string.status_pending)
                    },
                    style = TDTheme.typography.subheading2,
                    color = if (task.isCompleted) TDTheme.colors.darkGreen else TDTheme.colors.gray,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        TaskPhotosSection(
            photoUrls = task.photoUrls,
            onPick = { bytes, mime -> onAction(UiAction.OnPhotoPicked(bytes, mime)) },
            onDelete = { photoId -> onAction(UiAction.OnPhotoDelete(photoId)) },
        )
    }
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
