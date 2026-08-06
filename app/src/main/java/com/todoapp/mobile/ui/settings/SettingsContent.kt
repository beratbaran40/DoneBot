package com.todoapp.mobile.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.BuildConfig
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.common.rememberSendFeedback
import com.todoapp.mobile.ui.permissions.rememberCameraPermissionRequest
import com.todoapp.mobile.ui.settings.SettingsContract.UiAction
import com.todoapp.mobile.ui.settings.SettingsContract.UiState
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

@Composable
internal fun SettingsContent(
    modifier: Modifier = Modifier,
    uiState: UiState,
    onAction: (UiAction) -> Unit,
    onCheckPermissions: () -> Unit,
    onDismissPermission: (PermissionType) -> Unit,
) {
    if (uiState.showDeleteAccountDialog) {
        SettingsDeleteAccountDialog(
            isDeleting = uiState.isDeletingAccount,
            onDismiss = { onAction(UiAction.OnDeleteAccountDismiss) },
            onConfirm = { onAction(UiAction.OnDeleteAccountConfirm) },
        )
    }

    if (uiState.showLogoutDialog) {
        SettingsLogoutDialog(
            onConfirm = { onAction(UiAction.OnLogoutConfirm) },
            onDismiss = { onAction(UiAction.OnLogoutDismiss) },
        )
    }

    val requestCameraPermission = rememberCameraPermissionRequest(onGranted = { onCheckPermissions() })
    val onSendFeedback = rememberSendFeedback(userEmail = uiState.email)

    Column(
        modifier =
        modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        SettingsPermissionPager(
            permissions = uiState.visiblePermissions,
            onDismiss = onDismissPermission,
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.isUserAuthenticated) {
            SettingsProfileCard(
                displayName = uiState.displayName,
                email = uiState.email,
                avatarUrl = uiState.avatarUrl,
                avatarVersion = uiState.avatarVersion,
                onClick = { onAction(UiAction.OnNavigateToProfile) },
            )
        } else {
            SettingsLoginCard(onClick = { onAction(UiAction.OnLoginOrRegisterClick) })
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsPersonalizationSection(
            currentTheme = uiState.currentTheme,
            currentLanguage = uiState.currentLanguage,
            onAction = onAction,
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsNotificationsSection(
            isUserAuthenticated = uiState.isUserAuthenticated,
            pushNotificationsEnabled = uiState.pushNotificationsEnabled,
            isPushTogglePending = uiState.isPushTogglePending,
            mutedPushTypes = uiState.mutedPushTypes,
            dailyPlanEnabled = uiState.dailyPlanEnabled,
            onAction = onAction,
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsProductivitySection(onAction = onAction)

        Spacer(modifier = Modifier.height(16.dp))

        SettingsPrivacySection(
            journalBiometricProtected = uiState.journalBiometricProtected,
            cameraGranted = uiState.cameraGranted,
            onRequestCameraPermission = { requestCameraPermission() },
            onAction = onAction,
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsDataSection(
            sharePerformanceDiagnostics = uiState.sharePerformanceDiagnostics,
            crashAnalyticsEnabled = uiState.crashAnalyticsEnabled,
            onAction = onAction,
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsAccessibilitySection(
            reduceMotionEnabled = uiState.reduceMotionEnabled,
            onAction = onAction,
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsAboutSection(
            onSendFeedback = onSendFeedback,
            onAction = onAction,
        )

        if (uiState.isUserAuthenticated) {
            Spacer(modifier = Modifier.height(16.dp))
            SettingsAccountSections(
                isDeletingAccount = uiState.isDeletingAccount,
                onAction = onAction,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            TDText(
                text = if (BuildConfig.DEBUG) {
                    stringResource(R.string.settings_build_debug_badge)
                } else {
                    BuildConfig.VERSION_NAME
                },
                style = TDTheme.typography.subheading2,
                color = if (BuildConfig.DEBUG) TDTheme.colors.orange else TDTheme.colors.gray,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@TDPreview
@Composable
private fun SettingsContentAuthenticatedPreview() {
    TDTheme {
        SettingsContent(
            uiState = SettingsPreviewData.authenticatedState(),
            onAction = {},
            onCheckPermissions = {},
            onDismissPermission = {},
        )
    }
}

@TDPreview
@Composable
private fun SettingsContentGuestPreview() {
    TDTheme {
        SettingsContent(
            uiState = SettingsPreviewData.guestState(),
            onAction = {},
            onCheckPermissions = {},
            onDismissPermission = {},
        )
    }
}

@TDPreview
@Composable
private fun SettingsContentPermissionsPreview() {
    TDTheme {
        SettingsContent(
            uiState = SettingsPreviewData.permissionsState(),
            onAction = {},
            onCheckPermissions = {},
            onDismissPermission = {},
        )
    }
}
