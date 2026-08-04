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
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.model.LanguagePreference
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.theme.TDTheme

@Composable
fun LanguageSelector(
    modifier: Modifier = Modifier,
    currentLanguage: LanguagePreference,
    onLanguageChange: (LanguagePreference) -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TDText(
            text = stringResource(R.string.app_language),
            style = TDTheme.typography.heading6,
            color = TDTheme.colors.onBackground,
            modifier = Modifier.weight(1f),
        )

        Row(
            modifier =
            Modifier
                .padding(start = 16.dp)
                .clip(TDTheme.shapes.medium)
                .background(TDTheme.colors.background)
                .border(
                    width = 1.dp,
                    color = TDTheme.colors.lightGray,
                    shape = TDTheme.shapes.medium,
                ),
        ) {
            LanguageItem(
                selected = currentLanguage == LanguagePreference.ENGLISH,
                onClick = { onLanguageChange(LanguagePreference.ENGLISH) },
                flag = tdPainter(R.drawable.ic_american_flag),
                contentDescription = stringResource(R.string.language_english),
            )

            LanguageItem(
                selected = currentLanguage == LanguagePreference.TURKISH,
                onClick = { onLanguageChange(LanguagePreference.TURKISH) },
                flag = tdPainter(R.drawable.ic_turkish_flag),
                contentDescription = stringResource(R.string.language_turkish),
            )
        }
    }
}

@Composable
private fun LanguageItem(
    selected: Boolean,
    onClick: () -> Unit,
    flag: Painter,
    contentDescription: String,
) {
    Box(
        modifier =
        Modifier
            .clip(TDTheme.shapes.small)
            .background(
                if (selected) TDTheme.colors.pendingGray else Color.Transparent,
            ).clickable { onClick() }
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = flag,
            contentDescription = contentDescription,
            tint = Color.Unspecified,
            modifier = Modifier.size(24.dp),
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun LanguageSelectorPreview_Light() {
    TDTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LanguageSelector(currentLanguage = LanguagePreference.ENGLISH, onLanguageChange = {})
            LanguageSelector(currentLanguage = LanguagePreference.TURKISH, onLanguageChange = {})
        }
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun LanguageSelectorPreview_Dark() {
    TDTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LanguageSelector(currentLanguage = LanguagePreference.ENGLISH, onLanguageChange = {})
            LanguageSelector(currentLanguage = LanguagePreference.TURKISH, onLanguageChange = {})
        }
    }
}
