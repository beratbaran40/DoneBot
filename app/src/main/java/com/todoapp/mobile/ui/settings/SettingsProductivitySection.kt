package com.todoapp.mobile.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.settings.SettingsContract.UiAction
import com.todoapp.uikit.components.TDSettingsGroup
import com.todoapp.uikit.components.TDSettingsItem
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

@Composable
internal fun SettingsProductivitySection(onAction: (UiAction) -> Unit) {
    TDSettingsGroup(title = stringResource(R.string.settings_section_productivity)) {
        TDSettingsItem(
            title = stringResource(R.string.plan_your_day),
            icon = tdPainter(com.example.uikit.R.drawable.ic_sun),
            iconTint = TDTheme.colors.orange,
            iconContainerColor = TDTheme.colors.warmContainer,
            onClick = { onAction(UiAction.OnNavigateToPlanYourDay) },
        )
        TDSettingsItem(
            title = stringResource(R.string.pomodoro_configure_timer),
            icon = tdPainter(com.example.uikit.R.drawable.ic_pomodoro),
            iconTint = TDTheme.colors.orange,
            iconContainerColor = TDTheme.colors.warmContainer,
            onClick = { onAction(UiAction.OnNavigateToPomodoroSettings) },
        )
    }
}

@TDPreview
@Composable
private fun SettingsProductivitySectionPreview() {
    TDTheme {
        Column(modifier = Modifier.background(TDTheme.colors.background).padding(16.dp)) {
            SettingsProductivitySection(onAction = {})
        }
    }
}
