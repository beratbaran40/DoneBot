package com.todoapp.mobile.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.groups.groupdetail.MemberAvatar
import com.todoapp.uikit.components.TDSettingsGroup
import com.todoapp.uikit.components.TDSettingsItem
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

@Composable
internal fun SettingsProfileCard(
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

    TDSettingsGroup {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MemberAvatar(
                initials = initials,
                size = 48,
                avatarUrl = avatarUrl,
                avatarVersion = avatarVersion,
            )
            Spacer(modifier = Modifier.width(12.dp))
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
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                painter = tdPainter(com.example.uikit.R.drawable.ic_arrow_forward),
                contentDescription = null,
                tint = TDTheme.colors.gray,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
internal fun SettingsLoginCard(onClick: () -> Unit) {
    TDSettingsGroup {
        TDSettingsItem(
            title = stringResource(R.string.login_or_create_account),
            icon = tdPainter(com.example.uikit.R.drawable.ic_profile),
            iconTint = TDTheme.colors.primary,
            iconContainerColor = TDTheme.colors.bgColorPurple,
            titleColor = TDTheme.colors.darkPending,
            onClick = onClick,
        )
    }
}

@TDPreview
@Composable
private fun SettingsProfileCardPreview() {
    TDTheme {
        Column(modifier = Modifier.background(TDTheme.colors.background).padding(16.dp)) {
            SettingsProfileCard(
                displayName = "Ada Lovelace",
                email = "ada@example.com",
                avatarUrl = null,
                avatarVersion = 0L,
                onClick = {},
            )
        }
    }
}

@TDPreview
@Composable
private fun SettingsLoginCardPreview() {
    TDTheme {
        Column(modifier = Modifier.background(TDTheme.colors.background).padding(16.dp)) {
            SettingsLoginCard(onClick = {})
        }
    }
}
