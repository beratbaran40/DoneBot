@file:Suppress("MatchingDeclarationName")

package com.todoapp.mobile.ui.journal

import androidx.annotation.StringRes
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.model.JournalMood

/**
 * Maps a [JournalMood] to a presentation tuple (emoji + label string resource).
 * The mapping lives in :app — uikit components must not know about [JournalMood].
 */
internal data class MoodPresentation(
    val mood: JournalMood,
    val emoji: String,
    @StringRes val labelRes: Int,
)

internal val ORDERED_MOODS: List<MoodPresentation> = listOf(
    MoodPresentation(JournalMood.HAPPY, "😊", R.string.journal_mood_happy),
    MoodPresentation(JournalMood.NEUTRAL, "😐", R.string.journal_mood_neutral),
    MoodPresentation(JournalMood.SAD, "😢", R.string.journal_mood_sad),
    MoodPresentation(JournalMood.GRATEFUL, "🙏", R.string.journal_mood_grateful),
    MoodPresentation(JournalMood.ANXIOUS, "😰", R.string.journal_mood_anxious),
)

internal fun JournalMood.emoji(): String = ORDERED_MOODS.first { it.mood == this }.emoji

internal fun moodIndex(mood: JournalMood?): Int = if (mood == null) -1 else ORDERED_MOODS.indexOfFirst { it.mood == mood }
