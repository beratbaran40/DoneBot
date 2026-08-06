package com.todoapp.mobile.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.settings.SettingsContract.UiAction
import com.todoapp.uikit.components.TDSettingsGroup
import com.todoapp.uikit.components.TDSettingsItem
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

/**
 * One row into [NotificationSettingsScreen]. It briefly held the whole thing — nine rows and six
 * switches, the largest block in Settings — which buried the two controls people actually look for
 * (the daily reminder and the alarm sound) under a wall of per-type toggles.
 *
 * The subtitle carries the state so the destination is not a black box: the point of a summary row
 * is that you can tell at a glance whether you need to open it.
 */
@Composable
internal fun SettingsNotificationsSection(
    isUserAuthenticated: Boolean,
    pushNotificationsEnabled: Boolean,
    mutedPushTypes: Set<String>,
    dailyPlanEnabled: Boolean,
    onAction: (UiAction) -> Unit,
) {
    TDSettingsGroup(title = stringResource(R.string.settings_section_notifications)) {
        TDSettingsItem(
            title = stringResource(R.string.settings_notifications_entry_title),
            subtitle = notificationsSummary(
                isUserAuthenticated = isUserAuthenticated,
                pushNotificationsEnabled = pushNotificationsEnabled,
                mutedPushTypes = mutedPushTypes,
                dailyPlanEnabled = dailyPlanEnabled,
            ),
            icon = tdPainter(com.example.uikit.R.drawable.ic_notification),
            iconTint = TDTheme.colors.darkPending,
            iconContainerColor = TDTheme.colors.lightPending,
            onClick = { onAction(UiAction.OnNavigateToNotificationSettings) },
        )
    }
}

/**
 * "Push notifications off" beats "4 turned off" when the master switch is the reason — one cause,
 * stated once, instead of a count the user then has to explain to themselves.
 */
@Composable
private fun notificationsSummary(
    isUserAuthenticated: Boolean,
    pushNotificationsEnabled: Boolean,
    mutedPushTypes: Set<String>,
    dailyPlanEnabled: Boolean,
): String {
    if (isUserAuthenticated && !pushNotificationsEnabled) {
        return stringResource(R.string.settings_notifications_summary_push_off)
    }
    val mutedTypeGroups = if (isUserAuthenticated) {
        PUSH_TYPE_GROUP_KEYS.count { types -> types.all { it in mutedPushTypes } }
    } else {
        0
    }
    val offCount = mutedTypeGroups + if (dailyPlanEnabled) 0 else 1
    return if (offCount == 0) {
        stringResource(R.string.settings_notifications_summary_all_on)
    } else {
        pluralStringResource(R.plurals.settings_notifications_summary_off, offCount, offCount)
    }
}

/** Mirrors the grouping in [NotificationSettingsScreen] so the count matches what the screen shows. */
private val PUSH_TYPE_GROUP_KEYS = listOf(
    listOf("TASK_ASSIGNED"),
    listOf("TASK_COMPLETED"),
    listOf("TASK_DUE_SOON"),
    listOf("INVITATION_RECEIVED", "INVITATION_ACCEPTED", "INVITATION_DECLINED", "GROUP_OWNERSHIP_TRANSFERRED"),
)

@TDPreview
@Composable
private fun SettingsNotificationsSectionAllOnPreview() {
    TDTheme {
        Column(modifier = Modifier.background(TDTheme.colors.background).padding(16.dp)) {
            SettingsNotificationsSection(
                isUserAuthenticated = true,
                pushNotificationsEnabled = true,
                mutedPushTypes = emptySet(),
                dailyPlanEnabled = true,
                onAction = {},
            )
        }
    }
}

@TDPreview
@Composable
private fun SettingsNotificationsSectionSomeOffPreview() {
    TDTheme {
        Column(modifier = Modifier.background(TDTheme.colors.background).padding(16.dp)) {
            SettingsNotificationsSection(
                isUserAuthenticated = true,
                pushNotificationsEnabled = true,
                mutedPushTypes = setOf("TASK_COMPLETED"),
                dailyPlanEnabled = false,
                onAction = {},
            )
        }
    }
}

@TDPreview
@Composable
private fun SettingsNotificationsSectionPushOffPreview() {
    TDTheme {
        Column(modifier = Modifier.background(TDTheme.colors.background).padding(16.dp)) {
            SettingsNotificationsSection(
                isUserAuthenticated = true,
                pushNotificationsEnabled = false,
                mutedPushTypes = emptySet(),
                dailyPlanEnabled = true,
                onAction = {},
            )
        }
    }
}
