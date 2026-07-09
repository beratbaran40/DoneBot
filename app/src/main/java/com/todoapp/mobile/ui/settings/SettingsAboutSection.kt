package com.todoapp.mobile.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.BuildConfig
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.settings.SettingsContract.UiAction
import com.todoapp.uikit.components.TDSettingsGroup
import com.todoapp.uikit.components.TDSettingsItem
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

@Composable
internal fun SettingsAboutSection(
    onSendFeedback: () -> Unit,
    onAction: (UiAction) -> Unit,
) {
    val context = LocalContext.current
    TDSettingsGroup(title = stringResource(R.string.settings_section_about)) {
        TDSettingsItem(
            title = stringResource(R.string.settings_send_feedback),
            icon = painterResource(com.example.uikit.R.drawable.ic_mail),
            iconTint = TDTheme.colors.primary,
            iconContainerColor = TDTheme.colors.bgColorPurple,
            onClick = onSendFeedback,
        )
        TDSettingsItem(
            title = stringResource(R.string.settings_privacy_policy),
            icon = painterResource(R.drawable.ic_settings_shield),
            iconTint = TDTheme.colors.primary,
            iconContainerColor = TDTheme.colors.bgColorPurple,
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PRIVACY_POLICY_URL))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            },
        )
        TDSettingsItem(
            title = stringResource(R.string.settings_terms_of_service),
            icon = painterResource(R.drawable.ic_settings_document),
            iconTint = TDTheme.colors.primary,
            iconContainerColor = TDTheme.colors.bgColorPurple,
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.TERMS_OF_SERVICE_URL))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            },
        )
        TDSettingsItem(
            title = stringResource(R.string.settings_licenses),
            icon = painterResource(R.drawable.ic_settings_code),
            iconTint = TDTheme.colors.primary,
            iconContainerColor = TDTheme.colors.bgColorPurple,
            onClick = { onAction(UiAction.OnNavigateToLicenses) },
        )
    }
}

@TDPreview
@Composable
private fun SettingsAboutSectionPreview() {
    TDTheme {
        Column(modifier = Modifier.background(TDTheme.colors.background).padding(16.dp)) {
            SettingsAboutSection(
                onSendFeedback = {},
                onAction = {},
            )
        }
    }
}
