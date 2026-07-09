package com.todoapp.mobile.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.model.LanguagePreference
import com.todoapp.mobile.domain.model.ThemePreference
import com.todoapp.mobile.ui.settings.SettingsContract.UiAction
import com.todoapp.uikit.components.TDSettingsGroup
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

@Composable
internal fun SettingsPersonalizationSection(
    currentTheme: ThemePreference,
    currentLanguage: LanguagePreference,
    onAction: (UiAction) -> Unit,
) {
    TDSettingsGroup(title = stringResource(R.string.settings_section_personalization)) {
        ThemeSelector(
            currentTheme = currentTheme,
            onThemeChange = { onAction(UiAction.OnThemeChange(it)) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LanguageSelector(
            currentLanguage = currentLanguage,
            onLanguageChange = { onAction(UiAction.OnLanguageChange(it)) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@TDPreview
@Composable
private fun SettingsPersonalizationSectionPreview() {
    TDTheme {
        Column(modifier = Modifier.background(TDTheme.colors.background).padding(16.dp)) {
            SettingsPersonalizationSection(
                currentTheme = ThemePreference.SYSTEM_DEFAULT,
                currentLanguage = LanguagePreference.ENGLISH,
                onAction = {},
            )
        }
    }
}
