package com.todoapp.mobile.ui.settings

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.todoapp.mobile.BuildConfig
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.model.ThemePreference
import com.todoapp.mobile.domain.security.SecretModeReopenOptions
import com.todoapp.mobile.ui.permissions.NotificationPermissionPrompt
import com.todoapp.mobile.ui.permissions.OverlayPermissionPrompt
import com.todoapp.mobile.ui.settings.SettingsContract.UiAction
import com.todoapp.mobile.ui.settings.SettingsContract.UiState
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.theme.TDTheme
import java.time.LocalTime

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun SettingsScreen(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
    onCheckPermissions: () -> Unit,
    onDismissPermission: (PermissionType) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            onCheckPermissions()
        }
    }

    SettingsContent(
        uiState = uiState,
        onAction = onAction,
        onDismissPermission = onDismissPermission,
    )
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun SettingsContent(
    modifier: Modifier = Modifier,
    uiState: UiState,
    onAction: (UiAction) -> Unit,
    onDismissPermission: (PermissionType) -> Unit,
) {
    if (uiState.showDeleteAccountDialog) {
        DeleteAccountDialog(
            isDeleting = uiState.isDeletingAccount,
            onDismiss = { onAction(UiAction.OnDeleteAccountDismiss) },
            onConfirm = { onAction(UiAction.OnDeleteAccountConfirm) },
        )
    }

    if (uiState.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { onAction(UiAction.OnLogoutDismiss) },
            title = {
                TDText(
                    text = stringResource(R.string.logout_dialog_title),
                    style = TDTheme.typography.heading5,
                    color = TDTheme.colors.onBackground,
                )
            },
            text = {
                TDText(
                    text = stringResource(R.string.logout_dialog_message),
                    style = TDTheme.typography.subheading1,
                    color = TDTheme.colors.onBackground,
                )
            },
            confirmButton = {
                TextButton(onClick = { onAction(UiAction.OnLogoutConfirm) }) {
                    TDText(
                        text = stringResource(R.string.logout),
                        style = TDTheme.typography.subheading1,
                        color = TDTheme.colors.crossRed,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(UiAction.OnLogoutDismiss) }) {
                    TDText(
                        text = stringResource(R.string.cancel),
                        style = TDTheme.typography.subheading1,
                        color = TDTheme.colors.onBackground,
                    )
                }
            },
            containerColor = TDTheme.colors.surface,
        )
    }

    val context = LocalContext.current

    Column(
        modifier =
        modifier
            .fillMaxSize()
            .background(TDTheme.colors.background)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        PermissionPager(
            permissions = uiState.visiblePermissions,
            onDismiss = onDismissPermission,
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.isUserAuthenticated) {
            ProfileRow(
                displayName = uiState.displayName,
                email = uiState.email,
                avatarUrl = uiState.avatarUrl,
                avatarVersion = uiState.avatarVersion,
                onClick = { onAction(UiAction.OnNavigateToProfile) },
            )
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onAction(UiAction.OnLoginOrRegisterClick) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TDText(
                    text = stringResource(R.string.login_or_create_account),
                    style = TDTheme.typography.heading6,
                    color = TDTheme.colors.darkPending,
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    painter = painterResource(com.example.uikit.R.drawable.ic_arrow_forward),
                    contentDescription = null,
                    tint = TDTheme.colors.darkPending,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider(color = TDTheme.colors.onBackground.copy(alpha = 0.1f))

        SectionHeader(R.string.settings_section_personalization)

        ThemeSelector(
            currentTheme = uiState.currentTheme,
            onThemeChange = { theme ->
                onAction(UiAction.OnThemeChange(theme))
            },
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = TDTheme.colors.onBackground.copy(alpha = 0.1f))
        Spacer(modifier = Modifier.height(16.dp))

        LanguageSelector(
            currentLanguage = uiState.currentLanguage,
            onLanguageChange = { language ->
                onAction(UiAction.OnLanguageChange(language))
            },
        )

        SectionHeader(R.string.settings_section_notifications)

        if (uiState.isUserAuthenticated) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TDText(
                    text = stringResource(R.string.settings_push_notifications),
                    style = TDTheme.typography.heading6,
                    color = TDTheme.colors.onBackground,
                )
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = uiState.pushNotificationsEnabled,
                    onCheckedChange = { onAction(UiAction.OnPushNotificationsToggle(it)) },
                    enabled = !uiState.isPushTogglePending,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TDTheme.colors.white,
                        checkedTrackColor = TDTheme.colors.pendingGray,
                    ),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = TDTheme.colors.onBackground.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))
        }

        ExactAlarmsRow()
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = TDTheme.colors.onBackground.copy(alpha = 0.1f))
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onAction(UiAction.OnNavigateToAlarmSounds) },
        ) {
            TDText(
                text = stringResource(R.string.alarm_sounds),
                style = TDTheme.typography.heading6,
                color = TDTheme.colors.onBackground,
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(com.example.uikit.R.drawable.ic_arrow_forward),
                contentDescription = null,
                tint = TDTheme.colors.onBackground,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = TDTheme.colors.onBackground.copy(alpha = 0.1f))
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onAction(UiAction.OnNavigateToPlanYourDay) },
        ) {
            TDText(
                text = stringResource(R.string.plan_your_day),
                style = TDTheme.typography.heading6,
                color = TDTheme.colors.onBackground,
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(com.example.uikit.R.drawable.ic_arrow_forward),
                contentDescription = null,
                tint = TDTheme.colors.onBackground,
            )
        }

        SectionHeader(R.string.settings_section_productivity)

        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onAction(UiAction.OnNavigateToPomodoroSettings) },
        ) {
            TDText(
                text = stringResource(R.string.pomodoro_configure_timer),
                style = TDTheme.typography.heading6,
                color = TDTheme.colors.onBackground,
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(com.example.uikit.R.drawable.ic_arrow_forward),
                contentDescription = null,
                tint = TDTheme.colors.onBackground,
            )
        }

        SectionHeader(R.string.settings_section_privacy_security)

        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onAction(UiAction.OnNavigateToSecretModeSettings) },
        ) {
            TDText(
                text = stringResource(R.string.privacy_security),
                style = TDTheme.typography.heading6,
                color = TDTheme.colors.onBackground,
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(com.example.uikit.R.drawable.ic_arrow_forward),
                contentDescription = null,
                tint = TDTheme.colors.onBackground,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = TDTheme.colors.onBackground.copy(alpha = 0.1f))
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                TDText(
                    text = stringResource(R.string.settings_journal_biometric_protection_title),
                    style = TDTheme.typography.heading6,
                    color = TDTheme.colors.onBackground,
                )
                TDText(
                    text = stringResource(R.string.settings_journal_biometric_protection_description),
                    style = TDTheme.typography.subheading2,
                    color = TDTheme.colors.gray,
                )
            }
            Switch(
                checked = uiState.journalBiometricProtected,
                onCheckedChange = { onAction(UiAction.OnJournalBiometricProtectionToggle(it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TDTheme.colors.white,
                    checkedTrackColor = TDTheme.colors.pendingGray,
                ),
            )
        }

        SectionHeader(R.string.settings_section_accessibility)

        Row(
            Modifier
                .fillMaxWidth()
                .clickable {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                },
        ) {
            TDText(
                text = stringResource(R.string.settings_open_system_a11y),
                style = TDTheme.typography.heading6,
                color = TDTheme.colors.onBackground,
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(com.example.uikit.R.drawable.ic_arrow_forward),
                contentDescription = null,
                tint = TDTheme.colors.onBackground,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = TDTheme.colors.onBackground.copy(alpha = 0.1f))
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                TDText(
                    text = stringResource(R.string.settings_reduce_motion),
                    style = TDTheme.typography.heading6,
                    color = TDTheme.colors.onBackground,
                )
                TDText(
                    text = stringResource(R.string.settings_reduce_motion_desc),
                    style = TDTheme.typography.subheading2,
                    color = TDTheme.colors.gray,
                )
            }
            Switch(
                checked = uiState.reduceMotionEnabled,
                onCheckedChange = { onAction(UiAction.OnReduceMotionToggle(it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TDTheme.colors.white,
                    checkedTrackColor = TDTheme.colors.pendingGray,
                ),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = TDTheme.colors.onBackground.copy(alpha = 0.1f))
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .clickable {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_DISPLAY_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                TDText(
                    text = stringResource(R.string.settings_larger_text),
                    style = TDTheme.typography.heading6,
                    color = TDTheme.colors.onBackground,
                )
                TDText(
                    text = stringResource(R.string.settings_larger_text_desc),
                    style = TDTheme.typography.subheading2,
                    color = TDTheme.colors.gray,
                )
            }
            Icon(
                painter = painterResource(com.example.uikit.R.drawable.ic_arrow_forward),
                contentDescription = null,
                tint = TDTheme.colors.onBackground,
            )
        }

        SectionHeader(R.string.settings_section_about)

        Row(
            Modifier
                .fillMaxWidth()
                .clickable {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PRIVACY_POLICY_URL))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                },
        ) {
            TDText(
                text = stringResource(R.string.settings_privacy_policy),
                style = TDTheme.typography.heading6,
                color = TDTheme.colors.onBackground,
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(com.example.uikit.R.drawable.ic_arrow_forward),
                contentDescription = null,
                tint = TDTheme.colors.onBackground,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = TDTheme.colors.onBackground.copy(alpha = 0.1f))
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .clickable {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.TERMS_OF_SERVICE_URL))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                },
        ) {
            TDText(
                text = stringResource(R.string.settings_terms_of_service),
                style = TDTheme.typography.heading6,
                color = TDTheme.colors.onBackground,
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(com.example.uikit.R.drawable.ic_arrow_forward),
                contentDescription = null,
                tint = TDTheme.colors.onBackground,
            )
        }

        if (uiState.isUserAuthenticated) {
            SectionHeader(R.string.settings_section_account)

            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onAction(UiAction.OnLogoutClick) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TDText(
                    text = stringResource(R.string.logout),
                    style = TDTheme.typography.heading6,
                    color = TDTheme.colors.crossRed,
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    painter = painterResource(R.drawable.ic_logout),
                    contentDescription = null,
                    tint = TDTheme.colors.crossRed,
                )
            }

            SectionHeader(R.string.settings_section_danger_zone)

            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !uiState.isDeletingAccount) {
                        onAction(UiAction.OnDeleteAccountClick)
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TDText(
                    text = stringResource(R.string.settings_delete_account),
                    style = TDTheme.typography.heading6,
                    color = TDTheme.colors.crossRed,
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ProfileRow(
    displayName: String,
    email: String,
    avatarUrl: String?,
    avatarVersion: Long,
    onClick: () -> Unit,
) {
    val initials = remember(displayName, email) {
        val source = displayName.trim().ifEmpty { email.trim() }
        if (source.isBlank()) {
            "?"
        } else {
            source.split(' ', '.', '@')
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.first().uppercase() }
                .ifEmpty { source.first().uppercase() }
        }
    }
    val nameLine = displayName.ifBlank { email.substringBefore('@', missingDelimiterValue = email) }
    val subtitle = email.ifBlank { stringResource(R.string.settings_profile_subtitle_no_email) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        com.todoapp.mobile.ui.groups.groupdetail.MemberAvatar(
            initials = initials,
            size = 48,
            avatarUrl = avatarUrl,
            avatarVersion = avatarVersion,
        )
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            TDText(
                text = nameLine,
                style = TDTheme.typography.heading5,
                color = TDTheme.colors.onBackground,
            )
            TDText(
                text = subtitle,
                style = TDTheme.typography.subheading2,
                color = TDTheme.colors.gray,
            )
        }
        Icon(
            painter = painterResource(com.example.uikit.R.drawable.ic_arrow_forward),
            contentDescription = null,
            tint = TDTheme.colors.onBackground,
        )
    }
}

