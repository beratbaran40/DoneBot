package com.todoapp.mobile.ui.common.taskform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme
import java.time.LocalDate
import com.example.uikit.R as UiKitR

@TDPreview
@Composable
private fun TaskTypeHeaderPreview() {
    TDTheme {
        Column(
            modifier = Modifier
                .background(TDTheme.colors.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TaskTypeHeader(
                icon = painterResource(UiKitR.drawable.ic_edit_task),
                name = "One-time",
                subtitle = "A single task on a chosen day",
                accent = TDTheme.colors.darkPending,
            )
            TaskTypeHeader(
                icon = painterResource(R.drawable.ic_calendar),
                name = "Routine",
                subtitle = "Repeats on a schedule",
                accent = TDTheme.colors.purple,
            )
            TaskTypeHeader(
                icon = painterResource(R.drawable.ic_staged),
                name = "Staged",
                subtitle = "A few steps that finish a bigger job",
                accent = TDTheme.colors.mediumGreen,
            )
        }
    }
}

@TDPreview
@Composable
private fun TaskFormSectionsChipsPreview() {
    TDTheme {
        Column(
            modifier = Modifier
                .background(TDTheme.colors.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            TaskFormSectionLabel("Date")
            TaskFormDateField(date = LocalDate.of(2026, 6, 20), onSelect = {})
            TaskReminderChips(selected = 60L, onSelect = {})
            TaskFrequencyChips(selected = Recurrence.DAILY, onSelect = {})
        }
    }
}

@TDPreview
@Composable
private fun TaskSubtaskEditorPreview() {
    TDTheme {
        Column(
            modifier = Modifier
                .background(TDTheme.colors.background)
                .padding(16.dp),
        ) {
            TaskSubtaskEditor(
                drafts = listOf("Rinse the rice", "Boil the water", ""),
                onChange = { _, _ -> },
                onRemove = {},
                stepPlaceholder = { "Step ${it + 1}" },
            )
        }
    }
}
