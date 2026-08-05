package com.todoapp.uikit.components

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** What a tap on a calendar day should leave selected. */
sealed interface DayTapOutcome {
    data class SelectSingle(val day: LocalDate) : DayTapOutcome

    data class SelectSpan(val start: LocalDate, val end: LocalDate) : DayTapOutcome
}

/**
 * The whole tap grammar of the range picker, as one pure function.
 *
 * Long-press used to be needed for **both** ends of a span, which read as a chore — you had to
 * remember to keep holding for the second day. Now long-press only *starts* a span and a plain tap
 * finishes it, so the gesture is: hold once, then tap.
 *
 * With a span already on screen a tap means one of two things, decided by where it lands:
 * - **outside** it — the user is done with the span; drop back to a single day
 * - **inside** it — the user is trimming; move whichever end is nearer to the tapped day
 *
 * Keeping this out of the composable is deliberate: it is four interacting states (anchor / span /
 * inside / outside) whose only observable behaviour is which callback fires, which is exactly the
 * kind of thing that rots silently inside a click lambda.
 */
fun resolveDayTap(
    day: LocalDate,
    selectedDate: LocalDate?,
    rangeEnd: LocalDate?,
    anchorDate: LocalDate?,
): DayTapOutcome = if (anchorDate != null) finishHeldSpan(day, anchorDate) else retargetSpan(day, selectedDate, rangeEnd)

/** Second half of a hold-then-tap. Tapping the held day itself means "never mind". */
private fun finishHeldSpan(day: LocalDate, anchor: LocalDate): DayTapOutcome = if (day == anchor) {
    DayTapOutcome.SelectSingle(day)
} else {
    DayTapOutcome.SelectSpan(minOf(anchor, day), maxOf(anchor, day))
}

/** What confirming the picker should push back to the caller. */
sealed interface DatePickerCommit {
    /** The draft says the same thing the caller already holds — announce nothing. */
    data object Nothing : DatePickerCommit

    data object Deselect : DatePickerCommit

    /**
     * [clearRange] is set when the caller still holds a span the draft has dropped: the single-day
     * callback on its own would move the start and leave the old end stranded behind it.
     */
    data class Single(val day: LocalDate, val clearRange: Boolean) : DatePickerCommit

    data class Span(val start: LocalDate, val end: LocalDate) : DatePickerCommit
}

/**
 * The other half of the grammar: what "OK" means.
 *
 * The dialog edits a draft and writes nothing until it is confirmed, so committing has to compare two
 * selections — the draft and what the caller already holds — each of which can be nothing, one day or
 * a span. Same reasoning that keeps [resolveDayTap] out of a click lambda: the only observable
 * behaviour is which callback fires, and that rots silently inside a composable.
 *
 * Returning [DatePickerCommit.Nothing] for an unchanged draft matters. Opening the picker and
 * confirming without touching anything must not re-announce the date the caller gave us; a form that
 * diffs against its loaded task would otherwise be free to call itself edited.
 */
fun resolveCommit(
    draftDate: LocalDate?,
    draftEnd: LocalDate?,
    committedDate: LocalDate?,
    committedEnd: LocalDate?,
    rangesEnabled: Boolean,
): DatePickerCommit {
    // An end only means something where spans exist at all — on both sides. The four single-day
    // pickers must never be handed one to commit, nor be told to clear one they cannot hold, even if
    // restored state carried a stale value in.
    val end = draftEnd.takeIf { rangesEnabled }
    val committed = committedEnd.takeIf { rangesEnabled }
    return when {
        draftDate == committedDate && end == committed -> DatePickerCommit.Nothing
        draftDate == null -> DatePickerCommit.Deselect
        end != null -> DatePickerCommit.Span(draftDate, end)
        else -> DatePickerCommit.Single(draftDate, clearRange = committed != null)
    }
}

private fun retargetSpan(day: LocalDate, start: LocalDate?, end: LocalDate?): DayTapOutcome {
    // Either there is no usable span, or the tap landed outside it — both mean the user wants a
    // single day. (A malformed span can only come from restored state; treat it as no span.)
    val hasSpan = start != null && end != null && end.isAfter(start)
    if (!hasSpan || day.isBefore(start) || day.isAfter(end)) return DayTapOutcome.SelectSingle(day)

    // Inside: drag the nearer end. Ties go to the start, so the result is deterministic rather than
    // depending on which comparison happens to run first.
    //
    // This can never collapse the span: the guard above proved end > start, and whichever end moves,
    // it moves onto a day strictly between the two (tapping an endpoint takes the branch that leaves
    // that endpoint where it is). So there is no zero-length case to handle here.
    val toStart = ChronoUnit.DAYS.between(start, day)
    val toEnd = ChronoUnit.DAYS.between(day, end)
    return if (toStart <= toEnd) {
        DayTapOutcome.SelectSpan(day, end)
    } else {
        DayTapOutcome.SelectSpan(start, day)
    }
}
