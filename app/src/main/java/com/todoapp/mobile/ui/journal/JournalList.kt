package com.todoapp.mobile.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.journal.JournalContract.DateGroup
import com.todoapp.mobile.ui.journal.JournalContract.GroupedSection
import com.todoapp.uikit.components.TDJournalCard
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.theme.TDTheme

@Composable
internal fun JournalList(
    sections: List<GroupedSection>,
    onEntryClick: (Long) -> Unit,
    onEntryLongPress: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        sections.forEach { section ->
            item(key = "header-${section.group}") {
                JournalGroupHeader(group = section.group)
            }
            items(
                items = section.entries,
                key = { entry -> entry.id },
            ) { entry ->
                val titleText = entry.title.takeIf { it.isNotBlank() }
                // When the first line was auto-derived into the title, strip it from the
                // preview so the card doesn't show the same line twice.
                val previewText = if (titleText != null) {
                    entry.content.lineSequence().drop(1).joinToString("\n").trim()
                } else {
                    entry.content
                }
                TDJournalCard(
                    dateLabel = entryDateLabel(createdAtEpochMs = entry.createdAt),
                    title = titleText,
                    contentPreview = previewText,
                    photoCount = entry.photoPaths.size,
                    onClick = { onEntryClick(entry.id) },
                    onLongClick = { onEntryLongPress(entry.id) },
                )
            }
        }
    }
}

@Composable
private fun JournalGroupHeader(group: DateGroup) {
    val labelRes = when (group) {
        DateGroup.TODAY -> R.string.journal_date_today
        DateGroup.YESTERDAY -> R.string.journal_date_yesterday
        DateGroup.THIS_WEEK -> R.string.journal_date_this_week
        DateGroup.THIS_MONTH -> R.string.journal_date_this_month
        DateGroup.OLDER -> R.string.journal_date_older
    }
    TDText(
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
        text = stringResource(labelRes),
        style = TDTheme.typography.subheading2,
        color = TDTheme.colors.pendingGray,
    )
}
