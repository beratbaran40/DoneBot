package com.todoapp.mobile.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.theme.TDTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Two-tab text row with a rounded pill indicator under the active tab: "Bugünkü" / "Tekrarlı".
 * Selecting "Tekrarlı" surfaces the recurring filter chip row immediately below.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeSectionTabRow(
    isRecurring: Boolean,
    selectedDate: LocalDate,
    onSelectToday: () -> Unit,
    onSelectRecurring: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = if (isRecurring) 1 else 0
    val tabs =
        listOf(
            todaySectionTabLabel(selectedDate) to onSelectToday,
            stringResource(R.string.home_section_tab_recurring) to onSelectRecurring,
        )
    SecondaryTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier.fillMaxWidth(),
        // Transparent so the app-wide grid shows through this strip; labels/indicator stay explicit.
        containerColor = Color.Transparent,
        contentColor = TDTheme.colors.pendingGray,
        indicator = {
            Box(
                modifier = Modifier
                    .tabIndicatorOffset(selectedIndex)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .height(4.dp)
                    .background(
                        color = TDTheme.colors.darkPending,
                        shape = RoundedCornerShape(percent = 50),
                    ),
            )
        },
        divider = {},
    ) {
        tabs.forEachIndexed { index, (label, onClick) ->
            Tab(
                selected = selectedIndex == index,
                onClick = onClick,
                text = {
                    TDText(
                        text = label,
                        style = TDTheme.typography.subheading1,
                        color =
                        if (selectedIndex == index) {
                            TDTheme.colors.darkPending
                        } else {
                            TDTheme.colors.gray
                        },
                    )
                },
            )
        }
    }
}

/**
 * Today-tab label that surfaces the selected day so the tab isn't ambiguous when the date strip is
 * scrolled to another day: "Bugün · 21 Haz" when today is selected, just "25 Haz" otherwise.
 */
@Composable
private fun todaySectionTabLabel(selectedDate: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val formatter = remember(locale) { DateTimeFormatter.ofPattern(DAY_MONTH_PATTERN, locale) }
    val dateStr = formatter.format(selectedDate)
    return if (selectedDate == LocalDate.now()) {
        stringResource(
            R.string.home_section_tab_today_dated,
            stringResource(R.string.home_section_tab_today),
            dateStr,
        )
    } else {
        dateStr
    }
}

private const val DAY_MONTH_PATTERN = "d MMM"

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun HomeSectionTabRowTodayPreview() {
    TDTheme {
        HomeSectionTabRow(
            isRecurring = false,
            selectedDate = LocalDate.now(),
            onSelectToday = {},
            onSelectRecurring = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun HomeSectionTabRowOtherDayPreview() {
    TDTheme {
        HomeSectionTabRow(
            isRecurring = false,
            selectedDate = LocalDate.now().plusDays(4),
            onSelectToday = {},
            onSelectRecurring = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun HomeSectionTabRowRecurringPreview() {
    TDTheme {
        HomeSectionTabRow(
            isRecurring = true,
            selectedDate = LocalDate.now(),
            onSelectToday = {},
            onSelectRecurring = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun HomeSectionTabRowDarkPreview() {
    TDTheme(darkTheme = true) {
        HomeSectionTabRow(
            isRecurring = true,
            selectedDate = LocalDate.now(),
            onSelectToday = {},
            onSelectRecurring = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
