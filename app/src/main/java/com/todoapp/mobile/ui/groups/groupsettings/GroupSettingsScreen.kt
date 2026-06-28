package com.todoapp.mobile.ui.groups.groupsettings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uikit.R
import com.todoapp.mobile.ui.groups.groupsettings.GroupSettingsContract.UiAction
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDButtonType
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.extensions.collectWithLifecycle
import com.todoapp.uikit.theme.TDTheme

@Composable
fun GroupSettingsScreen(viewModel: GroupSettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    viewModel.uiEffect.collectWithLifecycle { effect ->
        when (effect) {
            is GroupSettingsContract.UiEffect.ShowToast -> {
                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    GroupSettingsContent(
        uiState = uiState,
        onAction = viewModel::onAction,
    )
}

@Composable
private fun GroupSettingsContent(
    uiState: GroupSettingsContract.UiState,
    onAction: (UiAction) -> Unit,
) {
    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .background(TDTheme.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        if (uiState.isLoading) {
            Spacer(modifier = Modifier.height(48.dp))
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = TDTheme.colors.pendingGray,
            )
            return@Column
        }

        val isAdmin = uiState.currentUserRole == "ADMIN"

        val avatarPicker =
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia(),
            ) { uri ->
                // Route the picked image through the crop screen; upload happens once it returns.
                if (uri != null) onAction(UiAction.OnAvatarPicked(uri.toString()))
            }

        GroupAvatar(
            avatarUrl = uiState.avatarUrl,
            avatarVersion = uiState.avatarVersion,
            name = uiState.name,
            isAdmin = isAdmin,
            onPickAvatar = {
                avatarPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(modifier = Modifier.height(24.dp))

        TDText(
            text = stringResource(com.todoapp.mobile.R.string.group_name_label),
            style = TDTheme.typography.subheading2,
            color = TDTheme.colors.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (isAdmin) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { onAction(UiAction.OnNameChange(it)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TDTheme.colors.pendingGray,
                    unfocusedBorderColor = TDTheme.colors.lightGray,
                    focusedTextColor = TDTheme.colors.onBackground,
                    unfocusedTextColor = TDTheme.colors.onBackground,
                    cursorColor = TDTheme.colors.pendingGray,
                ),
                singleLine = true,
            )
        } else {
            TDText(
                text = uiState.name,
                style = TDTheme.typography.regularTextStyle,
                color = TDTheme.colors.gray,
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TDText(
            text = stringResource(com.todoapp.mobile.R.string.group_description),
            style = TDTheme.typography.subheading2,
            color = TDTheme.colors.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (isAdmin) {
            OutlinedTextField(
                value = uiState.description,
                onValueChange = { onAction(UiAction.OnDescriptionChange(it)) },
                modifier =
                Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(12.dp),
                colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TDTheme.colors.pendingGray,
                    unfocusedBorderColor = TDTheme.colors.lightGray,
                    focusedTextColor = TDTheme.colors.onBackground,
                    unfocusedTextColor = TDTheme.colors.onBackground,
                    cursorColor = TDTheme.colors.pendingGray,
                ),
                maxLines = 4,
            )
        } else {
            TDText(
                text = uiState.description.ifBlank { "—" },
                style = TDTheme.typography.regularTextStyle,
                color = TDTheme.colors.gray,
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isAdmin) {
            TDButton(
                text = stringResource(com.todoapp.mobile.R.string.save_changes),
                type = TDButtonType.PRIMARY,
                isEnable = !uiState.isSaving,
                modifier = Modifier.align(Alignment.End),
                onClick = { onAction(UiAction.OnSaveTap) },
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.currentUserRole == "ADMIN") {
            TDText(
                text = stringResource(com.todoapp.mobile.R.string.management),
                style = TDTheme.typography.subheading1,
                color = TDTheme.colors.gray,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            ManagementRow(
                icon = R.drawable.ic_members,
                title = stringResource(com.todoapp.mobile.R.string.manage_members),
                subtitle = stringResource(com.todoapp.mobile.R.string.add_or_remove_people),
                onClick = { onAction(UiAction.OnManageMembersTap) },
            )

            Spacer(modifier = Modifier.height(12.dp))

            ManagementRow(
                icon = R.drawable.ic_members,
                title = stringResource(com.todoapp.mobile.R.string.transfer_ownership),
                subtitle = stringResource(com.todoapp.mobile.R.string.transfer_ownership_info),
                onClick = { onAction(UiAction.OnTransferOwnershipTap) },
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (!isAdmin) {
            Spacer(modifier = Modifier.height(8.dp))
            TDButton(
                text = stringResource(com.todoapp.mobile.R.string.leave_group),
                type = TDButtonType.OUTLINE,
                isEnable = !uiState.isLeaving,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onAction(UiAction.OnLeaveTap) },
            )
        }
    }

    if (uiState.isLeaveDialogOpen) {
        AlertDialog(
            onDismissRequest = { onAction(UiAction.OnLeaveDialogDismiss) },
            title = { Text(stringResource(com.todoapp.mobile.R.string.leave_group_title)) },
            titleContentColor = TDTheme.colors.onBackground,
            containerColor = TDTheme.colors.background,
            textContentColor = TDTheme.colors.gray,
            text = { Text(stringResource(com.todoapp.mobile.R.string.leave_group_message)) },
            confirmButton = {
                TextButton(onClick = { onAction(UiAction.OnLeaveConfirm) }) {
                    Text(
                        text = stringResource(com.todoapp.mobile.R.string.leave_group_confirm),
                        color = TDTheme.colors.crossRed,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(UiAction.OnLeaveDialogDismiss) }) {
                    Text(
                        text = stringResource(com.todoapp.mobile.R.string.cancel),
                        color = TDTheme.colors.gray,
                    )
                }
            },
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun GroupSettingsLoadingPreview() {
    TDTheme {
        GroupSettingsContent(
            uiState = GroupSettingsContract.UiState(isLoading = true),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun GroupSettingsAdminPreview() {
    TDTheme {
        GroupSettingsContent(
            uiState =
            GroupSettingsContract.UiState(
                groupId = 1L,
                name = "The Smith Family",
                description = "Daily chores, grocery lists, and vacation planning for 2024.",
                currentUserRole = "ADMIN",
                isLoading = false,
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun GroupSettingsMemberPreview() {
    TDTheme {
        GroupSettingsContent(
            uiState =
            GroupSettingsContract.UiState(
                groupId = 1L,
                name = "The Smith Family",
                description = "Daily chores, grocery lists, and vacation planning for 2024.",
                currentUserRole = "MEMBER",
                isLoading = false,
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreviewDialog
@Composable
private fun GroupSettingsLeaveDialogPreview() {
    TDTheme {
        GroupSettingsContent(
            uiState =
            GroupSettingsContract.UiState(
                groupId = 1L,
                name = "The Smith Family",
                description = "Daily chores, grocery lists, and vacation planning for 2024.",
                currentUserRole = "MEMBER",
                isLeaveDialogOpen = true,
                isLoading = false,
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun GroupSettingsSavingPreview() {
    TDTheme {
        GroupSettingsContent(
            uiState =
            GroupSettingsContract.UiState(
                groupId = 1L,
                name = "The Smith Family",
                description = "Daily chores, grocery lists, and vacation planning for 2024.",
                currentUserRole = "ADMIN",
                isSaving = true,
                isLoading = false,
            ),
            onAction = {},
        )
    }
}

@Composable
private fun ManagementRow(
    icon: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TDTheme.colors.lightPending)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = TDTheme.colors.pendingGray,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            TDText(
                text = title,
                style = TDTheme.typography.subheading2,
                color = TDTheme.colors.onBackground,
            )
            TDText(
                text = subtitle,
                style = TDTheme.typography.subheading1,
                color = TDTheme.colors.gray,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_arrow_forward),
            contentDescription = null,
            tint = TDTheme.colors.gray,
            modifier = Modifier.size(20.dp),
        )
    }
}
