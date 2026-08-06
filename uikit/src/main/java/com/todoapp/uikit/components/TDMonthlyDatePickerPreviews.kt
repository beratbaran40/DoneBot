package com.todoapp.uikit.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.previews.TDPreviewNarrow
import com.todoapp.uikit.previews.TDPreviewWide
import com.todoapp.uikit.theme.PaletteKit
import com.todoapp.uikit.theme.TDTheme
import java.time.LocalDate
import java.time.YearMonth

/**
 * Previews for [TDMonthlyDatePicker], in their own file because the component's is already past the
 * size the house rules allow and these are what grow fastest.
 *
 * The narrow entries are the point of the file. Each day is a bounded box holding sp-sized text, so
 * the three things that can overflow it — a 320dp screen, a larger system font, and the PIXEL kit's
 * wider face — each get a cell here.
 */

@TDPreviewWide
@Composable
private fun MonthlyDatePickerPreview() {
    TDTheme {
        var selected by remember { mutableStateOf(LocalDate.of(2025, 12, 3)) }

        TDMonthlyDatePicker(
            modifier = Modifier,
            displayedMonth = YearMonth.of(2025, 12),
            selectedDate = selected,
            onDateSelect = { selected = it },
            onPreviousMonth = {},
            onNextMonth = {},
        )
    }
}

@TDPreviewWide
@Composable
private fun MonthlyDatePickerWithOverduePreview() {
    TDTheme {
        val month = YearMonth.of(2025, 12)
        var selected by remember { mutableStateOf(LocalDate.of(2025, 12, 17)) }
        TDMonthlyDatePicker(
            modifier = Modifier,
            displayedMonth = month,
            selectedDate = selected,
            taskDates = setOf(month.atDay(10), month.atDay(20)),
            overdueDates = setOf(month.atDay(3), month.atDay(9), month.atDay(15)),
            hasOverdueBeforeDisplayedMonth = true,
            onDateSelect = { selected = it },
            onPreviousMonth = {},
            onNextMonth = {},
        )
    }
}

/**
 * September is the widest month name in English and the header has two 48dp icon buttons beside it,
 * so this is where the month label runs out of room first.
 */
@TDPreviewNarrow
@Composable
private fun MonthlyDatePickerNarrowPreview() {
    TDTheme {
        var selected by remember { mutableStateOf(LocalDate.of(2026, 9, 3)) }
        TDMonthlyDatePicker(
            modifier = Modifier,
            displayedMonth = YearMonth.of(2026, 9),
            selectedDate = selected,
            onDateSelect = { selected = it },
            onPreviousMonth = {},
            onNextMonth = {},
        )
    }
}

/** Pixelify Sans is materially wider than Poppins at the same size — the tightest fit of the three kits. */
@TDPreviewNarrow
@Composable
private fun MonthlyDatePickerPixelNarrowPreview() {
    TDTheme(palette = PaletteKit.PIXEL) {
        var selected by remember { mutableStateOf(LocalDate.of(2026, 9, 3)) }
        TDMonthlyDatePicker(
            modifier = Modifier,
            displayedMonth = YearMonth.of(2026, 9),
            selectedDate = selected,
            onDateSelect = { selected = it },
            onPreviousMonth = {},
            onNextMonth = {},
        )
    }
}

/**
 * The cards on their own, at the widest weekday labels the two languages produce ("Wed" / "Cmt").
 * A card that grows is fine; a label on two lines is the bug.
 */
@TDPreviewNarrow
@Composable
private fun DatePickerCardWidestLabelsPreview() {
    TDTheme {
        Row(modifier = Modifier.fillMaxWidth()) {
            // 2026-09-02 is a Wednesday, 2026-09-05 a Saturday.
            DatePickerCard(modifier = Modifier, currentDate = LocalDate.of(2026, 9, 2), isSelected = false)
            DatePickerCard(modifier = Modifier, currentDate = LocalDate.of(2026, 9, 5), isSelected = true, hasTask = true)
        }
    }
}

@TDPreview
@Composable
private fun DatePickerCardStatesPreview() {
    TDTheme {
        Column {
            Row {
                DatePickerCard(modifier = Modifier, currentDate = LocalDate.of(2025, 12, 17), isSelected = true)
                DatePickerCard(modifier = Modifier, currentDate = LocalDate.of(2025, 12, 18), isSelected = false)
            }
            Row(modifier = Modifier.padding(top = 8.dp)) {
                DatePickerCard(
                    modifier = Modifier,
                    currentDate = LocalDate.of(2025, 12, 9),
                    isSelected = false,
                    hasOverdue = true,
                )
                DatePickerCard(
                    modifier = Modifier,
                    currentDate = LocalDate.of(2025, 12, 9),
                    isSelected = true,
                    hasOverdue = true,
                )
                DatePickerCard(
                    modifier = Modifier,
                    currentDate = LocalDate.of(2025, 12, 10),
                    isSelected = false,
                    hasTask = true,
                )
                DatePickerCard(
                    modifier = Modifier,
                    currentDate = LocalDate.of(2025, 12, 10),
                    isSelected = true,
                    hasTask = true,
                )
            }
        }
    }
}
