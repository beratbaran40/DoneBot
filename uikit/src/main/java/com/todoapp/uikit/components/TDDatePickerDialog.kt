package com.todoapp.uikit.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
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
     * Non-null turns on span selection: **hold** a day to start one, then **tap** another to finish
     * it, and this fires with the two ordered. Also fires when a tap trims an existing span (see
     * [resolveDayTap]). Null keeps the picker single-day — no hint, no long-press, no band.
     */
    onRangeSelect: ((start: LocalDate, end: LocalDate) -> Unit)? = null,
    /** End of the currently selected span, if any. [selectedDate] is its start. */
    rangeEnd: LocalDate? = null,
    /**
     * Fired when a plain tap lands while a span is showing — "I want a single day again".
     *
     * Separate from [onDateSelect] because that also fires when the FIRST day of a new span is held,
     * and because changing only the start date must not silently discard an end the user picked from
     * the form's own chips. This callback fires on exactly one intent and no other.
     */
    onRangeClear: (() -> Unit)? = null,
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
            value = spanLabel(selectedDate, effectiveRangeEnd) ?: stringResource(R.string.pick_a_date),
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
                            LeadingIconRow(
                                icon = R.drawable.ic_info,
                                iconTint = TDTheme.colors.pendingGray,
                                text = rangeHint,
                                textColor = TDTheme.colors.pendingGray,
                                // Asymmetric on purpose so the hint LOOKS centred in its band: the
                                // grid below already contributes 16.dp of its own top padding, so an
                                // even 8/8 left the text pinned to the dialog's edge with a wide gap
                                // underneath. 20 above + (4 + the grid's 16) below balances it.
                                padding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
                            )
                        }
                        TDDatePickerSingleInput(
                            selectedMonth = selectedMonth,
                            selectedDate = selectedDate,
                            onMonthBack = { selectedMonth = selectedMonth.minusMonths(1) },
                            onMonthForward = { selectedMonth = selectedMonth.plusMonths(1) },
                            onDaySelect = { day ->
                                // The whole grammar lives in resolveDayTap: finish a span that is
                                // mid-gesture, trim one that already exists, or fall back to a plain
                                // single day. With ranges off it always returns SelectSingle, so the
                                // other four pickers behave exactly as before.
                                when (
                                    val outcome =
                                        resolveDayTap(day, selectedDate, effectiveRangeEnd, anchorDate)
                                ) {
                                    is DayTapOutcome.SelectSingle -> {
                                        anchorDate = null
                                        if (effectiveRangeEnd != null) onRangeClear?.invoke()
                                        onDateSelect(outcome.day)
                                    }
                                    is DayTapOutcome.SelectSpan -> {
                                        anchorDate = null
                                        onRangeSelect?.invoke(outcome.start, outcome.end)
                                    }
                                }
                            },
                            onDayDeselect = onDateDeselect,
                            rangeEnd = effectiveRangeEnd,
                            anchorDate = anchorDate,
                            // Long-press now only STARTS a span; the tap above finishes it. Holding
                            // for both ends meant remembering to keep holding for the second day,
                            // which is a chore for something a tap can say just as clearly.
                            onDayLongPress = onRangeSelect?.let {
                                { day ->
                                    if (effectiveRangeEnd != null) onRangeClear?.invoke()
                                    anchorDate = day
                                    onDateSelect(day)
                                }
                            },
                        )
                        summaryText?.let { format ->
                            LeadingIconRow(
                                icon = R.drawable.ic_warning,
                                iconTint = TDTheme.colors.orange,
                                text = format(
                                    selectedDate,
                                    effectiveRangeEnd,
                                    anchorDate != null && effectiveRangeEnd == null,
                                ),
                                // The icon and its sentence are one statement, so they share a colour —
                                // an orange glyph beside plain ink read as two unrelated things.
                                textColor = TDTheme.colors.orange,
                            )
                        }
                        Row(
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(TDTheme.colors.background)
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            // Equal halves with a gap between them. SpaceBetween sized each button to
                            // its own label, so "Bugün" and "Tamam" came out different widths and sat
                            // shoulder to shoulder in the middle. fullWidth makes each fill its half
                            // rather than fight the 140.dp minimum on a narrow screen.
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            TDButton(
                                modifier = Modifier.weight(1f),
                                fullWidth = true,
                                text = stringResource(R.string.today),
                                type = TDButtonType.OUTLINE,
                                size = TDButtonSize.SMALL,
                                // Exactly what the label says: today, as a single day. It used to fire
                                // onDateSelect alone, which left an existing span's END behind — the
                                // field then read "today – <old end>", and an end before the start makes
                                // firesOn reject every day, so the task saved but never appeared.
                                //
                                // It also no longer dismisses. Tapping a day cell doesn't, so having the
                                // one shortcut ALSO mean "confirm and exit" is what made a selection the
                                // user was still working on feel committed. Staying open costs the anchor
                                // its old escape route (reopening cleared it), hence the explicit reset:
                                // otherwise the next tap would finish a span from an abandoned hold.
                                onClick = {
                                    val today = LocalDate.now()
                                    anchorDate = null
                                    if (effectiveRangeEnd != null) onRangeClear?.invoke()
                                    selectedMonth = YearMonth.from(today)
                                    onDateSelect(today)
                                },
                            )
                            TDButton(
                                modifier = Modifier.weight(1f),
                                fullWidth = true,
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

/**
 * An icon and a line of text, centred against each other.
 *
 * The two sentences around the grid are both explanations, and a bare paragraph in a dialog full of
 * numbers is easy to skip; the glyph gives each one an anchor. `CenterVertically` matters when the
 * text wraps — with top alignment the icon floats beside the first line and reads as detached.
 */
@Composable
private fun LeadingIconRow(
    @DrawableRes icon: Int,
    iconTint: Color,
    text: String,
    textColor: Color,
    padding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(padding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp),
        )
        TDText(
            text = text,
            style = TDTheme.typography.subheading1,
            color = textColor,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * What the collapsed field says. With a span it must name BOTH ends — showing only the start read as
 * "this is a one-day task" for something the user had just told us runs for a month.
 *
 * Shared month or year is folded away ("10 – 20 August 2026") because the field is one line and the
 * full form is close to twice as long. The separator is an en dash, so nothing here needs translating
 * — :uikit holds no app strings.
 */
private fun spanLabel(start: LocalDate?, end: LocalDate?): String? {
    start ?: return null
    val full = DateTimeFormatter.ofPattern(FULL_DATE_PATTERN)
    val dayOnly = DateTimeFormatter.ofPattern(DAY_ONLY_PATTERN)
    val dayAndMonth = DateTimeFormatter.ofPattern(DAY_MONTH_PATTERN)
    return when {
        end == null -> start.format(full)
        start.year == end.year && start.month == end.month ->
            "${start.format(dayOnly)} – ${end.format(full)}"
        start.year == end.year -> "${start.format(dayAndMonth)} – ${end.format(full)}"
        else -> "${start.format(full)} – ${end.format(full)}"
    }
}

private const val FULL_DATE_PATTERN = "dd MMMM yyyy"
private const val DAY_ONLY_PATTERN = "dd"
private const val DAY_MONTH_PATTERN = "dd MMMM"

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
            rangeHint = "Hold a day to start, then tap another to set how long this task runs",
            summaryText = { from, to, awaitingEnd ->
                when {
                    awaitingEnd -> "Now tap a second day to set the end."
                    to != null -> "This task runs between $from and $to."
                    else -> "This is a one-day task: $from."
                }
            },
        )
    }
}

