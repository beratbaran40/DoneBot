@file:Suppress("TooManyFunctions")

package com.todoapp.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDColor
import com.todoapp.uikit.theme.TDTheme
import com.todoapp.uikit.theme.tdCorner
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private const val DAYS_IN_WEEK = 7
private val DEFAULT_CELL_SIZE = 12.dp
private val MAX_CELL_SIZE = 28.dp
private val CELL_GAP = 2.dp
private val GUTTER_WIDTH = 32.dp
private val MONTH_LABEL_ROW_HEIGHT = 14.dp

@Composable
fun TDActivityHeatmap(
    startDate: LocalDate,
    endDate: LocalDate,
    counts: Map<LocalDate, Int>,
    onCellClick: (LocalDate) -> Unit,
    title: String,
    legendLessLabel: String,
    legendMoreLabel: String,
    modifier: Modifier = Modifier,
    locale: Locale = Locale.getDefault(),
    singleMonthMode: Boolean = false,
) {
    val columns = remember(startDate, endDate) {
        val totalDays = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
        (totalDays + DAYS_IN_WEEK - 1) / DAYS_IN_WEEK
    }

    Column(modifier = modifier.fillMaxWidth()) {
        TDText(
            text = title,
            style = TDTheme.typography.heading5,
            color = TDTheme.colors.onBackground,
        )
        Spacer(Modifier.height(12.dp))

        if (singleMonthMode) {
            // Adaptive cells fill the card horizontally — no scroll, all 7 weekday labels visible.
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val available = maxWidth - GUTTER_WIDTH - CELL_GAP * (columns - 1).coerceAtLeast(0)
                val cellSize = (available / columns.coerceAtLeast(1)).coerceIn(DEFAULT_CELL_SIZE, MAX_CELL_SIZE)
                Row {
                    WeekdayGutter(locale = locale, monthLabelsVisible = false, showAllDays = true, cellSize = cellSize)
                    HeatmapGrid(
                        startDate = startDate,
                        endDate = endDate,
                        columns = columns,
                        counts = counts,
                        onCellClick = onCellClick,
                        locale = locale,
                        showMonthLabels = false,
                        cellSize = cellSize,
                    )
                }
            }
        } else {
            val scrollState = rememberScrollState()
            LaunchedEffect(columns) { scrollState.scrollTo(scrollState.maxValue) }
            Row {
                WeekdayGutter(locale = locale, monthLabelsVisible = true, showAllDays = false, cellSize = DEFAULT_CELL_SIZE)
                Box(modifier = Modifier.horizontalScroll(scrollState)) {
                    HeatmapGrid(
                        startDate = startDate,
                        endDate = endDate,
                        columns = columns,
                        counts = counts,
                        onCellClick = onCellClick,
                        locale = locale,
                        showMonthLabels = true,
                        cellSize = DEFAULT_CELL_SIZE,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        HeatmapLegend(legendLessLabel = legendLessLabel, legendMoreLabel = legendMoreLabel)
    }
}

@Composable
private fun WeekdayGutter(
    locale: Locale,
    monthLabelsVisible: Boolean,
    showAllDays: Boolean,
    cellSize: Dp,
) {
    Column(
        modifier = Modifier
            .width(GUTTER_WIDTH)
            // Push down only when the grid renders a month-label row above the cells.
            .padding(top = if (monthLabelsVisible) MONTH_LABEL_ROW_HEIGHT else 0.dp),
        verticalArrangement = Arrangement.spacedBy(CELL_GAP),
    ) {
        DayOfWeek.values().forEach { day ->
            Box(
                modifier = Modifier
                    .height(cellSize)
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterStart,
            ) {
                val visible = showAllDays || day == DayOfWeek.MONDAY || day == DayOfWeek.WEDNESDAY || day == DayOfWeek.FRIDAY
                if (visible) {
                    // Resource-backed because DayOfWeek.getDisplayName(SHORT, tr) falls back
                    // to English on some Android locale-data builds.
                    val labelRes = when (day) {
                        DayOfWeek.MONDAY -> com.example.uikit.R.string.weekday_abbr_mon
                        DayOfWeek.TUESDAY -> com.example.uikit.R.string.weekday_abbr_tue
                        DayOfWeek.WEDNESDAY -> com.example.uikit.R.string.weekday_abbr_wed
                        DayOfWeek.THURSDAY -> com.example.uikit.R.string.weekday_abbr_thu
                        DayOfWeek.FRIDAY -> com.example.uikit.R.string.weekday_abbr_fri
                        DayOfWeek.SATURDAY -> com.example.uikit.R.string.weekday_abbr_sat
                        DayOfWeek.SUNDAY -> com.example.uikit.R.string.weekday_abbr_sun
                    }
                    TDText(
                        text = stringResource(labelRes),
                        style = TDTheme.typography.subheading2,
                        color = TDTheme.colors.pendingGray,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeatmapGrid(
    startDate: LocalDate,
    endDate: LocalDate,
    columns: Int,
    counts: Map<LocalDate, Int>,
    onCellClick: (LocalDate) -> Unit,
    locale: Locale,
    showMonthLabels: Boolean,
    cellSize: Dp,
) {
    val tdColor = TDTheme.colors
    Column {
        if (showMonthLabels) {
            MonthLabelsRow(startDate = startDate, columns = columns, locale = locale, cellSize = cellSize)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(CELL_GAP)) {
            for (col in 0 until columns) {
                Column(verticalArrangement = Arrangement.spacedBy(CELL_GAP)) {
                    for (row in 0 until DAYS_IN_WEEK) {
                        val cellDate = startDate.plusDays((col * DAYS_IN_WEEK + row).toLong())
                        if (cellDate.isAfter(endDate)) {
                            Spacer(modifier = Modifier.size(cellSize))
                        } else {
                            HeatmapCell(
                                date = cellDate,
                                count = counts[cellDate] ?: 0,
                                colors = tdColor,
                                cellSize = cellSize,
                                onClick = onCellClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatmapCell(
    date: LocalDate,
    count: Int,
    colors: TDColor,
    cellSize: Dp,
    onClick: (LocalDate) -> Unit,
) {
    val description = "$date · $count"
    Box(
        modifier = Modifier
            .size(cellSize)
            .background(
                color = heatmapBucketColor(count, colors),
                shape = tdCorner(3.dp),
            )
            .clickable { onClick(date) }
            .semantics { contentDescription = description },
    )
}

@Composable
private fun MonthLabelsRow(
    startDate: LocalDate,
    columns: Int,
    locale: Locale,
    cellSize: Dp,
) {
    val labels = remember(startDate, columns, locale) {
        val byColumn = arrayOfNulls<String>(columns)
        var lastMonth = -1
        for (col in 0 until columns) {
            val firstDayOfColumn = startDate.plusDays((col * DAYS_IN_WEEK).toLong())
            val month = firstDayOfColumn.monthValue
            if (month != lastMonth) {
                byColumn[col] = firstDayOfColumn.month.getDisplayName(TextStyle.SHORT, locale)
                lastMonth = month
            }
        }
        byColumn.toList()
    }
    Row(
        modifier = Modifier.height(MONTH_LABEL_ROW_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(CELL_GAP),
    ) {
        labels.forEach { label ->
            Box(modifier = Modifier.width(cellSize)) {
                if (label != null) {
                    TDText(
                        text = label,
                        style = TDTheme.typography.subheading2,
                        color = TDTheme.colors.pendingGray,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeatmapLegend(legendLessLabel: String, legendMoreLabel: String) {
    val colors = TDTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TDText(
            text = legendLessLabel,
            style = TDTheme.typography.subheading2,
            color = TDTheme.colors.pendingGray,
        )
        listOf(0, 1, 2, 4).forEach { sample ->
            Box(
                modifier = Modifier
                    .size(DEFAULT_CELL_SIZE)
                    .background(
                        color = heatmapBucketColor(sample, colors),
                        shape = tdCorner(3.dp),
                    ),
            )
        }
        TDText(
            text = legendMoreLabel,
            style = TDTheme.typography.subheading2,
            color = TDTheme.colors.pendingGray,
        )
    }
}

// region previews

private fun previewRange(): Pair<LocalDate, LocalDate> {
    val end = LocalDate.of(2026, 4, 28)
    val start = end.minusDays(180).with(DayOfWeek.MONDAY)
    return start to end
}

private fun densePreviewData(start: LocalDate, end: LocalDate): Map<LocalDate, Int> {
    val map = mutableMapOf<LocalDate, Int>()
    var d = start
    var i = 0
    while (!d.isAfter(end)) {
        // Pseudo-random pattern that hits every bucket.
        val v = (i * 7 + i / 3) % 11
        if (v != 0) map[d] = v % 6
        d = d.plusDays(1)
        i++
    }
    return map
}

private fun sparsePreviewData(end: LocalDate): Map<LocalDate, Int> = mapOf(
    end.minusDays(2) to 3,
    end.minusDays(5) to 1,
    end.minusDays(9) to 2,
    end.minusDays(15) to 5,
    end.minusDays(40) to 1,
    end.minusDays(72) to 4,
    end.minusDays(120) to 2,
)

@TDPreview
@Composable
private fun TDActivityHeatmapEmptyPreview() {
    val (start, end) = previewRange()
    TDTheme {
        Box(modifier = Modifier.background(TDTheme.colors.background).padding(16.dp)) {
            TDActivityHeatmap(
                startDate = start,
                endDate = end,
                counts = emptyMap(),
                onCellClick = {},
                title = "Last 6 months",
                legendLessLabel = "Less",
                legendMoreLabel = "More",
            )
        }
    }
}

@TDPreview
@Composable
private fun TDActivityHeatmapSparsePreview() {
    val (start, end) = previewRange()
    TDTheme {
        Box(modifier = Modifier.background(TDTheme.colors.background).padding(16.dp)) {
            TDActivityHeatmap(
                startDate = start,
                endDate = end,
                counts = sparsePreviewData(end),
                onCellClick = {},
                title = "Last 6 months",
                legendLessLabel = "Less",
                legendMoreLabel = "More",
            )
        }
    }
}

@TDPreview
@Composable
private fun TDActivityHeatmapDensePreview() {
    val (start, end) = previewRange()
    TDTheme {
        Box(modifier = Modifier.background(TDTheme.colors.background).padding(16.dp)) {
            TDActivityHeatmap(
                startDate = start,
                endDate = end,
                counts = densePreviewData(start, end),
                onCellClick = {},
                title = "Last 6 months",
                legendLessLabel = "Less",
                legendMoreLabel = "More",
            )
        }
    }
}

@TDPreview
@Composable
private fun TDActivityHeatmapSingleMonthPreview() {
    val start = LocalDate.of(2026, 4, 1)
    val end = LocalDate.of(2026, 4, 30)
    val data = mapOf(
        LocalDate.of(2026, 4, 5) to 2,
        LocalDate.of(2026, 4, 6) to 1,
        LocalDate.of(2026, 4, 12) to 8,
        LocalDate.of(2026, 4, 18) to 4,
        LocalDate.of(2026, 4, 22) to 3,
    )
    TDTheme {
        Box(modifier = Modifier.background(TDTheme.colors.background).padding(16.dp)) {
            TDActivityHeatmap(
                startDate = start,
                endDate = end,
                counts = data,
                onCellClick = {},
                title = "April activity",
                legendLessLabel = "Less",
                legendMoreLabel = "More",
                singleMonthMode = true,
            )
        }
    }
}

// endregion
