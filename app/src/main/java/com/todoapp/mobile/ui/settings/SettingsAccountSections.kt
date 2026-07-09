package com.todoapp.mobile.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.settings.SettingsContract.UiAction
import com.todoapp.uikit.components.TDSettingsGroup
import com.todoapp.uikit.components.TDSettingsItem
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

@Composable
internal fun SettingsAccountSections(
    isDeletingAccount: Boolean,
    onAction: (UiAction) -> Unit,
) {
    TDSettingsGroup(title = stringResource(R.string.settings_section_account)) {
        TDSettingsItem(
            title = stringResource(R.string.logout),
            icon = painterResource(R.drawable.ic_logout),
            iconTint = TDTheme.colors.crossRed,
            iconContainerColor = TDTheme.colors.lightRed,
            titleColor = TDTheme.colors.crossRed,
            onClick = { onAction(UiAction.OnLogoutClick) },
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
    TDSettingsGroup(title = stringResource(R.string.settings_section_danger_zone)) {
        TDSettingsItem(
            title = stringResource(R.string.settings_delete_account),
            icon = painterResource(com.example.uikit.R.drawable.ic_delete),
            iconTint = TDTheme.colors.crossRed,
            iconContainerColor = TDTheme.colors.lightRed,
            titleColor = TDTheme.colors.crossRed,
            enabled = !isDeletingAccount,
            onClick = { onAction(UiAction.OnDeleteAccountClick) },
        )
    }
}

@TDPreview
@Composable
private fun SettingsAccountSectionsPreview() {
    TDTheme {
        Column(modifier = Modifier.background(TDTheme.colors.background).padding(16.dp)) {
            SettingsAccountSections(
                isDeletingAccount = false,
                onAction = {},
            )
        }
    }
}
