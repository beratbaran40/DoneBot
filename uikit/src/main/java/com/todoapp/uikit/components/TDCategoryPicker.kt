package com.todoapp.uikit.components

import androidx.annotation.DrawableRes
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
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.modifier.tdShadow
import com.todoapp.uikit.theme.TDTheme
import com.todoapp.uikit.theme.tdOutlineColor

/**
 * Horizontally-scrollable chip picker for selecting a task category. Mirrors the
 * TDReminderOffsetPicker style — selected chip is purple, unselected is lightPending. Light
 * mode adds a neumorphic shadow per chip; dark mode swaps in a subtle border.
 */
@Composable
fun TDCategoryPicker(
    selectedKey: String,
    options: List<TDCategoryOption>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = options, key = { it.key }) { option ->
            CategoryChip(
                option = option,
                isSelected = option.key == selectedKey,
                onClick = { onSelected(option.key) },
            )
        }
    }
}

/**
 * One category option. `key` is the stable enum name (e.g. "BIRTHDAY"); `label` is the
 * localized display string. `iconRes` is optional — when non-null, the chip renders a small
 * leading icon tinted to the chip's foreground color. The caller resolves all three — the
 * picker is stateless.
 */
data class TDCategoryOption(
    val key: String,
    val label: String,
    @DrawableRes val iconRes: Int? = null,
)

@Composable
private fun CategoryChip(
    option: TDCategoryOption,
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
        option.iconRes?.let { iconRes ->
            Icon(
                painter = tdPainter(iconRes),
                contentDescription = null,
                tint = foreground,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        TDText(
            text = option.label,
            color = foreground,
            style = TDTheme.typography.subheading2,
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun TDCategoryPickerPreview() {
    TDTheme {
        TDCategoryPicker(
            selectedKey = "WORK",
            options = listOf(
                TDCategoryOption(key = "PERSONAL", label = "Personal"),
                TDCategoryOption(key = "WORK", label = "Work"),
                TDCategoryOption(key = "STUDY", label = "Study"),
                TDCategoryOption(key = "HEALTH", label = "Health"),
                TDCategoryOption(key = "OTHER", label = "Other"),
            ),
            onSelected = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun TDCategoryPickerNoSelectionPreview() {
    TDTheme {
        TDCategoryPicker(
            selectedKey = "",
            options = listOf(
                TDCategoryOption(key = "PERSONAL", label = "Personal"),
                TDCategoryOption(key = "WORK", label = "Work"),
                TDCategoryOption(key = "BIRTHDAY", label = "Birthday"),
            ),
            onSelected = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun TDCategoryPickerSinglePreview() {
    TDTheme {
        TDCategoryPicker(
            selectedKey = "WORK",
            options = listOf(TDCategoryOption(key = "WORK", label = "Work")),
            onSelected = {},
        )
    }
}
