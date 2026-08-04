@file:Suppress("MatchingDeclarationName")

package com.todoapp.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.modifier.tdShadow
import com.todoapp.uikit.theme.TDTheme
import com.todoapp.uikit.theme.tdOutlineColor

/**
 * One reminder option. `minutes = 0L` means on-time; `minutes = null` means no reminder.
 * Positive values mean N minutes before the deadline.
 */
@Suppress("MatchingDeclarationName")
data class TDReminderOption(
    val label: String,
    val minutes: Long?,
)

/**
 * Premium horizontally-scrollable chip picker for selecting a reminder offset on a task.
 * Uses TDTheme tokens; selected chip is purple, unselected is lightPending. Light mode adds
 * a neumorphic shadow per chip; dark mode swaps in a subtle border to avoid harsh shadows.
 */
@Composable
fun TDReminderOffsetPicker(
    selectedMinutes: Long?,
    options: List<TDReminderOption>,
    onSelected: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = options, key = { it.label }) { option ->
            ReminderChip(
                option = option,
                isSelected = option.minutes == selectedMinutes,
                onClick = { onSelected(option.minutes) },
            )
        }
    }
}

@Composable
private fun ReminderChip(
    option: TDReminderOption,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val isDark = TDTheme.isDark
    val background = if (isSelected) TDTheme.colors.pendingGray else TDTheme.colors.lightPending
    val foreground = if (isSelected) TDTheme.colors.white else TDTheme.colors.onBackground
    val iconTint = if (isSelected) TDTheme.colors.white else TDTheme.colors.darkPending

    val baseModifier = Modifier
        .clip(TDTheme.shapes.xLarge)
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
        Icon(
            painter = tdPainter(R.drawable.ic_clock),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(6.dp))
        TDText(
            text = option.label,
            color = foreground,
            style = TDTheme.typography.subheading2,
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun TDReminderOffsetPickerPreview() {
    TDTheme {
        TDReminderOffsetPicker(
            selectedMinutes = 10L,
            options = listOf(
                TDReminderOption("None", null),
                TDReminderOption("On time", 0L),
                TDReminderOption("5 min", 5L),
                TDReminderOption("10 min", 10L),
                TDReminderOption("30 min", 30L),
            ),
            onSelected = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun TDReminderOffsetPickerNoneSelectedPreview() {
    TDTheme {
        TDReminderOffsetPicker(
            selectedMinutes = null,
            options = listOf(
                TDReminderOption("None", null),
                TDReminderOption("On time", 0L),
                TDReminderOption("5 min", 5L),
            ),
            onSelected = {},
        )
    }
}
