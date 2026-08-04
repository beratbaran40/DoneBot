package com.todoapp.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.todoapp.uikit.modifier.tdShadow
import com.todoapp.uikit.theme.TDTheme
import com.todoapp.uikit.theme.tdOutlineColor

/**
 * Horizontally-scrollable chip picker for selecting how often a task repeats. Same structure as
 * TDCategoryPicker — selected chip is purple, unselected is lightPending. The caller resolves
 * the localized labels.
 */
@Composable
fun TDRecurrencePicker(
    selectedKey: String,
    options: List<TDRecurrenceOption>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = options, key = { it.key }) { option ->
            RecurrenceChip(
                option = option,
                isSelected = option.key == selectedKey,
                onClick = { onSelected(option.key) },
            )
        }
    }
}

data class TDRecurrenceOption(
    val key: String,
    val label: String,
)

@Composable
private fun RecurrenceChip(
    option: TDRecurrenceOption,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val isDark = TDTheme.isDark
    val background = if (isSelected) TDTheme.colors.pendingGray else TDTheme.colors.lightPending
    val foreground = if (isSelected) TDTheme.colors.white else TDTheme.colors.onBackground

    val baseModifier = Modifier.clip(TDTheme.shapes.xLarge)
    val elevation = if (isDark) {
        baseModifier.border(
            width = TDTheme.style.borderWidth,
            color = tdOutlineColor(TDTheme.colors.lightGray.copy(alpha = 0.20f)),
            shape = TDTheme.shapes.xLarge,
        )
    } else {
        baseModifier.tdShadow(
            lightShadow = TDTheme.colors.white.copy(alpha = 0.85f),
            darkShadow = TDTheme.colors.darkPending.copy(alpha = 0.15f),
            cornerRadius = 20.dp,
            elevation = 4.dp,
        )
    }

    Row(
        modifier = elevation
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TDText(
            text = option.label,
            color = foreground,
            style = TDTheme.typography.subheading2,
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun TDRecurrencePickerPreview() {
    TDTheme {
        TDRecurrencePicker(
            selectedKey = "DAILY",
            options = listOf(
                TDRecurrenceOption("NONE", "Once"),
                TDRecurrenceOption("DAILY", "Every day"),
                TDRecurrenceOption("WEEKLY", "Every week"),
                TDRecurrenceOption("MONTHLY", "Every month"),
                TDRecurrenceOption("YEARLY", "Every year"),
            ),
            onSelected = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun TDRecurrencePickerNoneSelectedPreview() {
    TDTheme {
        TDRecurrencePicker(
            selectedKey = "NONE",
            options = listOf(
                TDRecurrenceOption("NONE", "Once"),
                TDRecurrenceOption("DAILY", "Every day"),
                TDRecurrenceOption("WEEKLY", "Every week"),
            ),
            onSelected = {},
        )
    }
}
