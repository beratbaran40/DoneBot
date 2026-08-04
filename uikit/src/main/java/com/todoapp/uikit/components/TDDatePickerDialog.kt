package com.todoapp.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.uikit.R
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreviewDialog
import com.todoapp.uikit.theme.TDTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun TDDatePickerDialog(
    modifier: Modifier = Modifier,
    selectedDate: LocalDate? = LocalDate.now(),
    onDateSelect: (LocalDate) -> Unit,
    onDateDeselect: () -> Unit,
    isError: Boolean = false,
    supportingText: String? = null,
    /**
     * Non-null turns on span selection: hold one day, then another, and this fires with the two
     * ordered. Null keeps the picker single-day — no hint, no long-press, no band. One switch.
     */
    onRangeSelect: ((start: LocalDate, end: LocalDate) -> Unit)? = null,
    /** End of the currently selected span, if any. [selectedDate] is its start. */
    rangeEnd: LocalDate? = null,
    /** Shown above the grid to teach the hold gesture. Only rendered when ranges are enabled. */
    rangeHint: String? = null,
    /**
     * The sentence under the grid explaining what the current selection means. Passed as a lambda
     * because :uikit holds no app strings, and the wording depends on which of the three states the
     * picker is in.
     */
    summaryText: (@Composable (start: LocalDate?, end: LocalDate?, awaitingEnd: Boolean) -> String)? = null,
) {
    var selectedMonth by rememberSaveable { mutableStateOf(YearMonth.from(selectedDate ?: LocalDate.now())) }
    var isPickerOpen by rememberSaveable { mutableStateOf(false) }
    // One switch means one switch: with ranges off, a leftover end date from the caller must not
    // draw a band or claim a span in the summary. Callers keep the value around across a
    // repeats/doesn't-repeat toggle, and honouring it here would show a span nobody can edit.
    val effectiveRangeEnd = rangeEnd.takeIf { onRangeSelect != null }
    // The day being held while we wait for the second one. Transient by design: an abandoned anchor
    // should not survive reopening the picker.
    var anchorDate by remember { mutableStateOf<LocalDate?>(null) }

    // Open on the month you are actually looking at. Without this the picker always opened on the
    // current month, so editing a task dated months away started with a pointless scroll.
    // Keyed on the OPEN transition only. Keying on selectedDate too looks harmless but is not:
    // holding the first day also sets selectedDate, which would re-run this and wipe the anchor the
    // hold just created — the span could never be completed.
    LaunchedEffect(isPickerOpen) {
        if (isPickerOpen) {
            selectedMonth = YearMonth.from(selectedDate ?: LocalDate.now())
            anchorDate = null
        }
    }

    // Fires off the state transition, not the tap handler, per the house rule on feedback effects.
    // Only on completion: combinedClickable already emits the platform long-press tick when the
    // anchor is set, and stacking a second buzz there would read as a double vibration.
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(effectiveRangeEnd) {
        if (effectiveRangeEnd != null) haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Top,
    ) {
        TDPickerField(
            title = stringResource(R.string.pick_a_date),
            value =
            selectedDate?.format(
                DateTimeFormatter.ofPattern(
                    "dd MMMM yyyy",
                ),
            ) ?: stringResource(R.string.pick_a_date),
            onClick = { isPickerOpen = true },
            emphasizeValue = selectedDate != null,
            trailingIcon = {
                Icon(
                    painter = tdPainter(id = R.drawable.ic_calendar2),
                    tint = TDTheme.colors.onBackground,
                    contentDescription = null,
                )
            },
            modifier = Modifier.fillMaxWidth(),
            isError = isError,
            supportingText = supportingText,
        )
        if (isPickerOpen) {
            Dialog(
                onDismissRequest = { isPickerOpen = false },
            ) {
                Surface(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 8.dp,
                ) {
                    Column(Modifier.background(TDTheme.colors.background)) {
                        if (onRangeSelect != null && rangeHint != null) {
                            TDText(
                                text = rangeHint,
                                style = TDTheme.typography.subheading1,
                                color = TDTheme.colors.pendingGray,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                        TDDatePickerSingleInput(
                            selectedMonth = selectedMonth,
                            selectedDate = selectedDate,
                            onMonthBack = { selectedMonth = selectedMonth.minusMonths(1) },
                            onMonthForward = { selectedMonth = selectedMonth.plusMonths(1) },
                            onDaySelect = { day ->
                                // A plain tap always means "one day", so it clears any span.
                                anchorDate = null
                                onDateSelect(day)
                            },
                            onDayDeselect = onDateDeselect,
                            rangeEnd = effectiveRangeEnd,
                            anchorDate = anchorDate,
                            onDayLongPress = onRangeSelect?.let { commit ->
                                { day ->
                                    val anchor = anchorDate
                                    if (anchor == null) {
                                        // First hold: remember it and wait. Holding again elsewhere
                                        // after a completed span starts a fresh one.
                                        anchorDate = day
                                        onDateSelect(day)
                                    } else {
                                        anchorDate = null
                                        commit(minOf(anchor, day), maxOf(anchor, day))
                                    }
                                }
                            },
                        )
                        summaryText?.let { format ->
                            TDText(
                                text = format(selectedDate, effectiveRangeEnd, anchorDate != null && effectiveRangeEnd == null),
                                style = TDTheme.typography.subheading1,
                                color = TDTheme.colors.onBackground,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                        Row(
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(TDTheme.colors.background)
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            TDButton(
                                text = stringResource(R.string.today),
                                type = TDButtonType.OUTLINE,
                                size = TDButtonSize.SMALL,
                                onClick = {
                                    onDateSelect(LocalDate.now())
                                    isPickerOpen = false
                                },
                            )
                            TDButton(
                                text = stringResource(R.string.ok),
                                onClick = { isPickerOpen = false },
                                size = TDButtonSize.SMALL,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@TDPreviewDialog
@Composable
fun TDDatePickerDialogPreview() {
    TDTheme {
        var selectedDate by remember { mutableStateOf(LocalDate.now()) }

        TDDatePickerDialog(
            selectedDate = selectedDate,
            onDateSelect = { selectedDate = it },
            onDateDeselect = { selectedDate = null },
        )
    }
}

@TDPreviewDialog
@Composable
private fun TDDatePickerDialogEmptyPreview() {
    TDTheme {
        TDDatePickerDialog(
            selectedDate = null,
            onDateSelect = {},
            onDateDeselect = {},
        )
    }
}

@TDPreviewDialog
@Composable
private fun TDDatePickerDialogErrorPreview() {
    TDTheme {
        TDDatePickerDialog(
            selectedDate = null,
            onDateSelect = {},
            onDateDeselect = {},
            isError = true,
            supportingText = "Date is required",
        )
    }
}

@TDPreviewDialog
@Composable
private fun TDDatePickerDialogRangePreview() {
    TDTheme {
        val start = LocalDate.now()
        TDDatePickerDialog(
            selectedDate = start,
            rangeEnd = start.plusDays(12),
            onDateSelect = {},
            onDateDeselect = {},
            onRangeSelect = { _, _ -> },
            rangeHint = "Hold two different days to set how long this task runs",
            summaryText = { from, to, awaitingEnd ->
                when {
                    awaitingEnd -> "Now hold a second day to set the end."
                    to != null -> "This task runs between $from and $to."
                    else -> "This is a one-day task: $from."
                }
            },
        )
    }
}
