package com.todoapp.mobile.ui.common

import com.todoapp.uikit.components.DatePickerCommit
import com.todoapp.uikit.components.DayTapOutcome
import com.todoapp.uikit.components.resolveCommit
import com.todoapp.uikit.components.resolveDayTap
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * The picker's whole tap grammar. Four states interact (nothing selected / mid-gesture / span, tapped
 * inside / span, tapped outside) and each one is a different callback for the caller, so this is
 * pinned here rather than discovered on a device.
 *
 * Lives in `:app` because `:uikit` has no test source set, like [CalendarGridTest].
 */
class DayTapOutcomeTest {
    private val start = LocalDate.of(2026, 8, 10)
    private val end = LocalDate.of(2026, 8, 20)

    @Test
    fun `with nothing selected a tap picks a single day`() {
        assertEquals(
            DayTapOutcome.SelectSingle(start),
            resolveDayTap(start, selectedDate = null, rangeEnd = null, anchorDate = null),
        )
    }

    @Test
    fun `a tap finishes a span that a long-press started`() {
        // The point of this round: holding for BOTH ends was a chore. Hold once, then tap.
        assertEquals(
            DayTapOutcome.SelectSpan(start, end),
            resolveDayTap(end, selectedDate = start, rangeEnd = null, anchorDate = start),
        )
    }

    @Test
    fun `finishing a span backwards still orders the two ends`() {
        assertEquals(
            DayTapOutcome.SelectSpan(start, end),
            resolveDayTap(start, selectedDate = end, rangeEnd = null, anchorDate = end),
        )
    }

    @Test
    fun `tapping the held day itself collapses to a single day`() {
        // A zero-length span is just a selection, and the user has clearly changed their mind.
        assertEquals(
            DayTapOutcome.SelectSingle(start),
            resolveDayTap(start, selectedDate = start, rangeEnd = null, anchorDate = start),
        )
    }

    @Test
    fun `tapping outside an existing span drops it back to one day`() {
        assertEquals(
            DayTapOutcome.SelectSingle(LocalDate.of(2026, 8, 25)),
            resolveDayTap(LocalDate.of(2026, 8, 25), start, end, anchorDate = null),
        )
        assertEquals(
            DayTapOutcome.SelectSingle(LocalDate.of(2026, 8, 1)),
            resolveDayTap(LocalDate.of(2026, 8, 1), start, end, anchorDate = null),
        )
    }

    @Test
    fun `tapping inside an existing span drags the nearer end to it`() {
        // Nearer the start (10) than the end (20) → the start moves up.
        assertEquals(
            DayTapOutcome.SelectSpan(LocalDate.of(2026, 8, 12), end),
            resolveDayTap(LocalDate.of(2026, 8, 12), start, end, anchorDate = null),
        )
        // Nearer the end → the end moves down.
        assertEquals(
            DayTapOutcome.SelectSpan(start, LocalDate.of(2026, 8, 18)),
            resolveDayTap(LocalDate.of(2026, 8, 18), start, end, anchorDate = null),
        )
    }

    @Test
    fun `an exact midpoint is resolved deterministically rather than by comparison order`() {
        // 15 is five days from each end. The tie goes to the start.
        assertEquals(
            DayTapOutcome.SelectSpan(LocalDate.of(2026, 8, 15), end),
            resolveDayTap(LocalDate.of(2026, 8, 15), start, end, anchorDate = null),
        )
    }

    @Test
    fun `tapping an endpoint leaves the span as it is`() {
        assertEquals(DayTapOutcome.SelectSpan(start, end), resolveDayTap(start, start, end, null))
        assertEquals(DayTapOutcome.SelectSpan(start, end), resolveDayTap(end, start, end, null))
    }

    @Test
    fun `trimming can never collapse a span to zero length`() {
        // Both days of a two-day span are endpoints, and tapping an endpoint moves the OTHER end
        // onto itself — i.e. nothing changes. There is no tap that shrinks a span out of existence,
        // which is why resolveDayTap has no zero-length branch. Dropping a span is what a tap
        // OUTSIDE it is for.
        val next = start.plusDays(1)
        assertEquals(DayTapOutcome.SelectSpan(start, next), resolveDayTap(start, start, next, null))
        assertEquals(DayTapOutcome.SelectSpan(start, next), resolveDayTap(next, start, next, null))
    }

    @Test
    fun `a malformed span is ignored rather than trusted`() {
        // end before start can only come from restored state; treat the tap as a plain selection.
        assertEquals(
            DayTapOutcome.SelectSingle(start),
            resolveDayTap(start, selectedDate = end, rangeEnd = start, anchorDate = null),
        )
    }

    // --- resolveCommit: what pressing OK tells the caller -------------------------------------

    @Test
    fun `confirming an untouched draft announces nothing`() {
        // Opening the picker and pressing OK is not an edit. A form that diffs against its loaded
        // task would otherwise be free to call itself dirty just because the dialog was looked at.
        assertEquals(
            DatePickerCommit.Nothing,
            resolveCommit(start, null, start, null, rangesEnabled = false),
        )
        assertEquals(
            DatePickerCommit.Nothing,
            resolveCommit(start, end, start, end, rangesEnabled = true),
        )
    }

    @Test
    fun `a moved day commits as a single day`() {
        assertEquals(
            DatePickerCommit.Single(end, clearRange = false),
            resolveCommit(end, null, start, null, rangesEnabled = false),
        )
    }

    @Test
    fun `a completed span commits as a span`() {
        assertEquals(
            DatePickerCommit.Span(start, end),
            resolveCommit(start, end, start, null, rangesEnabled = true),
        )
    }

    @Test
    fun `dropping a span asks for the old end to be cleared`() {
        // This is the "Today" bug in its general form: committing only the new start would leave the
        // caller holding the old end, and an end before the start makes firesOn reject every day.
        assertEquals(
            DatePickerCommit.Single(LocalDate.of(2026, 8, 25), clearRange = true),
            resolveCommit(LocalDate.of(2026, 8, 25), null, start, end, rangesEnabled = true),
        )
    }

    @Test
    fun `keeping the start while dropping the span still clears the end`() {
        // The day did not move, so only the range changed — Nothing would strand the end.
        assertEquals(
            DatePickerCommit.Single(start, clearRange = true),
            resolveCommit(start, null, start, end, rangesEnabled = true),
        )
    }

    @Test
    fun `an emptied draft commits as a deselect`() {
        assertEquals(
            DatePickerCommit.Deselect,
            resolveCommit(null, null, start, null, rangesEnabled = false),
        )
    }

    @Test
    fun `a draft end is ignored where ranges are switched off`() {
        // The four single-day pickers must never be handed a span, even if stale state carried one
        // in — they have no onRangeSelect to receive it.
        assertEquals(
            DatePickerCommit.Single(start, clearRange = false),
            resolveCommit(start, end, null, end, rangesEnabled = false),
        )
    }
}
