package com.todoapp.mobile.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * How far a bounded routine has come: "day 12 of 30".
 *
 * Both functions are pure and cheap for the shapes that actually carry a scheduled end (a daily
 * medicine course, a weekday study plan), but the by-weekday case has to count real firing days.
 * Compute these in a ViewModel or repository and pass the numbers down — never call them from inside
 * a composable, where they would re-run on every recomposition of every list item.
 */
private const val MAX_PROGRESS_WINDOW_DAYS = 400

/**
 * 1-based position of [day] among the days this rule fires on, counting from [anchor].
 *
 * Null when [day] isn't a firing day at all, or when the span is too wide to count (see
 * [MAX_PROGRESS_WINDOW_DAYS]) — the caller simply hides the progress indicator.
 */
fun RecurrenceRule.occurrenceIndex(
    anchor: LocalDate,
    day: LocalDate,
    finishedOn: LocalDate? = null,
): Int? {
    if (!firesOn(anchor, day, finishedOn)) return null
    val step = interval.coerceAtLeast(1)
    return when (frequency) {
        Recurrence.NONE -> 1
        Recurrence.DAILY -> (ChronoUnit.DAYS.between(anchor, day) / step + 1).toInt()
        Recurrence.WEEKLY -> weeklyOccurrenceIndex(anchor, day, finishedOn, step)
        Recurrence.MONTHLY ->
            (ChronoUnit.MONTHS.between(anchor.withDayOfMonth(1), day.withDayOfMonth(1)) / step + 1).toInt()
        Recurrence.YEARLY -> (day.year - anchor.year) / step + 1
    }
}

/**
 * Total number of days this rule will ever fire on, i.e. the "30" in "day 12 of 30".
 *
 * Null when the routine is open-ended ([RecurrenceRule.until] is null) — an unbounded routine has no
 * total, which is exactly when the progress indicator should not render. A [finishedOn] earlier than
 * the scheduled end wins, so retiring a routine early shrinks its total to what actually ran.
 */
fun RecurrenceRule.occurrenceTotal(
    anchor: LocalDate,
    finishedOn: LocalDate? = null,
): Int? {
    val scheduledEnd = until ?: return null
    val end = if (finishedOn != null && finishedOn.isBefore(scheduledEnd)) finishedOn else scheduledEnd
    if (end.isBefore(anchor)) return 0
    // Walk back from the end to the last day the rule actually fires, then reuse the index math.
    // Bounded because a wide interval (e.g. YEARLY every 3 years) can leave a long tail of dead days.
    var cursor = end
    var guard = 0
    while (guard < MAX_PROGRESS_WINDOW_DAYS && !cursor.isBefore(anchor)) {
        occurrenceIndex(anchor, cursor, finishedOn)?.let { return it }
        cursor = cursor.minusDays(1)
        guard++
    }
    return if (cursor.isBefore(anchor)) 0 else null
}

/**
 * Without a by-weekday set every week contributes exactly one firing day, so the index is the week
 * count divided by the interval. With one, the days per week vary and the only correct answer is to
 * count them — bounded, since a progress indicator is only ever shown for a scheduled span.
 */
private fun RecurrenceRule.weeklyOccurrenceIndex(
    anchor: LocalDate,
    day: LocalDate,
    finishedOn: LocalDate?,
    step: Int,
): Int? {
    if (byDay.isEmpty()) return (isoWeeksBetween(anchor, day) / step + 1).toInt()
    if (ChronoUnit.DAYS.between(anchor, day) >= MAX_PROGRESS_WINDOW_DAYS) return null
    var count = 0
    var cursor = anchor
    while (!cursor.isAfter(day)) {
        if (firesOn(anchor, cursor, finishedOn)) count++
        cursor = cursor.plusDays(1)
    }
    return count
}
