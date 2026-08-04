package com.todoapp.mobile.domain.model

import androidx.compose.runtime.Immutable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class Recurrence {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    ;

    companion object {
        fun fromStorage(value: String?): Recurrence = value?.let { runCatching { valueOf(it) }.getOrNull() } ?: NONE
    }
}

/**
 * A full repeat rule: the base [frequency] plus the RRULE-style refinements a "custom" task needs.
 *
 * The defaults reproduce the legacy enum-only behaviour exactly, which is what lets [Recurrence.firesOn]
 * stay a thin shim over this — pinned by a property test in RecurrenceTest.
 *
 * Deliberately models UNTIL and not COUNT: COUNT is the only RRULE shape that needs to count the
 * occurrences that already fired, which is state-dependent and would stop [firesOn] being a pure
 * `(anchor, day) -> Boolean`. "For one month" and "20 sessions" are both resolved to a concrete end
 * date at creation time.
 */
@Immutable
data class RecurrenceRule(
    val frequency: Recurrence = Recurrence.NONE,
    /** Fire every N periods of [frequency]. 1 = every period (legacy). Values below 1 are treated as 1. */
    val interval: Int = 1,
    /** WEEKLY only: the weekdays to fire on. Empty = derive from the anchor's weekday (legacy). */
    val byDay: Set<DayOfWeek> = emptySet(),
    /** Last day the rule may fire, inclusive. Null = open-ended. Distinct from `finishedOn` — see [firesOn]. */
    val until: LocalDate? = null,
)

/**
 * Whether a task with this rule fires on [day], anchored at [anchor] (the date the user chose when
 * creating the task). Pure function — used by the alarm next-fire calculator and the per-week
 * stat-card expansion.
 *
 * Two independent cutoffs apply and the earlier one wins:
 *  - [RecurrenceRule.until] is the *scheduled* end chosen at creation ("take this for a month").
 *  - [finishedOn] is the *manual* retire from the Recurring tab (null = active).
 *
 * Neither hides history: days up to and including a cutoff still fire, so a finished or expired
 * routine drops off upcoming days while its past per-day completions keep rendering.
 */
fun RecurrenceRule.firesOn(
    anchor: LocalDate,
    day: LocalDate,
    finishedOn: LocalDate? = null,
): Boolean {
    if (finishedOn != null && day.isAfter(finishedOn)) return false
    if (until != null && day.isAfter(until)) return false
    if (day.isBefore(anchor)) return false
    val step = interval.coerceAtLeast(1)
    return when (frequency) {
        Recurrence.NONE -> anchor == day
        Recurrence.DAILY -> ChronoUnit.DAYS.between(anchor, day) % step == 0L
        Recurrence.WEEKLY -> firesOnWeekly(anchor, day, step)
        Recurrence.MONTHLY -> day.dayOfMonth == clampedDayOfMonth(anchor.dayOfMonth, day.year, day.monthValue) &&
            ChronoUnit.MONTHS.between(anchor.withDayOfMonth(1), day.withDayOfMonth(1)) % step == 0L
        Recurrence.YEARLY -> day.monthValue == anchor.monthValue &&
            day.dayOfMonth == clampedDayOfMonth(anchor.dayOfMonth, day.year, day.monthValue) &&
            (day.year - anchor.year) % step == 0
    }
}

/**
 * Source-compatibility shim for the call sites that only know the bare enum. `interval = 1`,
 * `byDay = ∅` and `until = null` reproduce the pre-custom-task behaviour byte for byte.
 */
fun Recurrence.firesOn(
    anchor: LocalDate,
    day: LocalDate,
    finishedOn: LocalDate? = null,
): Boolean = RecurrenceRule(this).firesOn(anchor, day, finishedOn)

/**
 * WEEKLY fires on every weekday in [RecurrenceRule.byDay] (or the anchor's own weekday when the set is
 * empty), but only in weeks that land on the interval. The interval is phased on the *anchor's ISO
 * week* rather than on the day itself, so "every 2 weeks on Mon+Wed" keeps both days inside the same
 * week instead of alternating between them.
 */
private fun RecurrenceRule.firesOnWeekly(
    anchor: LocalDate,
    day: LocalDate,
    step: Int,
): Boolean {
    val days = byDay.ifEmpty { setOf(anchor.dayOfWeek) }
    if (day.dayOfWeek !in days) return false
    return isoWeeksBetween(anchor, day) % step == 0L
}

/**
 * "MONDAY,WEDNESDAY,FRIDAY" → the weekday set. Unknown names are dropped rather than thrown, so a row
 * written by a newer build can never crash an older one — same defensive shape as [Recurrence.fromStorage].
 *
 * One definition shared by the Room mapper and the wire mapper; the CSV form is identical on both sides.
 */
fun dayOfWeekSetFromStorage(value: String?): Set<DayOfWeek> = value?.split(',')
    ?.mapNotNull { name -> runCatching { DayOfWeek.valueOf(name.trim()) }.getOrNull() }
    ?.toSet()
    .orEmpty()

/** Null (not "") when empty, and always in weekday order so the stored string is canonical. */
fun Set<DayOfWeek>.toStorageCsv(): String? = takeIf { it.isNotEmpty() }?.sortedBy { it.value }?.joinToString(",") { it.name }

/** Whole ISO weeks between two dates, measured Monday-to-Monday so a partial week never skews the count. */
internal fun isoWeeksBetween(from: LocalDate, to: LocalDate): Long = ChronoUnit.WEEKS.between(from.with(DayOfWeek.MONDAY), to.with(DayOfWeek.MONDAY))

/**
 * Returns the day-of-month to use when the anchor is later than the target month's length.
 * E.g. clampedDayOfMonth(31, 2026, 2) = 28 (Feb 2026 has 28 days). For yearly tasks anchored on
 * Feb 29, returns 28 in non-leap years.
 */
fun clampedDayOfMonth(anchorDayOfMonth: Int, year: Int, month: Int): Int {
    val maxDay = LocalDate.of(year, month, 1).lengthOfMonth()
    return minOf(anchorDayOfMonth, maxDay)
}
