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
