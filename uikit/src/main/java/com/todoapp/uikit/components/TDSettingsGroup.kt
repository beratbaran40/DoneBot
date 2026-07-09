package com.todoapp.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

/**
 * A One UI-style "focus block": a rounded card that groups related settings rows, with an
 * optional muted section label above it. Rows inside separate by spacing only — no dividers.
 * The thin border keeps the card legible on both themes (slightly stronger in light mode).
 */
@Composable
fun TDSettingsGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    containerColor: Color = TDTheme.colors.settingsCard,
    showBorder: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val borderAlpha = if (TDTheme.isDark) 0.25f else 0.4f
    Column(modifier = modifier) {
        if (title != null) {
            TDText(
                text = title,
                style = TDTheme.typography.subheading1,
                color = TDTheme.colors.gray,
                isHeading = true,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(containerColor)
                .then(
                    if (showBorder) {
                        Modifier.border(1.dp, TDTheme.colors.lightGray.copy(alpha = borderAlpha), shape)
                    } else {
                        Modifier
                    },
                )
                .padding(vertical = 6.dp),
            content = content,
        )
    }
}

@TDPreview
@Composable
private fun TdSettingsGroupPreview() {
    TDTheme {
        Column(modifier = Modifier.background(TDTheme.colors.background).padding(16.dp)) {
            TDSettingsGroup(title = "Notifications & Reminders") {
                TDSettingsItem(
                    title = "Push notifications",
                    icon = painterResource(R.drawable.ic_notification),
                    iconTint = TDTheme.colors.darkPending,
                    iconContainerColor = TDTheme.colors.lightPending,
                    trailingContent = { TDSwitch(checked = true, onCheckedChange = {}) },
                )
                TDSettingsItem(
                    title = "Exact reminders",
                    subtitle = "Enabled",
                    icon = painterResource(R.drawable.ic_clock),
                    iconTint = TDTheme.colors.darkPending,
                    iconContainerColor = TDTheme.colors.lightPending,
                )
                TDSettingsItem(
                    title = "Alarm sound",
                    icon = painterResource(R.drawable.ic_notification),
                    iconTint = TDTheme.colors.darkPending,
                    iconContainerColor = TDTheme.colors.lightPending,
                    onClick = {},
                )
            }
        }
    }
}

@TDPreview
@Composable
private fun TdSettingsGroupUntitledPreview() {
    TDTheme {
        Column(modifier = Modifier.background(TDTheme.colors.background).padding(16.dp)) {
            TDSettingsGroup {
                TDSettingsItem(
                    title = "Login or Create Account",
                    icon = painterResource(R.drawable.ic_profile),
                    iconTint = TDTheme.colors.primary,
                    iconContainerColor = TDTheme.colors.bgColorPurple,
                    titleColor = TDTheme.colors.darkPending,
                    onClick = {},
                )
            }
        }
    }
}

@TDPreview
@Composable
private fun TdSettingsGroupDestructivePreview() {
    TDTheme {
        Column(modifier = Modifier.background(TDTheme.colors.background).padding(16.dp)) {
            TDSettingsGroup(title = "Danger Zone") {
                TDSettingsItem(
                    title = "Delete account",
                    icon = painterResource(R.drawable.ic_delete),
                    iconTint = TDTheme.colors.crossRed,
                    iconContainerColor = TDTheme.colors.lightRed,
                    titleColor = TDTheme.colors.crossRed,
                    onClick = {},
                )
            }
        }
    }
}
