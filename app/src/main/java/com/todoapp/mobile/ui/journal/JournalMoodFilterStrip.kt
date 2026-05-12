package com.todoapp.mobile.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.model.JournalMood
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.theme.TDTheme

@Composable
internal fun JournalMoodFilterStrip(
    selected: JournalMood?,
    onSelect: (JournalMood?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoodChipPill(
            label = stringResource(R.string.journal_mood_filter_all),
            emoji = null,
            isSelected = selected == null,
            onClick = { onSelect(null) },
        )
        ORDERED_MOODS.forEach { option ->
            MoodChipPill(
                label = stringResource(option.labelRes),
                emoji = option.emoji,
                isSelected = selected == option.mood,
                onClick = {
                    if (selected == option.mood) onSelect(null) else onSelect(option.mood)
                },
            )
        }
    }
}

@Composable
private fun MoodChipPill(
    label: String,
    emoji: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (isSelected) TDTheme.colors.lightPurple else TDTheme.colors.bgColorPurple
    val borderColor = if (isSelected) TDTheme.colors.purple else TDTheme.colors.lightGray
    val textColor = if (isSelected) TDTheme.colors.darkPurple else TDTheme.colors.onBackground

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (emoji != null) {
            TDText(text = emoji, style = TDTheme.typography.subheading2)
        }
        TDText(text = label, style = TDTheme.typography.subheading2, color = textColor)
    }
}
