package com.todoapp.mobile.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.model.ThemePreference
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.theme.TDTheme

@Composable
fun ThemeSelector(
    modifier: Modifier = Modifier,
    currentTheme: ThemePreference,
    onThemeChange: (ThemePreference) -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TDText(
            text = stringResource(R.string.app_theme),
            style = TDTheme.typography.heading6,
            color = TDTheme.colors.onBackground,
            modifier = Modifier.weight(1f),
        )

        Row(
            modifier =
            Modifier
                .padding(start = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(TDTheme.colors.background)
                .border(
                    width = 1.dp,
                    color = TDTheme.colors.gray,
                    shape = RoundedCornerShape(12.dp),
                ),
        ) {
            ThemeItem(
                selected = currentTheme == ThemePreference.LIGHT_MODE,
                onClick = { onThemeChange(ThemePreference.LIGHT_MODE) },
                icon = painterResource(com.example.uikit.R.drawable.ic_light_mode),
            )

            ThemeItem(
                selected = currentTheme == ThemePreference.DARK_MODE,
                onClick = { onThemeChange(ThemePreference.DARK_MODE) },
                icon = painterResource(com.example.uikit.R.drawable.ic_dark_mode),
            )

            ThemeItem(
                selected = currentTheme == ThemePreference.SYSTEM_DEFAULT,
                onClick = { onThemeChange(ThemePreference.SYSTEM_DEFAULT) },
                icon = painterResource(com.example.uikit.R.drawable.ic_system_default),
            )
        }
    }
}

@Composable
private fun ThemeItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: Painter,
) {
    Box(
        modifier =
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) {
                    TDTheme.colors.pendingGray
                } else {
                    Color.Transparent
                },
            ).clickable { onClick() }
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = icon,
            contentDescription = stringResource(R.string.app_theme),
            tint =
            if (selected) {
                TDTheme.colors.onPrimary
            } else {
                TDTheme.colors.gray
            },
            modifier = Modifier.size(24.dp),
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun ThemeSelectorAllStatesPreview_light() {
    TDTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ThemeSelector(
                currentTheme = ThemePreference.LIGHT_MODE,
                onThemeChange = {},
            )
            ThemeSelector(
                currentTheme = ThemePreference.DARK_MODE,
                onThemeChange = {},
            )
            ThemeSelector(
                currentTheme = ThemePreference.SYSTEM_DEFAULT,
                onThemeChange = {},
            )
        }
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun ThemeSelectorAllStatesPreview_dark() {
    TDTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ThemeSelector(
                currentTheme = ThemePreference.LIGHT_MODE,
                onThemeChange = {},
            )
            ThemeSelector(
                currentTheme = ThemePreference.DARK_MODE,
                onThemeChange = {},
            )
            ThemeSelector(
                currentTheme = ThemePreference.SYSTEM_DEFAULT,
                onThemeChange = {},
            )
        }
    }
}
