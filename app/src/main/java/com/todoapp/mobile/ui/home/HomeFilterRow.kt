package com.todoapp.mobile.ui.home

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.home.HomeContract.HomeFilter
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.modifier.tdShadow
import com.todoapp.uikit.theme.TDTheme
import com.todoapp.uikit.theme.tdOutlineColor

/**
 * Recurring filter chip row (Daily / Weekly / Monthly / Yearly). Surfaces under the section
 * tab row when the user activates the "Tekrarlı" tab. TODAY filter is intentionally excluded —
 * it lives in the tab itself.
 */
@Composable
internal fun HomeRecurringChipRow(
    selected: HomeFilter,
    onSelected: (HomeFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val recurringFilters = remember { HomeFilter.values().filter { it != HomeFilter.TODAY } }
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = recurringFilters, key = { it.name }) { filter ->
            FilterChip(
                filter = filter,
                isSelected = filter == selected,
                onClick = { onSelected(filter) },
            )
        }
    }
}

@Composable
private fun FilterChip(
    filter: HomeFilter,
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
            text = stringResource(filterLabelRes(filter)),
            color = foreground,
            style = TDTheme.typography.subheading2,
        )
    }
}

private fun filterLabelRes(filter: HomeFilter): Int = when (filter) {
    HomeFilter.TODAY -> R.string.filter_today
    HomeFilter.DAILY -> R.string.filter_daily
    HomeFilter.WEEKLY -> R.string.filter_weekly
    HomeFilter.MONTHLY -> R.string.filter_monthly
    HomeFilter.YEARLY -> R.string.filter_yearly
}
