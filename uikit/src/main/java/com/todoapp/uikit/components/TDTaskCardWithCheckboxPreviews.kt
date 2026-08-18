package com.todoapp.uikit.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.todoapp.uikit.previews.TDPreviewNarrow
import com.todoapp.uikit.theme.TDTheme

/**
 * Squeeze-matrix previews for [TDTaskCardWithCheckbox]. They live here rather than beside the
 * component because that file is already at detekt's per-file function limit — the same reason
 * `TDDatePickerDialogPreviews` and `TDMonthlyDatePickerPreviews` exist.
 */

/**
 * Every meta chip at once, in Turkish, squeezed. This is the case the strip was losing: in a Row the
 * location pill was measured against whatever the first three chips left it, so it was squeezed to
 * nothing and disappeared off the right edge. Flowed, the overflow drops to a second line intact.
 */
@TDPreviewNarrow
@Composable
private fun TDTaskCardAllChipsNarrowPreview() {
    TDTheme {
        Column(modifier = Modifier.padding(12.dp)) {
            TDTaskCardWithCheckbox(
                isChecked = false,
                taskText = "Haftalık alışveriş listesini hazırla ve markete uğra",
                taskDescription = "Süt, ekmek, yumurta",
                onCheckBoxClick = {},
                isOverdue = true,
                overdueLabel = "2 gün gecikti",
                categoryLabel = "Alışveriş",
                locationLabel = "Migros Kadıköy",
                isPendingSync = true,
            )
        }
    }
}

/** Subtask progress and an expand affordance sharing the same narrow text column. */
@TDPreviewNarrow
@Composable
private fun TDTaskCardSubtasksNarrowPreview() {
    TDTheme {
        Column(modifier = Modifier.padding(12.dp)) {
            TDTaskCardWithCheckbox(
                isChecked = false,
                taskText = "Sunum için kaynakları topla",
                taskDescription = null,
                onCheckBoxClick = {},
                subtaskTotal = 5,
                subtaskDone = 2,
                onSubtaskExpandToggle = {},
                categoryLabel = "İş",
            )
        }
    }
}
