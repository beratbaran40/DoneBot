package com.todoapp.mobile.ui.settings

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.settings.SettingsContract.UiAction
import com.todoapp.mobile.ui.settings.SettingsContract.UiState
import com.todoapp.uikit.components.TDSettingsGroup
import com.todoapp.uikit.components.TDSettingsItem
import com.todoapp.uikit.components.TDSwitch
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

/**
 * Everything notification-related, on its own screen.
 *
 * It lives apart from Settings for two reasons. Density: with the per-type switches this was nine
 * rows and six switches inline, the largest block in the app by some way. And kind: the top group is
 * device-local — alarms this app schedules itself, working offline and without an account — while
 * the bottom group is server-side push, which needs a session and a network. Sitting in one list
 * they read as one mechanism, which is exactly the confusion that had a user muting "push" and still
 * being woken by a local reminder.
 */
@Composable
fun NotificationSettingsScreen(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(8.dp))

        TDSettingsGroup(title = stringResource(R.string.settings_notifications_group_device)) {
            TDSettingsItem(
                title = stringResource(R.string.settings_daily_plan_reminder),
                subtitle = stringResource(R.string.settings_daily_plan_reminder_subtitle),
                icon = tdPainter(com.example.uikit.R.drawable.ic_sand_clock),
                iconTint = TDTheme.colors.darkPending,
                iconContainerColor = TDTheme.colors.lightPending,
                trailingContent = {
                    TDSwitch(
                        checked = uiState.dailyPlanEnabled,
                        onCheckedChange = { onAction(UiAction.OnDailyPlanToggle(it)) },
                    )
                },
            )
            // API 31 is where canScheduleExactAlarms() exists; below it the permission cannot be
            // revoked, so there is nothing to show and calling the method would throw.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ExactAlarmsRow()
            }
            TDSettingsItem(
                title = stringResource(R.string.alarm_sounds),
                icon = tdPainter(R.drawable.ic_settings_sound),
                iconTint = TDTheme.colors.darkPending,
                iconContainerColor = TDTheme.colors.lightPending,
                onClick = { onAction(UiAction.OnNavigateToAlarmSounds) },
            )
        }

        if (uiState.isUserAuthenticated) {
            Spacer(Modifier.height(16.dp))
            TDSettingsGroup(title = stringResource(R.string.settings_notifications_group_push)) {
                TDSettingsItem(
                    title = stringResource(R.string.settings_push_notifications),
                    icon = tdPainter(com.example.uikit.R.drawable.ic_notification),
                    iconTint = TDTheme.colors.darkPending,
                    iconContainerColor = TDTheme.colors.lightPending,
                    trailingContent = {
                        TDSwitch(
                            checked = uiState.pushNotificationsEnabled,
                            onCheckedChange = { onAction(UiAction.OnPushNotificationsToggle(it)) },
                            enabled = !uiState.isPushTogglePending,
                        )
                    },
                )
                // Only meaningful while push is on at all, so they follow the master switch rather
                // than sitting there greyed out.
                if (uiState.pushNotificationsEnabled) {
                    PUSH_TYPE_GROUPS.forEach { group ->
                        val muted = group.types.all { it in uiState.mutedPushTypes }
                        TDSettingsItem(
                            title = stringResource(group.titleRes),
                            subtitle = stringResource(R.string.settings_push_type_subtitle),
                            icon = tdPainter(com.example.uikit.R.drawable.ic_notification),
                            iconTint = TDTheme.colors.pendingGray,
                            iconContainerColor = TDTheme.colors.lightPending,
                            trailingContent = {
                                TDSwitch(
                                    checked = !muted,
                                    onCheckedChange = { enabled ->
                                        group.types.forEach { onAction(UiAction.OnPushTypeToggle(it, enabled)) }
                                    },
                                    enabled = !uiState.isPushTogglePending,
                                )
                            },
                        )
                    }
                }
            }
            // Every one of these only ever fires for a shared task — personal reminders are the local
            // alarms in the group above. Said plainly here so someone with no groups understands why
            // the block does nothing for them, instead of hiding it and leaving them to wonder.
            TDText(
                text = stringResource(R.string.settings_push_group_scope_note),
                style = TDTheme.typography.subheading2,
                color = TDTheme.colors.gray,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * The groups the per-type mutes are offered under. Deliberately coarser than
 * [com.todoapp.mobile.domain.model.NotificationType]: the three invitation events are one idea to a
 * user, and a settings screen mirroring an enum asks them to reason about our data model.
 */
private val PUSH_TYPE_GROUPS = listOf(
    PushTypeGroup(R.string.settings_push_type_assignments, listOf("TASK_ASSIGNED")),
    PushTypeGroup(R.string.settings_push_type_completions, listOf("TASK_COMPLETED")),
    PushTypeGroup(R.string.settings_push_type_due_soon, listOf("TASK_DUE_SOON")),
    PushTypeGroup(
        R.string.settings_push_type_invitations,
        listOf("INVITATION_RECEIVED", "INVITATION_ACCEPTED", "INVITATION_DECLINED", "GROUP_OWNERSHIP_TRANSFERRED"),
    ),
)

private data class PushTypeGroup(val titleRes: Int, val types: List<String>)

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

    val subtitle = if (canScheduleExact) {
        stringResource(R.string.settings_exact_alarms_status_enabled)
    } else {
        stringResource(R.string.settings_exact_alarms_description)
    }
    val openExactAlarmSettings: (() -> Unit)? = if (canScheduleExact) {
        null
    } else {
        {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(intent) }
        }
    }

    TDSettingsItem(
        title = stringResource(R.string.settings_exact_alarms_title),
        subtitle = subtitle,
        icon = tdPainter(com.example.uikit.R.drawable.ic_clock),
        iconTint = TDTheme.colors.darkPending,
        iconContainerColor = TDTheme.colors.lightPending,
        onClick = openExactAlarmSettings,
    )
}

@TDPreview
@Composable
private fun NotificationSettingsAllOnPreview() {
    TDTheme {
        NotificationSettingsScreen(
            uiState = SettingsPreviewData.authenticatedState(),
            onAction = {},
            modifier = Modifier.background(TDTheme.colors.background),
        )
    }
}

@TDPreview
@Composable
private fun NotificationSettingsSomeMutedPreview() {
    TDTheme {
        NotificationSettingsScreen(
            uiState = SettingsPreviewData.authenticatedState().copy(
                mutedPushTypes = setOf("TASK_COMPLETED"),
                dailyPlanEnabled = false,
            ),
            onAction = {},
            modifier = Modifier.background(TDTheme.colors.background),
        )
    }
}

@TDPreview
@Composable
private fun NotificationSettingsPushOffPreview() {
    TDTheme {
        NotificationSettingsScreen(
            uiState = SettingsPreviewData.authenticatedState().copy(pushNotificationsEnabled = false),
            onAction = {},
            modifier = Modifier.background(TDTheme.colors.background),
        )
    }
}

@TDPreview
@Composable
private fun NotificationSettingsSignedOutPreview() {
    TDTheme {
        NotificationSettingsScreen(
            uiState = SettingsPreviewData.guestState(),
            onAction = {},
            modifier = Modifier.background(TDTheme.colors.background),
        )
    }
}
