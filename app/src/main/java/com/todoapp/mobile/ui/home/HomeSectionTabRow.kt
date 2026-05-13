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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.theme.TDTheme

/**
 * Two-tab text row with a rounded pill indicator under the active tab: "Bugünkü" / "Tekrarlı".
 * Selecting "Tekrarlı" surfaces the recurring filter chip row immediately below.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeSectionTabRow(
    isRecurring: Boolean,
    onSelectToday: () -> Unit,
    onSelectRecurring: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = if (isRecurring) 1 else 0
    val tabs =
        listOf(
            stringResource(R.string.home_section_tab_today) to onSelectToday,
            stringResource(R.string.home_section_tab_recurring) to onSelectRecurring,
        )
    SecondaryTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier.fillMaxWidth(),
        containerColor = TDTheme.colors.background,
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

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun HomeSectionTabRowTodayPreview() {
    TDTheme {
        HomeSectionTabRow(
            isRecurring = false,
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
            onSelectToday = {},
            onSelectRecurring = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
