package com.todoapp.mobile.ui.groups.memberprofile

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.todoapp.mobile.ui.groups.groupdetail.MemberAvatar
import com.todoapp.mobile.ui.groups.groupdetail.RoleBadge
import com.todoapp.mobile.ui.groups.memberprofile.MemberProfileContract.MemberUiItem
import com.todoapp.mobile.ui.groups.memberprofile.MemberProfileContract.UiAction
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDButtonType
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.components.TDUndoSnackbar
import com.todoapp.uikit.extensions.ObscuredTouchGuard
import com.todoapp.uikit.extensions.collectWithLifecycle
import com.todoapp.uikit.theme.TDTheme

@Composable
fun MemberProfileScreen(viewModel: MemberProfileViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    viewModel.uiEffect.collectWithLifecycle { effect ->
        when (effect) {
            is MemberProfileContract.UiEffect.ShowToast ->
                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
        }
    }

    MemberProfileContent(
        uiState = uiState,
        onAction = viewModel::onAction,
    )
}

@Composable
private fun MemberProfileContent(
    uiState: MemberProfileContract.UiState,
    onAction: (UiAction) -> Unit,
) {
    Box(
        modifier =
        Modifier
            .fillMaxSize(),
    ) {
        when (uiState) {
            is MemberProfileContract.UiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = TDTheme.colors.pendingGray,
                )
            }

            is MemberProfileContract.UiState.Error -> {
                TDText(
                    modifier = Modifier.align(Alignment.Center),
                    text = uiState.message,
                    color = TDTheme.colors.crossRed,
                )
            }

            is MemberProfileContract.UiState.Success -> {
                MemberProfileSuccessContent(
                    uiState = uiState,
                    onAction = onAction,
                )
            }
        }
    }
}

@Composable
private fun MemberProfileSuccessContent(
    uiState: MemberProfileContract.UiState.Success,
    onAction: (UiAction) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            MemberAvatar(
                initials = uiState.member.initials,
                size = 72,
                avatarUrl = uiState.member.avatarUrl,
            )
            Spacer(modifier = Modifier.height(12.dp))
            TDText(
                text = uiState.member.displayName,
                style = TDTheme.typography.heading2,
                color = TDTheme.colors.onBackground,
            )
            Spacer(modifier = Modifier.height(4.dp))
            RoleBadge(role = uiState.member.role)
            Spacer(modifier = Modifier.height(32.dp))
            MemberInfoCard(member = uiState.member)
            Spacer(modifier = Modifier.height(32.dp))
            TDButton(
                text = stringResource(com.todoapp.mobile.R.string.report_user),
                type = TDButtonType.SECONDARY,
                fullWidth = true,
                onClick = { onAction(UiAction.OnReportTap) },
            )
            Spacer(modifier = Modifier.height(12.dp))
            TDButton(
                text = stringResource(
                    if (uiState.isBlocked) {
                        com.todoapp.mobile.R.string.unblock_user
                    } else {
                        com.todoapp.mobile.R.string.block_user
                    },
                ),
                type = TDButtonType.SECONDARY,
                fullWidth = true,
                onClick = { onAction(UiAction.OnBlockToggle) },
            )
            if (uiState.isCurrentUserAdmin) {
                Spacer(modifier = Modifier.height(12.dp))
                TDButton(
                    text = stringResource(com.todoapp.mobile.R.string.remove_from_group),
                    type = TDButtonType.CANCEL,
                    fullWidth = true,
                    isEnable = !uiState.pendingRemoval,
                    onClick = { onAction(UiAction.OnRemoveTap) },
                )
            }
            Spacer(modifier = Modifier.height(80.dp))
        }

        AnimatedVisibility(
            modifier =
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            visible = uiState.pendingRemoval,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            TDUndoSnackbar(
                message = stringResource(com.todoapp.mobile.R.string.member_removed),
                onUndo = { onAction(UiAction.OnUndoRemove) },
            )
        }
    }

    if (uiState.showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { onAction(UiAction.OnDismissDialog) },
            title = {
                TDText(
                    text = stringResource(com.todoapp.mobile.R.string.remove_member_title),
                    style = TDTheme.typography.heading3,
                    color = TDTheme.colors.onBackground,
                )
            },
            containerColor = TDTheme.colors.background,
            textContentColor = TDTheme.colors.onBackground,
            text = {
                ObscuredTouchGuard()
                TDText(
                    text = stringResource(com.todoapp.mobile.R.string.remove_member_message),
                    style = TDTheme.typography.subheading1,
                    color = TDTheme.colors.onBackground,
                )
            },
            confirmButton = {
                TextButton(onClick = { onAction(UiAction.OnConfirmRemove) }) {
                    TDText(
                        text = stringResource(com.todoapp.mobile.R.string.remove),
                        color = TDTheme.colors.crossRed,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(UiAction.OnDismissDialog) }) {
                    TDText(
                        text = stringResource(com.todoapp.mobile.R.string.cancel),
                        color = TDTheme.colors.onBackground,
                    )
                }
            },
        )
    }

    if (uiState.showReportDialog) {
        AlertDialog(
            onDismissRequest = { onAction(UiAction.OnReportDismiss) },
            title = {
                TDText(
                    text = stringResource(com.todoapp.mobile.R.string.report_user_title),
                    style = TDTheme.typography.heading3,
                    color = TDTheme.colors.onBackground,
                )
            },
            containerColor = TDTheme.colors.background,
            textContentColor = TDTheme.colors.onBackground,
            text = {
                TDText(
                    text = stringResource(com.todoapp.mobile.R.string.report_user_message),
                    style = TDTheme.typography.subheading1,
                    color = TDTheme.colors.onBackground,
                )
            },
            confirmButton = {
                TextButton(onClick = { onAction(UiAction.OnReportConfirm) }) {
                    TDText(
                        text = stringResource(com.todoapp.mobile.R.string.report_confirm_button),
                        color = TDTheme.colors.crossRed,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(UiAction.OnReportDismiss) }) {
                    TDText(
                        text = stringResource(com.todoapp.mobile.R.string.cancel),
                        color = TDTheme.colors.onBackground,
                    )
                }
            },
        )
    }
}

