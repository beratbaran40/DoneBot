package com.todoapp.mobile.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.settings.SettingsContract.UiAction
import com.todoapp.uikit.components.TDSettingsGroup
import com.todoapp.uikit.components.TDSettingsItem
import com.todoapp.uikit.components.TDSwitch
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

@Composable
internal fun SettingsAccessibilitySection(
    reduceMotionEnabled: Boolean,
    onAction: (UiAction) -> Unit,
) {
    val context = LocalContext.current
    TDSettingsGroup(title = stringResource(R.string.settings_section_accessibility)) {
        TDSettingsItem(
            title = stringResource(R.string.settings_open_system_a11y),
            icon = painterResource(R.drawable.ic_settings_accessibility),
            iconTint = TDTheme.colors.primary,
            iconContainerColor = TDTheme.colors.infoCardBgColor,
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            },
        )
        TDSettingsItem(
            title = stringResource(R.string.settings_reduce_motion),
            subtitle = stringResource(R.string.settings_reduce_motion_desc),
            icon = painterResource(R.drawable.ic_settings_motion),
            iconTint = TDTheme.colors.primary,
            iconContainerColor = TDTheme.colors.infoCardBgColor,
            trailingContent = {
                TDSwitch(
                    checked = reduceMotionEnabled,
                    onCheckedChange = { onAction(UiAction.OnReduceMotionToggle(it)) },
                )
            },
        )
        TDSettingsItem(
            title = stringResource(R.string.settings_larger_text),
            subtitle = stringResource(R.string.settings_larger_text_desc),
            icon = painterResource(R.drawable.ic_settings_text_size),
            iconTint = TDTheme.colors.primary,
            iconContainerColor = TDTheme.colors.infoCardBgColor,
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_DISPLAY_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            },
        )
    }
}

@TDPreview
@Composable
private fun SettingsAccessibilitySectionPreview() {
    TDTheme {
        Column(modifier = Modifier.background(TDTheme.colors.background).padding(16.dp)) {
            SettingsAccessibilitySection(
                reduceMotionEnabled = false,
                onAction = {},
            )
        }
    }
}
