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
import androidx.compose.runtime.mutableIntStateOf
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
import com.todoapp.uikit.theme.TDTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * A collapsed date field that opens a calendar dialog.
 *
 * The dialog edits a **draft** and tells the caller nothing until "OK" is pressed; "Cancel", a tap
 * outside and the back gesture all drop it. It used to write straight through on every tap, which
 * left "OK" with nothing to confirm and no way to offer a Cancel at all — a mistap was permanent, and
 * the one shortcut that did dismiss ("Today") therefore read as the confirm the dialog never had.
 *
 * The callbacks below are unchanged in shape; only their timing moved from per-tap to on-confirm.
 */
@Composable
fun TDDatePickerDialog(
    modifier: Modifier = Modifier,
    selectedDate: LocalDate? = LocalDate.now(),
    onDateSelect: (LocalDate) -> Unit,
    /**
     * Null means the caller cannot hold "no date" — a Creation Hub task always has one — and the
     * picker then refuses to clear the selection at all. It used to clear the draft regardless, so
     * tapping the selected day emptied the grid and the summary line, and OK put the old date
     * straight back: a form visibly lying about its own state.
     */
    onDateDeselect: (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    /**
     * Non-null turns on span selection: **hold** a day to start one, then **tap** another to finish
     * it. Fires on confirm with the two ordered, for a span the draft ended up holding — however it
     * got there, whether by the hold-then-tap gesture or by trimming one that was already showing
     * (see [resolveDayTap]). Null keeps the picker single-day — no hint, no long-press, no band.
     */
    onRangeSelect: ((start: LocalDate, end: LocalDate) -> Unit)? = null,
    /** End of the currently selected span, if any. [selectedDate] is its start. */
    rangeEnd: LocalDate? = null,
    /**
     * Fired on confirm when a span the caller still holds is gone from the draft — "I want a single
     * day again".
     *
     * Separate from [onDateSelect] because that also fires for a start date that simply moved, and
     * because changing only the start must not silently discard an end the user picked from the
     * form's own chips. This callback fires on exactly one intent and no other.
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
    // What the open dialog is editing. Plain remember, like the anchor: an uncommitted draft has no
    // business surviving process death — reopening reseeds it from the caller anyway.
    var draftDate by remember { mutableStateOf(selectedDate) }
    var draftEnd by remember { mutableStateOf(effectiveRangeEnd) }
    // The day being held while we wait for the second one. Transient by design: an abandoned anchor
    // should not survive reopening the picker.
    var anchorDate by remember { mutableStateOf<LocalDate?>(null) }

    // Open on the month you are actually looking at, showing what the caller currently holds. Without
    // this the picker always opened on the current month, so editing a task dated months away started
    // with a pointless scroll.
    // Keyed on the OPEN transition only. Keying on selectedDate too looks harmless but is not:
    // holding the first day also moves the draft, which would re-run this and wipe the anchor the
    // hold just created — the span could never be completed.
    LaunchedEffect(isPickerOpen) {
        if (isPickerOpen) {
            draftDate = selectedDate
            draftEnd = effectiveRangeEnd
            selectedMonth = YearMonth.from(selectedDate ?: LocalDate.now())
            anchorDate = null
        }
    }

    // Fires off the state transition, not the tap handler, per the house rule on feedback effects.
    // Only on completion: combinedClickable already emits the platform long-press tick when the
    // anchor is set, and stacking a second buzz there would read as a double vibration.
    //
    // Keyed on a counter the GESTURE increments, not on draftEnd. draftEnd is also written by the
    // seeding below, so a form that already held a span buzzed merely from opening the picker — a
    // "span completed" tick for a gesture the user never made, repeating on every cancel-and-reopen.
    var spanCompletions by remember { mutableIntStateOf(0) }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(spanCompletions) {
        if (spanCompletions > 0) haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Top,
    ) {
        TDPickerField(
            title = stringResource(R.string.pick_a_date),
            // The collapsed field shows what is COMMITTED, never the draft — it is the answer the
            // form is holding, and it must not move while a dialog the user may cancel is open.
            value = spanLabel(selectedDate, effectiveRangeEnd) ?: stringResource(R.string.pick_a_date),
            // Seed the draft HERE, not in an effect. LaunchedEffect bodies are dispatched after the
            // composition they belong to has already been applied, so seeding there let the whole grid
            // and summary render one frame against the previous draft — the abandoned selection from a
            // cancelled visit flashing up before snapping back. The effect below stays as the restore
            // path for a dialog reopened by the system, where this click never happened.
            onClick = {
                draftDate = selectedDate
                draftEnd = effectiveRangeEnd
                selectedMonth = YearMonth.from(selectedDate ?: LocalDate.now())
                anchorDate = null
                isPickerOpen = true
            },
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
                // Dismissing without confirming is a cancel: the draft dies with the dialog.
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
                            selectedDate = draftDate,
                            onMonthBack = { selectedMonth = selectedMonth.minusMonths(1) },
                            onMonthForward = { selectedMonth = selectedMonth.plusMonths(1) },
                            onDaySelect = { day ->
                                // The whole grammar lives in resolveDayTap: finish a span that is
                                // mid-gesture, trim one that already exists, or fall back to a plain
                                // single day. With ranges off it always returns SelectSingle, so the
                                // other four pickers behave exactly as before.
                                when (val outcome = resolveDayTap(day, draftDate, draftEnd, anchorDate)) {
                                    is DayTapOutcome.SelectSingle -> {
                                        anchorDate = null
                                        draftDate = outcome.day
                                        draftEnd = null
                                    }
                                    is DayTapOutcome.SelectSpan -> {
                                        anchorDate = null
                                        draftDate = outcome.start
                                        draftEnd = outcome.end
                                        spanCompletions++
                                    }
                                }
                            },
                            onDayDeselect = {
                                // Only where the caller can actually act on it (see onDateDeselect).
                                if (onDateDeselect != null) {
                                    draftDate = null
                                    draftEnd = null
                                    // Backing out of a half-made hold must retire the anchor with it,
                                    // or the next tap silently rebuilds the span just cancelled — and
                                    // resolveDayTap's "tap the held day = never mind" branch becomes
                                    // unreachable from the real dialog.
                                    anchorDate = null
                                }
                            },
                            rangeEnd = draftEnd,
                            anchorDate = anchorDate,
                            // Long-press now only STARTS a span; the tap above finishes it. Holding
                            // for both ends meant remembering to keep holding for the second day,
                            // which is a chore for something a tap can say just as clearly.
                            onDayLongPress = onRangeSelect?.let {
                                { day ->
                                    anchorDate = day
                                    draftDate = day
                                    draftEnd = null
                                }
                            },
                        )
                        summaryText?.let { format ->
                            LeadingIconRow(
                                icon = R.drawable.ic_warning,
                                iconTint = TDTheme.colors.orange,
                                // Reads the draft, not the caller: this sentence is the running
                                // commentary on what the user is building right now.
                                text = format(draftDate, draftEnd, anchorDate != null && draftEnd == null),
                                // The icon and its sentence are one statement, so they share a colour —
                                // an orange glyph beside plain ink read as two unrelated things.
                                textColor = TDTheme.colors.orange,
                            )
                        }
                        DatePickerActionRow(
                            // Exactly what the label says: today, as a single day. A shortcut that
                            // left an existing span's end behind produced "today – <old end>", and an
                            // end before the start makes firesOn reject every day — a task that saves
                            // and then never appears. It moves the draft and nothing else; the grid
                            // follows so the result is visible instead of happening off-screen.
                            onToday = {
                                val today = LocalDate.now()
                                anchorDate = null
                                draftDate = today
                                draftEnd = null
                                selectedMonth = YearMonth.from(today)
                            },
                            onCancel = { isPickerOpen = false },
                            onConfirm = {
                                when (
                                    val commit =
                                        resolveCommit(
                                            draftDate = draftDate,
                                            draftEnd = draftEnd,
                                            committedDate = selectedDate,
                                            committedEnd = effectiveRangeEnd,
                                            rangesEnabled = onRangeSelect != null,
                                            anchorPending = anchorDate != null && draftEnd == null,
                                        )
                                ) {
                                    DatePickerCommit.Nothing -> Unit
                                    is DatePickerCommit.Deselect -> {
                                        if (commit.clearRange) onRangeClear?.invoke()
                                        onDateDeselect?.invoke()
                                    }
                                    is DatePickerCommit.Single -> {
                                        if (commit.clearRange) onRangeClear?.invoke()
                                        onDateSelect(commit.day)
                                    }
                                    is DatePickerCommit.Span -> onRangeSelect?.invoke(commit.start, commit.end)
                                }
                                isPickerOpen = false
                            },
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

/**
 * "Select today" on the left, the two ways out as squares on the right.
 *
 * Three labelled buttons never fit. TDButton.SMALL claims a 140.dp minimum each, and even weighted
 * over `fullWidth` the two short labels ate a third of a narrow dialog in padding — "Tamam" was
 * wrapping onto a second line on a 385dp phone. Cancel and OK are the two most recognisable glyphs in
 * the product, so they lose their labels and keep their meaning through contentDescription. The
 * shortcut goes the other way and gains words: "Today" on its own read as a confirm, which is the bug
 * this row was rebuilt around in the first place, and the space the squares free up is exactly what
 * pays for the longer label.
 *
 * Cancel stays an outline rather than [TDButtonType.CANCEL]: that type is a red fill, and red is
 * reserved for destructive work. Dropping an uncommitted draft is not destructive.
 *
 * All three children are 48.dp tall — the squares because that is the touch-target minimum, and the
 * label because TDButtonSize.SMALL's 40.dp is only a `heightIn(min=)`, so an explicit height wins and
 * the row reads as one bar rather than a short pill beside two taller blocks.
 */
@Composable
internal fun DatePickerActionRow(
    onToday: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .background(TDTheme.colors.background)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TDButton(
            modifier = Modifier
                .weight(1f)
                .height(TDIconButtonSize),
            fullWidth = true,
            text = stringResource(R.string.date_picker_select_today),
            // Pinned height clips a wrapped label instead of growing, so at a large font scale this
            // has to shorten rather than fold.
            maxLines = 1,
            type = TDButtonType.OUTLINE,
            size = TDButtonSize.SMALL,
            onClick = onToday,
        )
        TDIconButton(
            icon = tdPainter(R.drawable.ic_close),
            contentDescription = stringResource(R.string.cancel),
            type = TDButtonType.OUTLINE,
            onClick = onCancel,
        )
        TDIconButton(
            icon = tdPainter(R.drawable.ic_check),
            contentDescription = stringResource(R.string.ok),
            onClick = onConfirm,
        )
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