@Composable
private fun MemberInfoCard(member: MemberUiItem) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TDTheme.colors.background),
    ) {
        InfoRow(
            iconRes = R.drawable.ic_name,
            label = if (member.lastName.isNotEmpty()) "${member.firstName} ${member.lastName}" else member.firstName,
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = TDTheme.colors.lightGray.copy(alpha = 0.5f),
        )
        InfoRow(
            iconRes = R.drawable.ic_mail,
            label = member.email,
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = TDTheme.colors.lightGray.copy(alpha = 0.5f),
        )
        InfoRow(
            iconRes = com.todoapp.mobile.R.drawable.ic_calendar,
            label = "${stringResource(com.todoapp.mobile.R.string.joined)} ${member.joinedAt}",
        )
    }
}

@Composable
private fun InfoRow(
    iconRes: Int,
    label: String,
) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = TDTheme.colors.pendingGray,
            modifier = Modifier.size(20.dp),
        )
        TDText(
            text = label,
            style = TDTheme.typography.subheading1,
            color = TDTheme.colors.onBackground,
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun MemberProfileContentPreview() {
    TDTheme {
        MemberProfileContent(
            uiState =
            MemberProfileContract.UiState.Success(
                member =
                MemberUiItem(
                    userId = 1,
                    firstName = "John",
                    lastName = "Doe",
                    email = "john@example.com",
                    avatarUrl = null,
                    role = "ADMIN",
                    joinedAt = "Jan 12, 2024",
                    displayName = "John Doe",
                    initials = "JD",
                ),
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun MemberProfileContentDarkPreview() {
    TDTheme {
        MemberProfileContent(
            uiState =
            MemberProfileContract.UiState.Success(
                member =
                MemberUiItem(
                    userId = 2,
                    firstName = "Jane",
                    lastName = "Smith",
                    email = "jane@example.com",
                    avatarUrl = null,
                    role = "MEMBER",
                    joinedAt = "Feb 05, 2024",
                    displayName = "Jane Smith",
                    initials = "JS",
                ),
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun MemberProfileLoadingPreview() {
    TDTheme {
        MemberProfileContent(uiState = MemberProfileContract.UiState.Loading, onAction = {})
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun MemberProfileErrorPreview() {
    TDTheme {
        MemberProfileContent(
            uiState = MemberProfileContract.UiState.Error("Failed to load member"),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun MemberProfileConfirmRemovePreview() {
    TDTheme {
        MemberProfileContent(
            uiState =
            MemberProfileContract.UiState.Success(
                member =
                MemberUiItem(
                    userId = 1,
                    firstName = "John",
                    lastName = "Doe",
                    email = "john@example.com",
                    avatarUrl = null,
                    role = "MEMBER",
                    joinedAt = "Jan 12, 2024",
                    displayName = "John Doe",
                    initials = "JD",
                ),
                showConfirmDialog = true,
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun MemberProfilePendingRemovalPreview() {
    TDTheme {
        MemberProfileContent(
            uiState =
            MemberProfileContract.UiState.Success(
                member =
                MemberUiItem(
                    userId = 1,
                    firstName = "John",
                    lastName = "Doe",
                    email = "john@example.com",
                    avatarUrl = null,
                    role = "MEMBER",
                    joinedAt = "Jan 12, 2024",
                    displayName = "John Doe",
                    initials = "JD",
                ),
                pendingRemoval = true,
            ),
            onAction = {},
        )
    }
}

private fun sampleMember() = MemberUiItem(
    userId = 1,
    firstName = "John",
    lastName = "Doe",
    email = "john@example.com",
    avatarUrl = null,
    role = "MEMBER",
    joinedAt = "Jan 12, 2024",
    displayName = "John Doe",
    initials = "JD",
)

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun MemberProfileAdminPreview() {
    TDTheme {
        MemberProfileContent(
            uiState = MemberProfileContract.UiState.Success(member = sampleMember(), isCurrentUserAdmin = true),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun MemberProfileBlockedPreview() {
    TDTheme {
        MemberProfileContent(
            uiState = MemberProfileContract.UiState.Success(member = sampleMember(), isBlocked = true),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun MemberProfileReportDialogPreview() {
    TDTheme {
        MemberProfileContent(
            uiState = MemberProfileContract.UiState.Success(member = sampleMember(), showReportDialog = true),
            onAction = {},
        )
    }
}