@Composable
private fun SectionHeader(@StringRes titleRes: Int) {
    Spacer(modifier = Modifier.height(20.dp))
    TDText(
        text = stringResource(titleRes),
        style = TDTheme.typography.heading7,
        color = TDTheme.colors.gray,
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun ExactAlarmsRow() {
    val context = LocalContext.current
    val alarmManager = remember(context) { context.getSystemService(AlarmManager::class.java) }
    var canScheduleExact by remember {
        mutableStateOf(alarmManager?.canScheduleExactAlarms() == true)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            canScheduleExact = alarmManager?.canScheduleExactAlarms() == true
        }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = !canScheduleExact) {
                val intent =
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                runCatching { context.startActivity(intent) }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            TDText(
                text = stringResource(R.string.settings_exact_alarms_title),
                style = TDTheme.typography.heading6,
                color = TDTheme.colors.onBackground,
            )
            TDText(
                text =
                if (canScheduleExact) {
                    stringResource(R.string.settings_exact_alarms_status_enabled)
                } else {
                    stringResource(R.string.settings_exact_alarms_description)
                },
                style = TDTheme.typography.subheading3,
                color = TDTheme.colors.gray,
            )
        }
        if (!canScheduleExact) {
            Icon(
                painter = painterResource(com.example.uikit.R.drawable.ic_arrow_forward),
                contentDescription = null,
                tint = TDTheme.colors.onBackground,
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun PermissionPager(
    permissions: List<PermissionType>,
    onDismiss: (PermissionType) -> Unit,
) {
    if (permissions.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { permissions.size })

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { index ->
            when (permissions[index]) {
                PermissionType.NOTIFICATION ->
                    NotificationPermissionPrompt(
                        onGranted = { onDismiss(PermissionType.NOTIFICATION) },
                        onDismiss = { onDismiss(PermissionType.NOTIFICATION) },
                    )

                PermissionType.OVERLAY ->
                    OverlayPermissionPrompt(
                        onGranted = { onDismiss(PermissionType.OVERLAY) },
                        onDismiss = { onDismiss(PermissionType.OVERLAY) },
                    )
            }
        }

        if (permissions.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                repeat(permissions.size) { index ->
                    Box(
                        modifier =
                        Modifier
                            .padding(horizontal = 4.dp)
                            .size(8.dp)
                            .background(
                                color =
                                if (pagerState.currentPage == index) {
                                    TDTheme.colors.pendingGray
                                } else {
                                    TDTheme.colors.onBackground.copy(alpha = 0.3f)
                                },
                                shape = CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteAccountDialog(
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val reduceMotion = com.todoapp.mobile.ui.common.LocalReduceMotion.current
    com.todoapp.uikit.components.TDGoodbyeDialog(
        speechBubbleText = stringResource(R.string.delete_account_speech_bubble),
        legalDetailText = stringResource(R.string.delete_account_dialog_message),
        typedConfirmLabel = stringResource(R.string.delete_account_typed_confirm_label),
        typedConfirmWord = stringResource(R.string.delete_account_typed_confirm_word),
        confirmButtonText = stringResource(R.string.delete_account_button),
        dismissButtonText = stringResource(R.string.cancel),
        inProgressText = stringResource(R.string.delete_account_in_progress),
        isProcessing = isDeleting,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        reduceMotion = reduceMotion,
    )
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@com.todoapp.uikit.previews.TDPreview
@Composable
private fun SettingsScreenPreview() {
    TDTheme {
        SettingsScreen(
            uiState =
            UiState(
                currentTheme = ThemePreference.SYSTEM_DEFAULT,
                selectedSecretMode = SecretModeReopenOptions.Immediate,
                remainedSecretModeTime = "",
                isSecretModeActive = true,
                dailyPlanTime = LocalTime.of(9, 0),
                visiblePermissions = listOf(PermissionType.OVERLAY, PermissionType.NOTIFICATION),
            ),
            onAction = {},
            onCheckPermissions = {},
            onDismissPermission = {},
        )
    }
}
