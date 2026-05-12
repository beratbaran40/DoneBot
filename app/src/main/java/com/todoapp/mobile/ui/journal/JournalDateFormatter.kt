package com.todoapp.mobile.ui.journal

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.todoapp.mobile.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Pretty date for a journal entry card. Returns "Bugün", "Dün", or e.g. "11 May 2026" otherwise.
 */
@Composable
internal fun entryDateLabel(createdAtEpochMs: Long, today: LocalDate = LocalDate.now()): String {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val date = Instant.ofEpochMilli(createdAtEpochMs).atZone(ZoneId.systemDefault()).toLocalDate()
    return when (date) {
        today -> stringResource(R.string.journal_date_today)
        today.minusDays(1) -> stringResource(R.string.journal_date_yesterday)
        else -> {
            val pattern = if (date.year == today.year) PATTERN_SAME_YEAR else PATTERN_OTHER_YEAR
            DateTimeFormatter.ofPattern(pattern, locale).format(date)
        }
    }
}

private const val PATTERN_SAME_YEAR = "d MMM"
private const val PATTERN_OTHER_YEAR = "d MMM yyyy"
