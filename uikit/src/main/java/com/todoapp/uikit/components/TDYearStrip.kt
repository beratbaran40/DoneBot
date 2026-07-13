package com.todoapp.uikit.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.PaletteKit
import com.todoapp.uikit.theme.TDTheme

@Composable
fun TDYearStrip(
    title: String,
    monthLabels: List<String>,
    monthCounts: List<Int>,
    selectedIndex: Int,
    onMonthClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    require(monthLabels.size == monthCounts.size) { "monthLabels.size must equal monthCounts.size" }

    Column(modifier = modifier.fillMaxWidth()) {
        TDText(
            text = title,
            style = TDTheme.typography.heading5,
            color = TDTheme.colors.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            monthLabels.forEachIndexed { index, label ->
                MonthCell(
                    label = label,
                    count = monthCounts[index],
                    isSelected = index == selectedIndex,
                    onClick = { onMonthClick(index) },
                )
            }
        }
    }
}

@Composable
private fun MonthCell(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = TDTheme.colors
    // MONOCHROME: neutral selection accent (near-white in dark) instead of blue for the current month.
    val monthAccent = if (TDTheme.palette == PaletteKit.MONOCHROME) colors.onBackground else colors.purple
    Column(
        modifier = Modifier
            .width(20.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(heatmapBucketColor(count, colors))
                .then(
                    if (isSelected) {
                        Modifier.border(
                            width = 2.dp,
                            color = monthAccent,
                            shape = RoundedCornerShape(4.dp),
                        )
                    } else {
                        Modifier
                    },
                ),
        )
        Spacer(modifier = Modifier.height(4.dp))
        TDText(
            text = label,
            style = TDTheme.typography.subheading2,
            color = if (isSelected) monthAccent else colors.pendingGray,
        )
    }
}

@TDPreview
@Composable
private fun TDYearStripDensePreview() {
    val labels = listOf("M", "J", "J", "A", "S", "O", "N", "D", "J", "F", "M", "A")
    TDTheme {
        Box(modifier = Modifier.background(TDTheme.colors.background).padding(16.dp)) {
            TDYearStrip(
                title = "Last 12 months",
                monthLabels = labels,
                monthCounts = listOf(2, 5, 8, 1, 0, 12, 6, 3, 9, 4, 7, 11),
                selectedIndex = 11,
                onMonthClick = {},
            )
        }
    }
}

@TDPreview
@Composable
private fun TDYearStripEmptyPreview() {
    val labels = listOf("M", "J", "J", "A", "S", "O", "N", "D", "J", "F", "M", "A")
    TDTheme {
        Box(modifier = Modifier.background(TDTheme.colors.background).padding(16.dp)) {
            TDYearStrip(
                title = "Last 12 months",
                monthLabels = labels,
                monthCounts = List(12) { 0 },
                selectedIndex = 5,
                onMonthClick = {},
            )
        }
    }
}
