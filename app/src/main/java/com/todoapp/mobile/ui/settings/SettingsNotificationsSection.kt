package com.todoapp.mobile.ui.settings

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
import com.todoapp.uikit.components.TDSettingsGroup
import com.todoapp.uikit.components.TDSettingsItem
import com.todoapp.uikit.components.TDSwitch
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

@RequiresApi(Build.VERSION_CODES.S)
@Composable
internal fun SettingsNotificationsSection(
    isUserAuthenticated: Boolean,
    pushNotificationsEnabled: Boolean,
    isPushTogglePending: Boolean,
    onAction: (UiAction) -> Unit,
) {
    TDSettingsGroup(title = stringResource(R.string.settings_section_notifications)) {
        if (isUserAuthenticated) {
            TDSettingsItem(
                title = stringResource(R.string.settings_push_notifications),
                icon = tdPainter(com.example.uikit.R.drawable.ic_notification),
                iconTint = TDTheme.colors.darkPending,
                iconContainerColor = TDTheme.colors.lightPending,
                trailingContent = {
                    TDSwitch(
                        checked = pushNotificationsEnabled,
                        onCheckedChange = { onAction(UiAction.OnPushNotificationsToggle(it)) },
                        enabled = !isPushTogglePending,
                    )
                },
            )
        }
        SettingsExactAlarmsRow()
        TDSettingsItem(
            title = stringResource(R.string.alarm_sounds),
            icon = tdPainter(R.drawable.ic_settings_sound),
            iconTint = TDTheme.colors.darkPending,
            iconContainerColor = TDTheme.colors.lightPending,
            onClick = { onAction(UiAction.OnNavigateToAlarmSounds) },
        )
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun SettingsExactAlarmsRow() {
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

@RequiresApi(Build.VERSION_CODES.S)
@TDPreview
@Composable
private fun SettingsNotificationsSectionPreview() {
    TDTheme {
        Column(modifier = Modifier.background(TDTheme.colors.background).padding(16.dp)) {
            SettingsNotificationsSection(
                isUserAuthenticated = true,
                pushNotificationsEnabled = true,
                isPushTogglePending = false,
                onAction = {},
            )
        }
    }
}
