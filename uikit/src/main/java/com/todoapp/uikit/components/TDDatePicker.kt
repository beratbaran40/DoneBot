package com.todoapp.uikit.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreviewWide
import com.todoapp.uikit.theme.TDTheme
import com.todoapp.uikit.theme.tdCorner
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TDDatePicker(
    modifier: Modifier = Modifier,
    selectedDate: LocalDate? = null,
    selectedMonth: YearMonth = YearMonth.now(),
    onMonthForward: () -> Unit = {},
    onMonthBack: () -> Unit = {},
    taskDates: Set<LocalDate> = emptySet(),
    overdueDates: Set<LocalDate> = emptySet(),
    hasOverdueBeforeDisplayedMonth: Boolean = false,
    onDaySelect: (LocalDate) -> Unit,
    onDayDeselect: () -> Unit,
) {
    // Safely obtain the locale from configuration to avoid exceptions in Preview environments
    val configuration = LocalConfiguration.current
    val locale = if (!configuration.locales.isEmpty) configuration.locales[0] else Locale.getDefault()

    // Resource-backed weekday labels. .take(2) on DayOfWeek.getDisplayName(SHORT, tr) used to
    // emit "Pz" for Monday — which collides with Pazar (Sunday) in Turkish. The strings file
    // ships unambiguous 3-letter abbreviations per locale (Pzt/Sal/Çar/Per/Cum/Cmt/Paz in TR).
    val daysOfWeek = listOf(
        stringResource(R.string.weekday_abbr_mon),
        stringResource(R.string.weekday_abbr_tue),
        stringResource(R.string.weekday_abbr_wed),
        stringResource(R.string.weekday_abbr_thu),
        stringResource(R.string.weekday_abbr_fri),
        stringResource(R.string.weekday_abbr_sat),
        stringResource(R.string.weekday_abbr_sun),
    )
    val today = LocalDate.now()

    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val prevMonthOverdueCd = stringResource(R.string.cd_prev_month_has_overdue)
            Box {
                IconButton(
                    onClick = onMonthBack,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        tdPainter(R.drawable.ic_arrow_back),
                        tint = TDTheme.colors.onBackground,
                        contentDescription =
                        if (hasOverdueBeforeDisplayedMonth) prevMonthOverdueCd else "Previous Month",
                    )
                }
                if (hasOverdueBeforeDisplayedMonth) {
                    Box(
                        modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-6).dp, y = 6.dp)
                            .size(8.dp)
                            .background(TDTheme.colors.crossRed, TDTheme.shapes.circle),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            TDText(
                text = "${
                    selectedMonth.month.getDisplayName(TextStyle.FULL, locale)
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
                } ${selectedMonth.year}",
                style = TDTheme.typography.heading5,
                color = TDTheme.colors.onBackground,
            )
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = onMonthForward,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    tdPainter(R.drawable.ic_arrow_forward),
                    tint = TDTheme.colors.onBackground,
                    contentDescription = "Next Month",
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            daysOfWeek.forEach { day ->
                TDText(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = TDTheme.typography.dayOfTheCalendar,
                    color = TDTheme.colors.onBackground,
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = 1.dp,
        )

        // Six rows via the shared helper — five leaves the last day of some months unrenderable.
        calendarGridWeeks(selectedMonth).forEach { week ->
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                week.forEach { currentDate ->
                    val isFromCurrentMonth =
                        currentDate.year == selectedMonth.year && currentDate.month == selectedMonth.month
                    val isSelected = currentDate == selectedDate
                    val isToday = currentDate == today
                    val hasTask = isFromCurrentMonth && currentDate in taskDates
                    val hasOverdue = isFromCurrentMonth && currentDate in overdueDates
                    TDCalendarCell(
                        modifier = Modifier.weight(1f),
                        dayText = currentDate.dayOfMonth.toString(),
                        isSelected = isSelected,
                        isToday = isToday,
                        isFromCurrentMonth = isFromCurrentMonth,
                        hasTask = hasTask,
                        hasOverdue = hasOverdue,
                        onClick = { if (isSelected) onDayDeselect() else onDaySelect(currentDate) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TDCalendarCell(
    modifier: Modifier = Modifier,
    dayText: String,
    isSelected: Boolean,
    isToday: Boolean,
    isFromCurrentMonth: Boolean,
    hasTask: Boolean,
    hasOverdue: Boolean = false,
    onClick: () -> Unit,
) {
    val animatedColor by animateColorAsState(
        targetValue = if (isSelected) TDTheme.colors.pendingGray else Color.Transparent,
        animationSpec = tween(durationMillis = 220),
        label = "td_calendar_cell_bg",
    )

    val textColor =
        when {
            isSelected -> Color.White
            !isFromCurrentMonth -> TDTheme.colors.lightGray
            else -> TDTheme.colors.onBackground
        }
    val overdueCd = stringResource(R.string.cd_day_has_overdue)
    val hasTaskCd = stringResource(R.string.cd_day_has_tasks)
    val barColor: Color? = when {
        hasOverdue -> TDTheme.colors.crossRed
        hasTask && isSelected -> Color.White
        hasTask -> TDTheme.colors.pendingGray
        else -> null
    }
    val barCd: String? = when {
        hasOverdue -> overdueCd
        hasTask -> hasTaskCd
        else -> null
    }

    Box(
        modifier =
        modifier
            .height(52.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isToday && !isSelected) {
                    Box(
                        Modifier
                            .size(34.dp)
                            .border(2.dp, TDTheme.colors.pendingGray, TDTheme.shapes.circle),
                    )
                }
                Box(
                    modifier =
                    Modifier
                        .size(36.dp)
                        .background(animatedColor, TDTheme.shapes.circle),
                )
                TDText(
                    text = dayText,
                    color = textColor,
                    style = TDTheme.typography.subheading4,
                    textAlign = TextAlign.Center,
                )
            }
            Box(
                modifier = Modifier.height(12.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                if (barColor != null) {
                    Box(
                        modifier =
                        Modifier
                            .padding(top = 3.dp)
                            .height(4.dp)
                            .width(24.dp)
                            .background(barColor, tdCorner(2.dp))
                            .then(
                                if (barCd != null) {
                                    Modifier.semantics { contentDescription = barCd }
                                } else {
                                    Modifier
                                },
                            ),
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TDDatePickerSingleInput(
    selectedMonth: YearMonth,
    modifier: Modifier = Modifier,
    onMonthForward: () -> Unit = {},
    onMonthBack: () -> Unit = {},
    selectedDate: LocalDate? = null,
    onDaySelect: (LocalDate) -> Unit,
    onDayDeselect: () -> Unit,
    /** End of a selected span; [selectedDate] is its start. Null = plain single-day selection. */
    rangeEnd: LocalDate? = null,
    /** A day held down while waiting for the second one. Drawn outlined, not filled. */
    anchorDate: LocalDate? = null,
    /** Null disables long-press entirely — no range, no anchor. */
    onDayLongPress: ((LocalDate) -> Unit)? = null,
) {
    // Safely obtain the locale from configuration to avoid exceptions in Preview environments
    val configuration = LocalConfiguration.current
    val locale = if (!configuration.locales.isEmpty) configuration.locales[0] else Locale.getDefault()

    // Resource-backed weekday labels. .take(2) on DayOfWeek.getDisplayName(SHORT, tr) used to
    // emit "Pz" for Monday — which collides with Pazar (Sunday) in Turkish. The strings file
    // ships unambiguous 3-letter abbreviations per locale (Pzt/Sal/Çar/Per/Cum/Cmt/Paz in TR).
    val daysOfWeek = listOf(
        stringResource(R.string.weekday_abbr_mon),
        stringResource(R.string.weekday_abbr_tue),
        stringResource(R.string.weekday_abbr_wed),
        stringResource(R.string.weekday_abbr_thu),
        stringResource(R.string.weekday_abbr_fri),
        stringResource(R.string.weekday_abbr_sat),
        stringResource(R.string.weekday_abbr_sun),
    )

    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .background(TDTheme.colors.background)
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onMonthBack, modifier = Modifier.size(40.dp)) {
                Icon(
                    tdPainter(R.drawable.ic_arrow_back),
                    tint = TDTheme.colors.onBackground,
                    contentDescription = "Previous Month",
                )
            }
            Spacer(Modifier.weight(1f))
            TDText(
                text = "${
                    selectedMonth.month
                        .getDisplayName(TextStyle.FULL, locale)
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
                } ${selectedMonth.year}",
                style = TDTheme.typography.heading5,
                color = TDTheme.colors.onBackground,
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onMonthForward, modifier = Modifier.size(40.dp)) {
                Icon(
                    tdPainter(R.drawable.ic_arrow_forward),
                    tint = TDTheme.colors.onBackground,
                    contentDescription = "Next Month",
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            daysOfWeek.forEach { day ->
                TDText(
                    text = day,
                    color = TDTheme.colors.onBackground,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = TDTheme.typography.dayOfTheCalendar,
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 1.dp)

        calendarGridWeeks(selectedMonth).forEach { week ->
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                week.forEach { currentDate ->
                    val isFromCurrentMonth =
                        currentDate.year == selectedMonth.year && currentDate.month == selectedMonth.month
                    // With a range, both ends read as "selected"; the days between only get the band.
                    val isRangeEnd = rangeEnd != null && currentDate == rangeEnd
                    val isSelected = selectedDate == currentDate || isRangeEnd
                    val isAnchor = anchorDate == currentDate && rangeEnd == null
                    TDAnimatedCell(
                        modifier = Modifier.weight(1f),
                        backgroundColor = when {
                            isSelected -> TDTheme.colors.pendingGray
                            isAnchor -> TDTheme.colors.lightPending
                            else -> Color.Transparent
                        },
                        band = rangeBandFor(currentDate, selectedDate, rangeEnd),
                        // The selection colour faded, not a fixed token: pendingGray is the selected
                        // day's fill, so it is already guaranteed to contrast with the background in
                        // both themes and all three palette kits. lightPending was #1A1A1A on a
                        // #0D0D0D dialog in dark mode — 13 levels apart, effectively invisible.
                        bandColor = TDTheme.colors.pendingGray.copy(alpha = RANGE_BAND_ALPHA),
                        delayMillis = 0,
                        onClick = { if (isSelected && rangeEnd == null) onDayDeselect() else onDaySelect(currentDate) },
                        onLongClick = onDayLongPress?.let { press -> { press(currentDate) } },
                    ) {
                        Text(
                            text = currentDate.dayOfMonth.toString(),
                            color =
                            when {
                                !isFromCurrentMonth -> TDTheme.colors.gray.copy(alpha = 0.35f)
                                isSelected -> Color.White
                                else -> TDTheme.colors.gray
                            },
                            style = TDTheme.typography.subheading4,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TDAnimatedCell(
    modifier: Modifier,
    backgroundColor: Color,
    delayMillis: Int,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    band: RangeBand = RangeBand.NONE,
    bandColor: Color = Color.Transparent,
    content: @Composable () -> Unit,
) {
    val animatedColor by animateColorAsState(
        targetValue = backgroundColor,
        animationSpec = tween(durationMillis = 220, delayMillis = delayMillis),
        label = "td_animated_cell_bg",
    )

    Box(
        // Drawn behind rather than as a background so the two ends can cover HALF the cell: a
        // full-width band spills out past the 36.dp circle and shows a square edge beside it.
        // Height matches the circle exactly, so neighbouring cells join into one unbroken bar.
        modifier = modifier
            .drawBehind {
                if (band == RangeBand.NONE) return@drawBehind
                val barHeight = CELL_DIAMETER_DP.dp.toPx()
                val radius = barHeight / 2f
                val top = (size.height - barHeight) / 2f
                val circleLeft = (size.width - barHeight) / 2f
                // The bar starts and ends at the CIRCLE's edge, not the cell's centre, and its outer
                // corners are rounded to the circle's own radius. That makes the cap concentric with
                // the day circle, so the two read as one pill — square caps left visible corners
                // poking out where the circle's curve pulled away from the straight edge.
                val left = if (band == RangeBand.START) circleLeft else 0f
                val right = if (band == RangeBand.END) circleLeft + barHeight else size.width
                val capStart = if (band == RangeBand.START) CornerRadius(radius) else CornerRadius.Zero
                val capEnd = if (band == RangeBand.END) CornerRadius(radius) else CornerRadius.Zero
                drawPath(
                    path = Path().apply {
                        addRoundRect(
                            RoundRect(
                                left = left,
                                top = top,
                                right = right,
                                bottom = top + barHeight,
                                topLeftCornerRadius = capStart,
                                bottomLeftCornerRadius = capStart,
                                topRightCornerRadius = capEnd,
                                bottomRightCornerRadius = capEnd,
                            ),
                        )
                    },
                    color = bandColor,
                )
            }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(CELL_DIAMETER_DP.dp)
                .background(color = animatedColor, shape = TDTheme.shapes.circle),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

/** The day circle's diameter. The range band matches it so the bar lines up with the circles. */
private const val CELL_DIAMETER_DP = 36

/** Faint enough to read as "between the ends", solid enough to survive a dark background. */
private const val RANGE_BAND_ALPHA = 0.25f

@RequiresApi(Build.VERSION_CODES.O)
@TDPreviewWide
@Composable
fun TDDatePickerPreview() {
    TDTheme {
        Column {
            TDDatePicker(
                selectedDate = LocalDate.of(2025, 3, 5),
                onDaySelect = {},
                onDayDeselect = {},
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@TDPreviewWide
@Composable
private fun TDDatePickerNoSelectionPreview() {
    TDTheme {
        Column {
            TDDatePicker(
                selectedDate = null,
                selectedMonth = YearMonth.of(2025, 3),
                onDaySelect = {},
                onDayDeselect = {},
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@TDPreviewWide
@Composable
private fun TDDatePickerWithTaskDatesPreview() {
    TDTheme {
        Column {
            TDDatePicker(
                selectedDate = LocalDate.of(2025, 3, 12),
                selectedMonth = YearMonth.of(2025, 3),
                taskDates =
                setOf(
                    LocalDate.of(2025, 3, 5),
                    LocalDate.of(2025, 3, 8),
                    LocalDate.of(2025, 3, 14),
                    LocalDate.of(2025, 3, 21),
                    LocalDate.of(2025, 3, 28),
                ),
                onDaySelect = {},
                onDayDeselect = {},
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@TDPreviewWide
@Composable
private fun TDDatePickerWithOverduePreview() {
    TDTheme {
        Column {
            TDDatePicker(
                selectedDate = LocalDate.of(2025, 3, 9),
                selectedMonth = YearMonth.of(2025, 3),
                taskDates =
                setOf(
                    LocalDate.of(2025, 3, 5),
                    LocalDate.of(2025, 3, 20),
                ),
                overdueDates =
                setOf(
                    LocalDate.of(2025, 3, 3),
                    LocalDate.of(2025, 3, 9),
                    LocalDate.of(2025, 3, 15),
                ),
                hasOverdueBeforeDisplayedMonth = true,
                onDaySelect = {},
                onDayDeselect = {},
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@TDPreviewWide
@Composable
private fun TDDatePickerSingleInputPreview() {
    TDTheme {
        Column {
            TDDatePickerSingleInput(
                selectedMonth = YearMonth.of(2025, 3),
                selectedDate = LocalDate.of(2025, 3, 5),
                onDaySelect = {},
                onDayDeselect = {},
            )
        }
    }
}
