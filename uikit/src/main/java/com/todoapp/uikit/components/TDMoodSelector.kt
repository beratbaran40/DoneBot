@file:Suppress("MatchingDeclarationName")

package com.todoapp.uikit.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

@Immutable
data class TDMoodOption(
    val emoji: String,
    val label: String,
)

@Composable
fun TDMoodSelector(
    options: List<TDMoodOption>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, option ->
            MoodChip(
                option = option,
                isSelected = index == selectedIndex,
                onClick = { onSelect(if (selectedIndex == index) -1 else index) },
            )
        }
    }
}

@Composable
private fun MoodChip(
    option: TDMoodOption,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.12f else 1.0f,
        label = "moodScale",
    )
    val backgroundColor = if (isSelected) TDTheme.colors.lightPurple else Color.Transparent
    val borderColor = if (isSelected) TDTheme.colors.purple else TDTheme.colors.lightGray

    val description = stringResource(R.string.td_mood_selector_label_cd, option.label)

    Box(
        modifier = Modifier
            .size(44.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(backgroundColor, CircleShape)
            .border(width = 1.dp, color = borderColor, shape = CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        TDText(
            text = option.emoji,
            style = TDTheme.typography.heading3,
        )
    }
}

@TDPreview
@Composable
private fun TdMoodSelectorAllStatesPreview() {
    TDTheme {
        Column(modifier = Modifier.fillMaxWidth()) {
            val opts = sampleOptions()
            TDMoodSelector(
                options = opts,
                selectedIndex = -1,
                onSelect = {},
            )
            Spacer(modifier = Modifier.height(16.dp))
            TDMoodSelector(
                options = opts,
                selectedIndex = 0,
                onSelect = {},
            )
            Spacer(modifier = Modifier.height(16.dp))
            TDMoodSelector(
                options = opts,
                selectedIndex = 2,
                onSelect = {},
            )
        }
    }
}

private fun sampleOptions(): List<TDMoodOption> = listOf(
    TDMoodOption(emoji = "😊", label = "Happy"),
    TDMoodOption(emoji = "😐", label = "Neutral"),
    TDMoodOption(emoji = "😢", label = "Sad"),
    TDMoodOption(emoji = "🙏", label = "Grateful"),
    TDMoodOption(emoji = "😰", label = "Anxious"),
)
