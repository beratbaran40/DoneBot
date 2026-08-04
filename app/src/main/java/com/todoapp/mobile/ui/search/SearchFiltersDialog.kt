package com.todoapp.mobile.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.TaskCategory
import com.todoapp.mobile.ui.search.SearchContract.DateRangeFilter
import com.todoapp.mobile.ui.search.SearchContract.SearchFilter
import com.todoapp.mobile.ui.search.SearchContract.SearchFilters
import com.todoapp.mobile.ui.search.SearchContract.StatusFilter
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.theme.TDTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SearchFiltersDialog(
    initialFilters: SearchFilters,
    onApply: (SearchFilters) -> Unit,
    onDismiss: () -> Unit,
    onClearAll: () -> Unit,
) {
    var draft by remember { mutableStateOf(initialFilters) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TDTheme.colors.background,
        shape = TDTheme.shapes.xLarge,
        title = {
            TDText(
                text = stringResource(R.string.search_filter_dialog_title),
                style = TDTheme.typography.heading4,
                color = TDTheme.colors.onBackground,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                FilterSection(titleRes = R.string.search_filter_section_show) {
                    SearchFilter.entries.forEach { type ->
                        ChoiceChip(
                            label = stringResource(resultTypeLabel(type)),
                            selected = draft.resultType == type,
                            onClick = { draft = draft.copy(resultType = type) },
                        )
                    }
                }

                FilterSection(titleRes = R.string.search_filter_section_category) {
                    TaskCategory.entries.forEach { cat ->
                        ChoiceChip(
                            label = stringResource(categoryLabel(cat)),
                            selected = cat in draft.categories,
                            onClick = {
                                draft = draft.copy(
                                    categories = draft.categories.toggled(cat),
                                )
                            },
                        )
                    }
                }

                FilterSection(titleRes = R.string.search_filter_section_recurrence) {
                    Recurrence.entries.forEach { rec ->
                        ChoiceChip(
                            label = stringResource(recurrenceLabel(rec)),
                            selected = rec in draft.recurrences,
                            onClick = {
                                draft = draft.copy(
                                    recurrences = draft.recurrences.toggled(rec),
                                )
                            },
                        )
                    }
                }

                FilterSection(titleRes = R.string.search_filter_section_status) {
                    StatusFilter.entries.forEach { st ->
                        ChoiceChip(
                            label = stringResource(statusLabel(st)),
                            selected = draft.status == st,
                            onClick = { draft = draft.copy(status = st) },
                        )
                    }
                }

                FilterSection(titleRes = R.string.search_filter_section_date) {
                    DateRangeFilter.entries.forEach { dr ->
                        ChoiceChip(
                            label = stringResource(dateLabel(dr)),
                            selected = draft.dateRange == dr,
                            onClick = { draft = draft.copy(dateRange = dr) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(draft) }) {
                TDText(
                    text = stringResource(R.string.search_filter_apply),
                    style = TDTheme.typography.subheading1,
                    color = TDTheme.colors.purple,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = {
                draft = SearchFilters()
                onClearAll()
            }) {
                TDText(
                    text = stringResource(R.string.search_filter_clear_all),
                    style = TDTheme.typography.subheading1,
                    color = TDTheme.colors.gray,
                )
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(
    titleRes: Int,
    content: @Composable FlowRowScope.() -> Unit,
) {
    Spacer(Modifier.padding(vertical = 4.dp))
    TDText(
        text = stringResource(titleRes),
        style = TDTheme.typography.subheading2,
        color = TDTheme.colors.gray,
    )
    Spacer(Modifier.padding(vertical = 4.dp))
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
    Spacer(Modifier.padding(vertical = 4.dp))
}

@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) TDTheme.colors.purple else TDTheme.colors.lightPending
    val text = if (selected) TDTheme.colors.background else TDTheme.colors.darkPending
    val borderColor = if (selected) TDTheme.colors.purple else TDTheme.colors.lightGray
    Column(
        modifier = Modifier
            .background(bg, TDTheme.shapes.xLarge)
            .border(width = 1.dp, color = borderColor, shape = TDTheme.shapes.xLarge)
            .clickable(onClick = onClick)
            .padding(PaddingValues(horizontal = 14.dp, vertical = 8.dp)),
    ) {
        TDText(
            text = label,
            style = TDTheme.typography.subheading2,
            color = text,
        )
    }
}

private fun <T> Set<T>.toggled(value: T): Set<T> = if (value in this) this - value else this + value

private fun resultTypeLabel(filter: SearchFilter): Int = when (filter) {
    SearchFilter.ALL -> R.string.search_filter_all
    SearchFilter.TASKS -> R.string.search_filter_tasks
    SearchFilter.GROUPS -> R.string.search_filter_groups
    SearchFilter.GROUP_TASKS -> R.string.search_filter_group_tasks
}

private fun categoryLabel(category: TaskCategory): Int = when (category) {
    TaskCategory.PERSONAL -> R.string.category_personal
    TaskCategory.SHOPPING -> R.string.category_shopping
    TaskCategory.MEDICINE -> R.string.category_medicine
    TaskCategory.HEALTH -> R.string.category_health
    TaskCategory.WORK -> R.string.category_work
    TaskCategory.STUDY -> R.string.category_study
    TaskCategory.BIRTHDAY -> R.string.category_birthday
    TaskCategory.OTHER -> R.string.category_other
}

private fun recurrenceLabel(recurrence: Recurrence): Int = when (recurrence) {
    Recurrence.NONE -> R.string.search_filter_recurrence_none
    Recurrence.DAILY -> R.string.recurrence_daily
    Recurrence.WEEKLY -> R.string.recurrence_weekly
    Recurrence.MONTHLY -> R.string.recurrence_monthly
    Recurrence.YEARLY -> R.string.recurrence_yearly
}

private fun statusLabel(status: StatusFilter): Int = when (status) {
    StatusFilter.ALL -> R.string.search_filter_status_all
    StatusFilter.PENDING -> R.string.search_filter_status_pending
    StatusFilter.COMPLETED -> R.string.search_filter_status_completed
}

private fun dateLabel(range: DateRangeFilter): Int = when (range) {
    DateRangeFilter.ALL_TIME -> R.string.search_filter_date_all
    DateRangeFilter.TODAY -> R.string.search_filter_date_today
    DateRangeFilter.THIS_WEEK -> R.string.search_filter_date_week
    DateRangeFilter.THIS_MONTH -> R.string.search_filter_date_month
}

@com.todoapp.uikit.previews.TDPreviewDialog
@androidx.compose.runtime.Composable
private fun SearchFiltersDialogEmptyPreview() {
    TDTheme {
        SearchFiltersDialog(
            initialFilters = SearchFilters(),
            onApply = {},
            onDismiss = {},
            onClearAll = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreviewDialog
@androidx.compose.runtime.Composable
private fun SearchFiltersDialogActivePreview() {
    TDTheme {
        SearchFiltersDialog(
            initialFilters = SearchFilters(
                resultType = SearchFilter.TASKS,
                categories = setOf(TaskCategory.WORK, TaskCategory.HEALTH),
                recurrences = setOf(Recurrence.DAILY),
                status = StatusFilter.PENDING,
                dateRange = DateRangeFilter.THIS_WEEK,
            ),
            onApply = {},
            onDismiss = {},
            onClearAll = {},
        )
    }
}
