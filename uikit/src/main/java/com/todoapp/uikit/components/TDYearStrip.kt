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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.previews.TDPreviewNarrow
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
                    // Weighted rather than a fixed 20dp: twelve fixed cells claimed 240dp of the
                    // ~256dp an ActivityCard leaves on a 320dp screen, so the strip had 16dp of slack
                    // across eleven gaps and clipped the moment anything else took width.
                    modifier = Modifier.weight(1f),
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
    modifier: Modifier = Modifier,
) {
    val colors = TDTheme.colors
    // MONOCHROME: neutral selection accent (near-white in dark) instead of blue for the current month.
    // TERMINAL takes `primary`: the ring sits around a heatmap cell, and that ramp is already the
    // dim-green-to-cyan family, so violet clashes and the pale phosphor ink is too close to read as a
    // ring. The cursor green is the one saturated token the cells never use.
    val monthAccent = when (TDTheme.palette) {
        PaletteKit.MONOCHROME -> colors.onBackground
        PaletteKit.TERMINAL -> colors.primary
        PaletteKit.ORIGINAL, PaletteKit.PIXEL -> colors.purple
    }
    Column(
        modifier = modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(TDTheme.shapes.tiny)
                .background(heatmapBucketColor(count, colors))
                .then(
                    if (isSelected) {
                        Modifier.border(
                            width = 2.dp,
                            color = monthAccent,
                            shape = TDTheme.shapes.tiny,
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
            maxLines = 1,
        )
    }
}

/** 256dp is what an ActivityCard leaves the strip on a 320dp screen — twelve cells have to fit it. */
@TDPreviewNarrow
@Composable
private fun TDYearStripInCardWidthPreview() {
    val labels = listOf("O", "Ş", "M", "N", "M", "H", "T", "A", "E", "E", "K", "A")
    TDTheme {
        Box(modifier = Modifier.width(256.dp).background(TDTheme.colors.background).padding(16.dp)) {
            TDYearStrip(
                title = "Son 12 ay",
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
