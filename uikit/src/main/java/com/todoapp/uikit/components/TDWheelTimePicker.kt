package com.todoapp.uikit.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme
import com.todoapp.uikit.util.deviceUses24HourClock
import kotlinx.coroutines.flow.distinctUntilChanged
import java.text.DateFormatSymbols
import java.util.Locale

/**
 * Two- (or three-) column spinning time picker.
 *
 * The public contract is always **24-hour** (`hour` in 0..23, `onHourChange` emits 0..23),
 * regardless of what the wheels display. When [is24Hour] is false the hour column shows
 * 12, 1..11 and an AM/PM toggle is added; the 12h↔24h conversion happens entirely inside
 * this composable so every caller stays unchanged.
 */
@Composable
fun TDWheelTimePicker(
    modifier: Modifier = Modifier,
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    is24Hour: Boolean = deviceUses24HourClock(LocalContext.current),
) {
    val itemHeight = 56.dp
    val visibleHeight = itemHeight * VISIBLE_ITEMS

    val hourFormat: (Int) -> String =
        if (is24Hour) {
            { value -> "%02d".format(value) }
        } else {
            { value -> if (value == 0) "12" else value.toString() }
        }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WheelColumn(
            count = if (is24Hour) 24 else 12,
            selected = if (is24Hour) hour else hour % 12,
            onSelected = { position ->
                onHourChange(
                    if (is24Hour) position else (if (hour >= 12) 12 else 0) + position,
                )
            },
            itemHeight = itemHeight,
            visibleHeight = visibleHeight,
            format = hourFormat,
        )

        TDText(
            text = ":",
            style = TDTheme.typography.heading2.copy(fontWeight = FontWeight.Bold),
            color = TDTheme.colors.onBackground,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        WheelColumn(
            count = 60,
            selected = minute,
            onSelected = onMinuteChange,
            itemHeight = itemHeight,
            visibleHeight = visibleHeight,
            format = { "%02d".format(it) },
        )

        if (!is24Hour) {
            Spacer(modifier = Modifier.width(8.dp))
            val amPmLabels = remember { DateFormatSymbols.getInstance(Locale.getDefault()).amPmStrings }
            AmPmToggle(
                isPM = hour >= 12,
                amLabel = amPmLabels.getOrNull(0)?.takeIf { it.isNotBlank() } ?: "AM",
                pmLabel = amPmLabels.getOrNull(1)?.takeIf { it.isNotBlank() } ?: "PM",
                itemHeight = itemHeight,
                onSelect = { pm -> onHourChange((if (pm) 12 else 0) + (hour % 12)) },
            )
        }
    }
}

@Composable
private fun AmPmToggle(
    isPM: Boolean,
    amLabel: String,
    pmLabel: String,
    itemHeight: Dp,
    onSelect: (Boolean) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AmPmChip(label = amLabel, selected = !isPM, itemHeight = itemHeight, onClick = { onSelect(false) })
        AmPmChip(label = pmLabel, selected = isPM, itemHeight = itemHeight, onClick = { onSelect(true) })
    }
}

@Composable
private fun AmPmChip(
    label: String,
    selected: Boolean,
    itemHeight: Dp,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
        Modifier
            .width(64.dp)
            .height(itemHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) TDTheme.colors.pendingGray.copy(alpha = 0.1f) else Color.Transparent,
            ).clickable { onClick() },
    ) {
        TDText(
            text = label,
            maxLines = 1,
            style =
            if (selected) {
                TDTheme.typography.heading3.copy(fontWeight = FontWeight.Bold)
            } else {
                TDTheme.typography.heading4
            },
            color =
            if (selected) {
                TDTheme.colors.pendingGray
            } else {
                TDTheme.colors.onBackground.copy(alpha = 0.3f)
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelColumn(
    count: Int,
    selected: Int,
    onSelected: (Int) -> Unit,
    itemHeight: Dp,
    visibleHeight: Dp,
    format: (Int) -> String,
) {
    // Read the latest onSelected inside the long-running scroll collector; in 12h mode the
    // caller's lambda closes over the current hour (for the AM/PM offset), which changes.
    val currentOnSelected by rememberUpdatedState(onSelected)
    val totalItems = count * CYCLE_MULTIPLIER
    val baseIndex = (totalItems / 2) - ((totalItems / 2) % count)
    val initialIndex = baseIndex + selected

    val listState =
        rememberLazyListState(
            initialFirstVisibleItemIndex = initialIndex - VISIBLE_ITEMS / 2,
        )
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    var lastReportedValue by remember { mutableIntStateOf(selected) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling ->
                if (!scrolling) {
                    val settled = listState.settledCenterValue(count)
                    if (settled != lastReportedValue) {
                        lastReportedValue = settled
                        currentOnSelected(settled)
                    }
                }
            }
    }

    LaunchedEffect(selected) {
        if (selected != lastReportedValue) {
            lastReportedValue = selected
            val targetIndex = baseIndex + selected - VISIBLE_ITEMS / 2
            listState.animateScrollToItem(targetIndex)
        }
    }

    val centeredIndex by remember { derivedStateOf { listState.settledCenterIndex() } }
    val isScrolling by remember { derivedStateOf { listState.isScrollInProgress } }

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier =
            Modifier
                .width(80.dp)
                .height(itemHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(TDTheme.colors.pendingGray.copy(alpha = 0.1f)),
        )

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier =
            Modifier
                .width(80.dp)
                .height(visibleHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(totalItems, key = { it }) { index ->
                val value = index % count
                val isCentered = !isScrolling && centeredIndex == index

                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                    Modifier
                        .width(80.dp)
                        .height(itemHeight),
                ) {
                    TDText(
                        text = format(value),
                        style =
                        if (isCentered) {
                            TDTheme.typography.heading2.copy(fontWeight = FontWeight.Bold)
                        } else {
                            TDTheme.typography.heading4
                        },
                        color =
                        if (isCentered) {
                            TDTheme.colors.pendingGray
                        } else {
                            TDTheme.colors.onBackground.copy(alpha = 0.3f)
                        },
                    )
                }
            }
        }
    }
}

private fun LazyListState.settledCenterIndex(): Int {
    val info = layoutInfo
    val center =
        info.viewportStartOffset +
            (info.viewportEndOffset - info.viewportStartOffset) / 2
    return info.visibleItemsInfo
        .minByOrNull {
            kotlin.math.abs((it.offset + it.size / 2) - center)
        }?.index ?: 0
}

private fun LazyListState.settledCenterValue(count: Int): Int = settledCenterIndex() % count

private const val CYCLE_MULTIPLIER = 1000
private const val VISIBLE_ITEMS = 5

@TDPreview
@Composable
private fun TDWheelTimePicker24hPreview() {
    TDTheme {
        Box(
            Modifier
                .background(TDTheme.colors.background)
                .padding(24.dp),
        ) {
            TDWheelTimePicker(
                hour = 9,
                minute = 0,
                onHourChange = {},
                onMinuteChange = {},
                is24Hour = true,
            )
        }
    }
}

@TDPreview
@Composable
private fun TDWheelTimePicker12hPreview() {
    TDTheme {
        Box(
            Modifier
                .background(TDTheme.colors.background)
                .padding(24.dp),
        ) {
            TDWheelTimePicker(
                hour = 14,
                minute = 30,
                onHourChange = {},
                onMinuteChange = {},
                is24Hour = false,
            )
        }
    }
}
