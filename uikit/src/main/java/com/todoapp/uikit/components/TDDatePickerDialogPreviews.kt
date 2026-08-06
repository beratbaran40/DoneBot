package com.todoapp.uikit.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.todoapp.uikit.previews.TDPreviewDialog
import com.todoapp.uikit.theme.TDTheme
import java.time.LocalDate

/*
 * Previews for TDDatePickerDialog.
 *
 * Note what these can and cannot show: the dialog starts closed, so every preview below renders the
 * COLLAPSED field — its label, its emphasis, and how a span folds a shared month or year away. The
 * open calendar is not reachable from a preview, which is why the action row has its own entry at the
 * bottom; the rest of the open dialog is verified on a device.
 */

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

/**
 * The dialog's footer, previewed on its own because the dialog itself is never open in a preview.
 *
 * Three buttons is where the 140.dp minimum on TDButton.SMALL starts to bite, so this is the entry
 * that shows whether the labels still fit once they are translated — @TDPreviewDialog is the narrow
 * decoration, which is exactly the width that would break first.
 */
@TDPreviewDialog
@Composable
private fun TDDatePickerActionRowPreview() {
    TDTheme {
        DatePickerActionRow(onToday = {}, onCancel = {}, onConfirm = {})
    }
}