/**
 * A span that crosses a month boundary, which is where the collapsed field's label has to fall back
 * to naming the month twice. @TDPreviewDialog renders light AND dark in one pass, so this is also
 * the preview that would catch the band disappearing on a dark background again.
 */
@TDPreviewDialog
@Composable
private fun TDDatePickerDialogRangeAcrossMonthsPreview() {
    TDTheme {
        val start = LocalDate.of(2026, 8, 28)
        TDDatePickerDialog(
            selectedDate = start,
            rangeEnd = LocalDate.of(2026, 9, 3),
            onDateSelect = {},
            onDateDeselect = {},
            onRangeSelect = { _, _ -> },
            onRangeClear = {},
            rangeHint = "Hold a day to start, then tap another to set how long this task runs",
            summaryText = { from, to, _ -> "This task runs between $from and $to." },
        )
    }
}

/** Two adjacent days — the tightest span, where the two half-bands have to meet with no gap. */
@TDPreviewDialog
@Composable
private fun TDDatePickerDialogAdjacentRangePreview() {
    TDTheme {
        val start = LocalDate.of(2026, 8, 12)
        TDDatePickerDialog(
            selectedDate = start,
            rangeEnd = start.plusDays(1),
            onDateSelect = {},
            onDateDeselect = {},
            onRangeSelect = { _, _ -> },
            onRangeClear = {},
            summaryText = { from, to, _ -> "This task runs between $from and $to." },
        )
    }
}
