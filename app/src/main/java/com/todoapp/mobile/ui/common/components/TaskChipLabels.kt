package com.todoapp.mobile.ui.common.components

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.Task
import com.todoapp.mobile.domain.model.TaskCategory
import com.todoapp.mobile.ui.common.taskform.recurrenceIntervalLabel
import java.time.format.TextStyle
import java.util.Locale
import com.example.uikit.R as UiKitR

/**
 * The category/recurrence chip shown on a task card.
 *
 * Lived inside HomeTaskList until the recurrence rule grew an interval and a weekday set: at that
 * point Home was the only surface saying anything about a routine at all, and Search / Calendar /
 * FilteredTasks were silently showing a Mon-Wed-Fri task as if it were a plain one-off. Shared here
 * so every surface tells the same story.
 */
@Composable
fun taskChipLabel(task: Task): String? {
    val categoryText = categoryDisplayText(task)
    val recurrenceText = recurrenceDisplayText(task)
    return remember(categoryText, recurrenceText) {
        when {
            categoryText != null && recurrenceText != null -> "$categoryText · $recurrenceText"
            categoryText != null -> categoryText
            recurrenceText != null -> recurrenceText
            else -> null
        }
    }
}

@Composable
fun categoryDisplayText(task: Task): String? {
    val category = task.category
    if (category == TaskCategory.PERSONAL) return null
    if (category == TaskCategory.OTHER) {
        return task.customCategoryName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.category_other)
    }
    val res = when (category) {
        TaskCategory.SHOPPING -> R.string.category_shopping
        TaskCategory.MEDICINE -> R.string.category_medicine
        TaskCategory.HEALTH -> R.string.category_health
        TaskCategory.WORK -> R.string.category_work
        TaskCategory.STUDY -> R.string.category_study
        TaskCategory.BIRTHDAY -> R.string.category_birthday
        TaskCategory.PERSONAL, TaskCategory.OTHER -> return null
    }
    return stringResource(res)
}

/**
 * Note the R: the category glyphs live in :uikit, so they resolve against ITS R. Non-transitive R
 * means they are simply absent from the app's — the strings above still come from the app's R.
 */
@DrawableRes
fun categoryIconFor(category: TaskCategory): Int? = when (category) {
    TaskCategory.SHOPPING -> UiKitR.drawable.ic_shopping_label
    TaskCategory.MEDICINE -> UiKitR.drawable.ic_medication_label
    TaskCategory.HEALTH -> UiKitR.drawable.ic_health_label
    TaskCategory.WORK -> UiKitR.drawable.ic_work_label
    TaskCategory.STUDY -> UiKitR.drawable.ic_study_label
    TaskCategory.BIRTHDAY -> UiKitR.drawable.ic_birthday_label
    TaskCategory.PERSONAL, TaskCategory.OTHER -> null
}

/**
 * "Every day", "Every 2 days", "Mon · Wed · Fri". A bare frequency name stopped being enough once a
 * rule could carry an interval or a weekday set — the card would have said "Weekly" for a
 * Mon/Wed/Fri routine, which is wrong three times a week.
 */
@Composable
fun recurrenceDisplayText(task: Task): String? {
    if (task.recurrence == Recurrence.NONE) return null
    // The weekday set is the most specific thing we can say, so it wins when present.
    if (task.recurrence == Recurrence.WEEKLY && task.recurrenceByDay.isNotEmpty()) {
        val locale = Locale.getDefault()
        return task.recurrenceByDay
            .sortedBy { it.value }
            .joinToString(" · ") { it.getDisplayName(TextStyle.SHORT, locale) }
    }
    return recurrenceIntervalLabel(task.recurrence, task.recurrenceInterval)
}
