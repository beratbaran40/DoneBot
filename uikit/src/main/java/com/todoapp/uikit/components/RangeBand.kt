package com.todoapp.uikit.components

import java.time.LocalDate

/**
 * Where one day sits inside a selected span, which decides how much of its cell the connecting band
 * covers.
 *
 * The band used to be a single boolean painted across the WHOLE cell. That is right for the days in
 * the middle and wrong at both ends: the day circle is only 36.dp wide, so a full-width band spilled
 * out past it — a square edge sticking out to the left of the first day and to the right of the last.
 * Half a cell at each end makes the bar stop exactly where the circle does while still joining up
 * with the neighbour.
 */
enum class RangeBand {
    /** No band: no span, or this day is outside it. */
    NONE,

    /** First day — draw the RIGHT half only, so the bar starts under the circle and runs onward. */
    START,

    /** Fully inside — draw the whole cell so adjacent days form one continuous bar. */
    MIDDLE,

    /** Last day — draw the LEFT half only. */
    END,
}

/**
 * Pure so it can be tested without a composition; the drawing side then has no logic worth testing.
 *
 * A one-day span ([start] == [end]) is [NONE] on purpose: it renders identically to a plain single
 * selection, and a half-band hanging off one side of a lone circle reads as a rendering glitch.
 */
fun rangeBandFor(
    day: LocalDate,
    start: LocalDate?,
    end: LocalDate?,
): RangeBand {
    if (start == null || end == null || !end.isAfter(start)) return RangeBand.NONE
    return when {
        day == start -> RangeBand.START
        day == end -> RangeBand.END
        day.isAfter(start) && day.isBefore(end) -> RangeBand.MIDDLE
        else -> RangeBand.NONE
    }
}
