package com.todoapp.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

/**
 * One list row inside a [TDSettingsGroup]: an optional colored icon medallion, a title, an
 * optional subtitle, and a trailing control. When [trailingContent] is null and the row is
 * clickable, a chevron renders automatically; a row with neither stays trailing-free (status
 * rows). [iconContainerColor] = null renders the bare glyph without the medallion circle.
 * The caller supplies [icon] as a [Painter] so :uikit stays decoupled from :app drawables.
 */
@Composable
fun TDSettingsItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: Painter? = null,
    iconTint: Color = TDTheme.colors.pendingGray,
    iconContainerColor: Color? = null,
    titleColor: Color = TDTheme.colors.onBackground,
    subtitleColor: Color = TDTheme.colors.gray,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable(enabled = enabled) { onClick() }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            if (iconContainerColor != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconContainerColor),
                ) {
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp),
                    )
                }
            } else {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            TDText(
                text = title,
                style = TDTheme.typography.heading6,
                color = titleColor,
            )
            if (subtitle != null) {
                TDText(
                    text = subtitle,
                    style = TDTheme.typography.subheading2,
                    color = subtitleColor,
                )
            }
        }
        when {
            trailingContent != null -> {
                Spacer(modifier = Modifier.width(12.dp))
                trailingContent()
            }

            onClick != null -> {
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_forward),
                    contentDescription = null,
                    tint = TDTheme.colors.gray,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@TDPreview
@Composable
private fun TdSettingsItemNavigationPreview() {
    TDTheme {
        Column(modifier = Modifier.background(TDTheme.colors.settingsCard)) {
            TDSettingsItem(
                title = "Push notifications",
                icon = painterResource(R.drawable.ic_notification),
                iconTint = TDTheme.colors.darkPending,
                iconContainerColor = TDTheme.colors.lightPending,
                onClick = {},
            )
            TDSettingsItem(
                title = "Send feedback",
                subtitle = "Questions, ideas, bug reports",
                icon = painterResource(R.drawable.ic_mail),
                iconTint = TDTheme.colors.primary,
                iconContainerColor = TDTheme.colors.bgColorPurple,
                onClick = {},
            )
        }
    }
}

@TDPreview
@Composable
private fun TdSettingsItemSwitchPreview() {
    TDTheme {
        Column(modifier = Modifier.background(TDTheme.colors.settingsCard)) {
            TDSettingsItem(
                title = "Reduce motion",
                subtitle = "Disables non-essential animations",
                icon = painterResource(R.drawable.ic_info),
                iconTint = TDTheme.colors.primary,
                iconContainerColor = TDTheme.colors.infoCardBgColor,
                trailingContent = { TDSwitch(checked = true, onCheckedChange = {}) },
            )
            TDSettingsItem(
                title = "Share usage data",
                icon = painterResource(R.drawable.ic_warning),
                iconTint = TDTheme.colors.darkGreen,
                iconContainerColor = TDTheme.colors.lightGreen,
                trailingContent = { TDSwitch(checked = false, onCheckedChange = {}) },
            )
        }
    }
}

@TDPreview
@Composable
private fun TdSettingsItemBareGlyphPreview() {
    TDTheme {
        Column(modifier = Modifier.background(TDTheme.colors.settingsCard)) {
            TDSettingsItem(
                title = "App language",
                icon = painterResource(R.drawable.ic_globe),
                iconTint = TDTheme.colors.purple,
                onClick = {},
            )
        }
    }
}

@TDPreview
@Composable
private fun TdSettingsItemDestructivePreview() {
    TDTheme {
        Column(modifier = Modifier.background(TDTheme.colors.settingsCard)) {
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

@TDPreview
@Composable
private fun TdSettingsItemStatusPreview() {
    TDTheme {
        Column(modifier = Modifier.background(TDTheme.colors.settingsCard)) {
            TDSettingsItem(
                title = "Exact reminders",
                subtitle = "Enabled",
                icon = painterResource(R.drawable.ic_clock),
                iconTint = TDTheme.colors.darkPending,
                iconContainerColor = TDTheme.colors.lightPending,
            )
        }
    }
}
