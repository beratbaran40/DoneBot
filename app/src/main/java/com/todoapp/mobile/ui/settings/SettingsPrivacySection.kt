package com.todoapp.mobile.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.common.openAppDetailsSettings
import com.todoapp.mobile.ui.settings.SettingsContract.UiAction
import com.todoapp.uikit.components.TDSettingsGroup
import com.todoapp.uikit.components.TDSettingsItem
import com.todoapp.uikit.components.TDSwitch
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

@Composable
internal fun SettingsPrivacySection(
    journalBiometricProtected: Boolean,
    cameraGranted: Boolean,
    onRequestCameraPermission: () -> Unit,
    onAction: (UiAction) -> Unit,
) {
    val context = LocalContext.current
    TDSettingsGroup(title = stringResource(R.string.settings_section_privacy_security)) {
        TDSettingsItem(
            title = stringResource(R.string.privacy_security),
            icon = tdPainter(R.drawable.ic_secret_mode),
            iconTint = TDTheme.colors.purple,
            iconContainerColor = TDTheme.colors.purpleContainer,
            onClick = { onAction(UiAction.OnNavigateToSecretModeSettings) },
        )
        TDSettingsItem(
            title = stringResource(R.string.settings_blocked_users),
            icon = tdPainter(R.drawable.ic_settings_block),
            iconTint = TDTheme.colors.purple,
            iconContainerColor = TDTheme.colors.purpleContainer,
            onClick = { onAction(UiAction.OnNavigateToBlockedUsers) },
        )
        TDSettingsItem(
            title = stringResource(R.string.settings_journal_biometric_protection_title),
            subtitle = stringResource(R.string.settings_journal_biometric_protection_description),
            icon = tdPainter(R.drawable.ic_settings_fingerprint),
            iconTint = TDTheme.colors.purple,
            iconContainerColor = TDTheme.colors.purpleContainer,
            trailingContent = {
                TDSwitch(
                    checked = journalBiometricProtected,
                    onCheckedChange = { onAction(UiAction.OnJournalBiometricProtectionToggle(it)) },
                )
            },
        )
        TDSettingsItem(
            title = stringResource(R.string.settings_camera_permission_title),
            subtitle = stringResource(R.string.settings_camera_permission_description),
            icon = tdPainter(R.drawable.ic_settings_camera),
            iconTint = TDTheme.colors.purple,
            iconContainerColor = TDTheme.colors.purpleContainer,
            trailingContent = {
                TDSwitch(
                    checked = cameraGranted,
                    onCheckedChange = {
                        if (cameraGranted) {
                            // Camera permission can't be revoked programmatically — send to App Settings.
                            context.openAppDetailsSettings()
                        } else {
                            onRequestCameraPermission()
                        }
                    },
                )
            },
        )
    }
}

@Composable
internal fun SettingsDataSection(
    sharePerformanceDiagnostics: Boolean,
    crashAnalyticsEnabled: Boolean,
    onAction: (UiAction) -> Unit,
) {
    TDSettingsGroup(title = stringResource(R.string.settings_section_data)) {
        TDSettingsItem(
            title = stringResource(R.string.settings_download_data_title),
            subtitle = stringResource(R.string.settings_download_data_description),
            icon = tdPainter(R.drawable.ic_settings_download),
            iconTint = TDTheme.colors.darkGreen,
            iconContainerColor = TDTheme.colors.lightGreen,
            onClick = { onAction(UiAction.OnDownloadDataClick) },
        )
        TDSettingsItem(
            title = stringResource(R.string.settings_share_performance_diagnostics_title),
            subtitle = stringResource(R.string.settings_share_performance_diagnostics_description),
            icon = tdPainter(R.drawable.ic_settings_diagnostics),
            iconTint = TDTheme.colors.darkGreen,
            iconContainerColor = TDTheme.colors.lightGreen,
            trailingContent = {
                TDSwitch(
                    checked = sharePerformanceDiagnostics,
                    onCheckedChange = { onAction(UiAction.OnSharePerformanceDiagnosticsToggle(it)) },
                )
            },
        )
        TDSettingsItem(
            title = stringResource(R.string.settings_crash_analytics_title),
            subtitle = stringResource(R.string.settings_crash_analytics_description),
            icon = tdPainter(R.drawable.ic_settings_bug),
            iconTint = TDTheme.colors.darkGreen,
            iconContainerColor = TDTheme.colors.lightGreen,
            trailingContent = {
                TDSwitch(
                    checked = crashAnalyticsEnabled,
                    onCheckedChange = { onAction(UiAction.OnCrashAnalyticsToggle(it)) },
                )
            },
        )
    }
}

@TDPreview
@Composable
private fun SettingsPrivacySectionPreview() {
    TDTheme {
        Column(modifier = Modifier.background(TDTheme.colors.background).padding(16.dp)) {
            SettingsPrivacySection(
                journalBiometricProtected = true,
                cameraGranted = false,
                onRequestCameraPermission = {},
                onAction = {},
            )
        }
    }
}

@TDPreview
@Composable
private fun SettingsDataSectionPreview() {
    TDTheme {
        Column(modifier = Modifier.background(TDTheme.colors.background).padding(16.dp)) {
            SettingsDataSection(
                sharePerformanceDiagnostics = false,
                crashAnalyticsEnabled = true,
                onAction = {},
            )
        }
    }
}
