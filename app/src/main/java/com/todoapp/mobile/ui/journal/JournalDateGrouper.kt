package com.todoapp.mobile.ui.journal

import com.todoapp.mobile.domain.model.JournalEntry
import com.todoapp.mobile.ui.journal.JournalContract.DateGroup
import com.todoapp.mobile.ui.journal.JournalContract.GroupedSection
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Groups [entries] (assumed sorted DESC by createdAt) into chronological buckets relative to [today].
 * Buckets retain the input order so newest-first ordering survives the bucketing pass.
 */
fun groupEntries(
    entries: List<JournalEntry>,
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault(),
): List<GroupedSection> {
    if (entries.isEmpty()) return emptyList()
    val yesterday = today.minusDays(1)
    val sevenDaysAgo = today.minusDays(THIS_WEEK_DAYS.toLong())
    val thirtyDaysAgo = today.minusDays(THIS_MONTH_DAYS.toLong())

    val buckets = linkedMapOf<DateGroup, MutableList<JournalEntry>>()
    for (entry in entries) {
        val date = Instant.ofEpochMilli(entry.createdAt).atZone(zone).toLocalDate()
        val bucket = when {
            date == today -> DateGroup.TODAY
            date == yesterday -> DateGroup.YESTERDAY
            !date.isBefore(sevenDaysAgo) -> DateGroup.THIS_WEEK
            !date.isBefore(thirtyDaysAgo) -> DateGroup.THIS_MONTH
            else -> DateGroup.OLDER
        }
        buckets.getOrPut(bucket) { mutableListOf() }.add(entry)
    }
    return buckets.map { (group, list) -> GroupedSection(group = group, entries = list) }
}

private const val THIS_WEEK_DAYS = 7
private const val THIS_MONTH_DAYS = 30

/** Days between [from] and [to] inclusive, ignoring tz wrap. Exposed for tests. */
internal fun daysBetween(from: LocalDate, to: LocalDate): Long = ChronoUnit.DAYS.between(from, to)
