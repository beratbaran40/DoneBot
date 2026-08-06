@file:Suppress("TooManyFunctions")

package com.todoapp.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.theme.TDTheme
import com.todoapp.uikit.theme.tdCorner
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max

@Composable
fun TDMonthlyDatePicker(
    modifier: Modifier,
    displayedMonth: YearMonth,
    selectedDate: LocalDate? = LocalDate.now(),
    taskDates: Set<LocalDate> = emptySet(),
    overdueDates: Set<LocalDate> = emptySet(),
    hasOverdueBeforeDisplayedMonth: Boolean = false,
    listState: LazyListState = rememberLazyListState(),
    onDateSelect: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    val daysInMonth = displayedMonth.lengthOfMonth()

    LaunchedEffect(displayedMonth, selectedDate) {
        val scrollIndex =
            max(
                0,
                selectedDate
                    ?.takeIf { YearMonth.from(it) == displayedMonth }
                    ?.dayOfMonth
                    ?.minus(4)
                    ?: 0,
            )
        listState.animateScrollToItem(scrollIndex)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        MonthNavigationHeader(
            displayedMonth = displayedMonth,
            hasOverdueBeforeDisplayedMonth = hasOverdueBeforeDisplayedMonth,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            // contentPadding, not a padding modifier: as an outer padding the inset clipped the first
            // and last cards instead of letting them scroll under it, so the strip could never sit flush.
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            state = listState,
        ) {
            items(count = daysInMonth, key = { it }) { i ->
                val date = displayedMonth.atDay(i + 1)
                DatePickerCard(
                    modifier = Modifier,
                    currentDate = date,
                    isSelected = selectedDate == date,
                    hasTask = date in taskDates,
                    hasOverdue = date in overdueDates,
                    onDateSelect = onDateSelect,
                )
            }
        }
    }
}

@Composable
private fun MonthNavigationHeader(
    displayedMonth: YearMonth,
    hasOverdueBeforeDisplayedMonth: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    // Use Locale.getDefault() as a fallback to avoid rendering issues in Previews where locales might be empty
    val configuration = LocalConfiguration.current
    val locale = if (!configuration.locales.isEmpty) configuration.locales[0] else Locale.getDefault()
    val monthLabel = displayedMonth.month.getDisplayName(TextStyle.FULL, locale)
    val yearLabel = displayedMonth.year.toString()
    val overdueBadgeCd = stringResource(R.string.cd_prev_month_has_overdue)

    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box {
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    painter = tdPainter(R.drawable.ic_arrow_back),
                    contentDescription =
                    if (hasOverdueBeforeDisplayedMonth) overdueBadgeCd else "Previous month",
                    tint = TDTheme.colors.onBackground,
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
        TDText(
            // Weighted and capped: unbounded between two 48dp icon buttons, a long month name wrapped
            // the header onto a second line on narrow screens and at larger font scales. The weight is
            // also what centres it — SpaceBetween was doing that job before, and stops once a child
            // takes the free space.
            modifier = Modifier.weight(1f),
            text = "$monthLabel $yearLabel",
            style = TDTheme.typography.heading4,
            color = TDTheme.colors.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = onNextMonth) {
            Icon(
                painter = tdPainter(R.drawable.ic_arrow_forward),
                contentDescription = "Next month",
                tint = TDTheme.colors.onBackground,
            )
        }
    }
}

@Composable
internal fun DatePickerCard(
    modifier: Modifier,
    currentDate: LocalDate,
    isSelected: Boolean,
    hasTask: Boolean = false,
    hasOverdue: Boolean = false,
    onDateSelect: (LocalDate) -> Unit = {},
) {
    val textColor = if (isSelected) TDTheme.colors.white else TDTheme.colors.lightGray
    val overdueCd = stringResource(R.string.cd_day_has_overdue)
    val hasTaskCd = stringResource(R.string.cd_day_has_tasks)
    val barColor: Color? = when {
        hasOverdue -> TDTheme.colors.crossRed
        hasTask && isSelected -> TDTheme.colors.white
        hasTask -> TDTheme.colors.pendingGray
        else -> null
    }
    val barCd: String? = when {
        hasOverdue -> overdueCd
        hasTask -> hasTaskCd
        else -> null
    }
    Column(
        modifier =
        modifier
            .background(
                shape = TDTheme.shapes.medium,
                color = if (isSelected) TDTheme.colors.pendingGray.copy(alpha = 0.8f) else Color.Transparent,
            )
            // Minimums, not a fixed size. A hard 48dp left the labels exactly 32dp of text width, and
            // "Wed"/"Cmt" at 14sp measure ~31dp — under a dp of headroom, and none at all once the
            // system font scales up or the PIXEL kit swaps in its wider face; the label then wrapped
            // inside the card. The 80dp height clipped the day number the same way. A LazyRow item
            // that sizes to its content costs nothing, and unlike an ellipsis it keeps the three
            // letters the cell exists to show.
            .widthIn(min = 48.dp)
            .heightIn(min = 80.dp)
            .clickable(
                onClick = { onDateSelect(currentDate) },
            )
            .padding(vertical = 4.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Modifier, not `modifier`: the incoming one already carries the background, click and padding
        // above, and replaying that chain into four spacers is a trap waiting for the first caller
        // that passes something.
        Spacer(Modifier.weight(0.8f))
        TDText(
            text = shortDayOfWeekLabel(currentDate.dayOfWeek),
            style = TDTheme.typography.regularTextStyle,
            color = textColor,
            maxLines = 1,
        )
        Spacer(Modifier.weight(0.2f))
        TDText(
            text = currentDate.dayOfMonth.toString(),
            style = TDTheme.typography.heading4,
            color = textColor,
            maxLines = 1,
        )
        Spacer(Modifier.weight(1f))
        if (barColor != null) {
            Box(
                modifier =
                Modifier
                    .height(4.dp)
                    .width(32.dp)
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
        Spacer(Modifier.weight(0.3f))
    }
}

@Composable
private fun shortDayOfWeekLabel(day: DayOfWeek): String {
    val resId = when (day) {
        DayOfWeek.MONDAY -> R.string.weekday_abbr_mon
        DayOfWeek.TUESDAY -> R.string.weekday_abbr_tue
        DayOfWeek.WEDNESDAY -> R.string.weekday_abbr_wed
        DayOfWeek.THURSDAY -> R.string.weekday_abbr_thu
        DayOfWeek.FRIDAY -> R.string.weekday_abbr_fri
        DayOfWeek.SATURDAY -> R.string.weekday_abbr_sat
        DayOfWeek.SUNDAY -> R.string.weekday_abbr_sun
    }
    return stringResource(resId)
}
