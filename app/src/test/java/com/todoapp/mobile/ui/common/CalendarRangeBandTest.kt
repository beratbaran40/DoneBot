package com.todoapp.mobile.ui.common

import com.todoapp.uikit.components.RangeBand
import com.todoapp.uikit.components.rangeBandFor
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * The band is what makes a span read as one bar instead of two dots. Its shape is pure arithmetic, so
 * it is pinned here rather than left to a screenshot — the failure it guards against (a square edge
 * sticking out beside a round day) is exactly the kind of thing nobody notices in a diff.
 *
 * Lives in `:app` because `:uikit` has no test source set, like [CalendarGridTest].
 */
class CalendarRangeBandTest {
    private val start = LocalDate.of(2026, 8, 10)
    private val end = LocalDate.of(2026, 8, 20)

    @Test
    fun `the two ends get half a band each and the days between get a full one`() {
        assertEquals(RangeBand.START, rangeBandFor(start, start, end))
        assertEquals(RangeBand.END, rangeBandFor(end, start, end))
        assertEquals(RangeBand.MIDDLE, rangeBandFor(LocalDate.of(2026, 8, 15), start, end))
    }

    @Test
    fun `days outside the span get nothing`() {
        assertEquals(RangeBand.NONE, rangeBandFor(LocalDate.of(2026, 8, 9), start, end))
        assertEquals(RangeBand.NONE, rangeBandFor(LocalDate.of(2026, 8, 21), start, end))
    }

    @Test
    fun `a plain single selection draws no band at all`() {
        // The regression this exists for: the band used to key off "is selected", so one tapped day
        // painted a full-width rectangle behind its own circle.
        assertEquals(RangeBand.NONE, rangeBandFor(start, start, end = null))
        assertEquals(RangeBand.NONE, rangeBandFor(start, start = null, end = null))
    }

    @Test
    fun `a one-day span is treated as a single selection`() {
        // Identical to a plain selection on screen, and half a band hanging off one side of a lone
        // circle reads as a glitch rather than a range.
        assertEquals(RangeBand.NONE, rangeBandFor(start, start, start))
    }

    @Test
    fun `an inverted span draws nothing rather than rendering backwards`() {
        // The picker always orders the two held days, but a caller restoring persisted state might
        // not — better to draw no band than a bar running the wrong way.
        assertEquals(RangeBand.NONE, rangeBandFor(start, end, start))
        assertEquals(RangeBand.NONE, rangeBandFor(LocalDate.of(2026, 8, 15), end, start))
    }

    @Test
    fun `two adjacent days meet in the middle with no gap`() {
        val next = start.plusDays(1)
        // START covers the right half, END the left half, so the halves touch at the cell boundary.
        assertEquals(RangeBand.START, rangeBandFor(start, start, next))
        assertEquals(RangeBand.END, rangeBandFor(next, start, next))
    }
}
